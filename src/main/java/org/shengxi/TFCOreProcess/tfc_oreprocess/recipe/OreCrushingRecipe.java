package org.shengxi.TFCOreProcess.tfc_oreprocess.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModRecipes;

/**
 * 手工原矿锤碎配方。
 * 严格要求输入为 1 个原矿物品 + 1 个带有 #c:tools/hammer 标签的工具，
 * 输出对应碎矿，并损耗锤子 1 点耐久（耐久耗尽时销毁）。
 */
public class OreCrushingRecipe implements CraftingRecipe {
    public static final TagKey<Item> HAMMERS_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("c", "tools/hammer")
    );

    private final Ingredient ingredient;
    private final ItemStack result;

    public OreCrushingRecipe(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        ingredients.add(Ingredient.of(HAMMERS_TAG));
        return ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean foundOre = false;
        boolean foundHammer = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (!foundHammer && stack.is(HAMMERS_TAG)) {
                foundHammer = true;
            } else if (!foundOre && ingredient.test(stack)) {
                foundOre = true;
            } else {
                // 出现额外物品或重复物品时拒绝匹配
                return false;
            }
        }

        return foundOre && foundHammer;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack output = result.copy();
        // 如果输入的原矿上携带有 OreProcessData 数据组件，产物如为通用碎矿则继承该组件
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack) && stack.has(ModDataComponents.ORE_PROCESS.get())) {
                output.set(ModDataComponents.ORE_PROCESS.get(), stack.get(ModDataComponents.ORE_PROCESS.get()));
                break;
            }
        }
        return output;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(HAMMERS_TAG)) {
                ItemStack hammer = stack.copy();
                hammer.setCount(1);
                // 扣除 1 点耐久，超限则损坏销毁
                int nextDamage = hammer.getDamageValue() + 1;
                if (nextDamage < hammer.getMaxDamage()) {
                    hammer.setDamageValue(nextDamage);
                    remaining.set(i, hammer);
                } else {
                    remaining.set(i, ItemStack.EMPTY);
                }
            }
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ORE_CRUSHING.get();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<OreCrushingRecipe> {
        public static final MapCodec<OreCrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(OreCrushingRecipe::getIngredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(OreCrushingRecipe::getResult)
        ).apply(instance, OreCrushingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, OreCrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, OreCrushingRecipe::getIngredient,
            ItemStack.STREAM_CODEC, OreCrushingRecipe::getResult,
            OreCrushingRecipe::new
        );

        @Override
        public MapCodec<OreCrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OreCrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
