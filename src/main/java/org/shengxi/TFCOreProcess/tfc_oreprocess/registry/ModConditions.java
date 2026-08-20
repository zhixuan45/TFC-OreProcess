package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.recipe.PipelineModeCondition;

/** 条件序列化器注册表。 */
public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
        DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, Tfc_oreprocess.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<PipelineModeCondition>> PIPELINE_MODE =
        CONDITION_CODECS.register("pipeline_mode", () -> PipelineModeCondition.CODEC);

    private ModConditions() {
    }
}
