package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents;

/** 固定矿物物品的默认组件，避免创造栏或配方产物丢失加工数据。 */
public final class OreProcessItem extends Item {
    private final OreMaterial material;
    private final int metalAmountMb;

    public OreProcessItem(Properties properties, OreMaterial material, int metalAmountMb) {
        super(properties);
        this.material = material;
        this.metalAmountMb = metalAmountMb;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        OreMaterialDefinition definition = material.definition();
        stack.set(ModDataComponents.ORE_PROCESS.get(), new OreProcessData(
            net.minecraft.resources.ResourceLocation.parse(definition.sourceItemId()),
            net.minecraft.resources.ResourceLocation.parse(definition.moltenFluidId()),
            metalAmountMb,
            definition.meltingTemperature()
        ));
        return stack;
    }
}
