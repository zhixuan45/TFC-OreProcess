package org.shengxi.TFCOreProcess.tfc_oreprocess.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.outputs.ItemStackProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreProcessData;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModRecipes;

/**
 * 动态 TFC 加热熔炼配方。
 * 读取物品上的 OreProcessData 数据组件，动态解析熔融金属流体、金属量与熔化温度，
 * 使得通用物品与额外添加的矿石无需预先写死静态 JSON 即可在 TFC 坩埚与火炉中熔化。
 */
public class DynamicHeatingRecipe extends HeatingRecipe {
    private final Ingredient ingredient;
    private final int defaultAmountMb;
    private final float defaultTemperature;

    public DynamicHeatingRecipe(Ingredient ingredient, int defaultAmountMb, float defaultTemperature) {
        super(ingredient, ItemStackProvider.empty(), FluidStack.EMPTY, defaultTemperature, false);
        this.ingredient = ingredient;
        this.defaultAmountMb = defaultAmountMb;
        this.defaultTemperature = defaultTemperature;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getDefaultAmountMb() {
        return defaultAmountMb;
    }

    public float getDefaultTemperature() {
        return defaultTemperature;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ingredient.test(stack) && stack.has(ModDataComponents.ORE_PROCESS.get());
    }

    @Override
    public FluidStack assembleFluid(ItemStack stack) {
        OreProcessData data = stack.get(ModDataComponents.ORE_PROCESS.get());
        if (data == null) {
            return FluidStack.EMPTY;
        }

        Fluid fluid = BuiltInRegistries.FLUID.get(data.moltenFluidId());
        if (fluid == null || fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }

        int amount = data.metalAmountMb() > 0 ? data.metalAmountMb() : defaultAmountMb;
        return new FluidStack(fluid, amount);
    }

    @Override
    public float getTemperature() {
        return defaultTemperature;
    }

    @Override
    public boolean isValidTemperature(float temp) {
        return temp >= defaultTemperature;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYNAMIC_HEATING.get();
    }

    public static class Serializer implements RecipeSerializer<DynamicHeatingRecipe> {
        public static final MapCodec<DynamicHeatingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(DynamicHeatingRecipe::getIngredient),
            Codec.INT.optionalFieldOf("amount", 5).forGetter(DynamicHeatingRecipe::getDefaultAmountMb),
            Codec.FLOAT.optionalFieldOf("temperature", 1080.0F).forGetter(DynamicHeatingRecipe::getDefaultTemperature)
        ).apply(instance, DynamicHeatingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DynamicHeatingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, DynamicHeatingRecipe::getIngredient,
            ByteBufCodecs.VAR_INT, DynamicHeatingRecipe::getDefaultAmountMb,
            ByteBufCodecs.FLOAT, DynamicHeatingRecipe::getDefaultTemperature,
            DynamicHeatingRecipe::new
        );

        @Override
        public MapCodec<DynamicHeatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DynamicHeatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
