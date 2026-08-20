package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.recipe.DynamicHeatingRecipe;
import org.shengxi.TFCOreProcess.tfc_oreprocess.recipe.OreCrushingRecipe;

/** 配方序列化器注册表。 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Tfc_oreprocess.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OreCrushingRecipe>> ORE_CRUSHING =
        RECIPE_SERIALIZERS.register("ore_crushing", OreCrushingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicHeatingRecipe>> DYNAMIC_HEATING =
        RECIPE_SERIALIZERS.register("dynamic_heating", DynamicHeatingRecipe.Serializer::new);

    private ModRecipes() {
    }
}
