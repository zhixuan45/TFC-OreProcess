package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents;

/** 
 * 固定矿物加工物品。
 * 在构建 Properties 阶段直接附加默认的 OreProcessData 数据组件，
 * 保证配方产物、战利品表掉落与创造栏物品均带有完整的加工元数据。
 */
public final class OreProcessItem extends Item {
    private final OreMaterial material;
    private final int metalAmountMb;

    public OreProcessItem(Properties properties, OreMaterial material, int metalAmountMb) {
        super(createProperties(properties, material, metalAmountMb));
        this.material = material;
        this.metalAmountMb = metalAmountMb;
    }

    private static Properties createProperties(Properties properties, OreMaterial material, int metalAmountMb) {
        OreMaterialDefinition definition = material.definition();
        return properties.component(
            ModDataComponents.ORE_PROCESS.get(),
            new OreProcessData(
                ResourceLocation.parse(definition.sourceItemId()),
                ResourceLocation.parse(definition.moltenFluidId()),
                metalAmountMb,
                definition.meltingTemperature()
            )
        );
    }

    public OreMaterial getMaterial() {
        return material;
    }

    public int getMetalAmountMb() {
        return metalAmountMb;
    }
}

