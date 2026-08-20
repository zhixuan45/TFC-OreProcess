package org.shengxi.TFCOreProcess.tfc_oreprocess.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Config;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModConditions;

import java.util.Locale;

/**
 * 基于模组配置流水线模式（PipelineMode）的配方加载条件。
 * 允许在数据包配方中指定仅在 STANDARD 或 TEMPLATE_CLASSIC 模式下加载。
 */
public record PipelineModeCondition(Config.PipelineMode mode) implements ICondition {
    public static final MapCodec<PipelineModeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("mode").xmap(
            str -> Config.PipelineMode.valueOf(str.toUpperCase(Locale.ROOT)),
            enumMode -> enumMode.name().toLowerCase(Locale.ROOT)
        ).forGetter(PipelineModeCondition::mode)
    ).apply(instance, PipelineModeCondition::new));

    @Override
    public boolean test(IContext context) {
        return Config.PIPELINE_MODE.get() == mode;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.PIPELINE_MODE.get();
    }
}
