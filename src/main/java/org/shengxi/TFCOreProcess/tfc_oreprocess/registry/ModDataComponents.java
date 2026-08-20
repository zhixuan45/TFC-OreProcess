package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreProcessData;

/** 加工矿物物品使用的数据组件注册表。 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Tfc_oreprocess.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OreProcessData>> ORE_PROCESS =
        COMPONENTS.registerComponentType("ore_process", builder -> builder
            .persistent(OreProcessData.CODEC)
            .networkSynchronized(OreProcessData.STREAM_CODEC));

    private ModDataComponents() {
    }
}
