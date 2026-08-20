package org.shengxi.TFCOreProcess.tfc_oreprocess;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreMaterial;

import java.util.EnumMap;
import java.util.Map;

/** 洗矿产量与副产物配置。 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue POWDER_YIELD_MULTIPLIER = BUILDER
        .comment("Expected powder yield multiplier per base 5 mB unit")
        .defineInRange("powderYieldMultiplier", 1.5D, 1.0D, 16.0D);

    public static final ModConfigSpec.BooleanValue ENABLE_BYPRODUCTS = BUILDER
        .comment("Enable independent washing byproducts")
        .define("enableByproducts", true);

    public static final ModConfigSpec.DoubleValue BYPRODUCT_CHANCE_MULTIPLIER = BUILDER
        .comment("Multiplier applied independently to each byproduct chance")
        .defineInRange("byproductChanceMultiplier", 1.0D, 0.0D, 16.0D);

    public static final ModConfigSpec.DoubleValue HOST_ROCK_CHANCE = BUILDER
        .comment("Chance to produce a host-rock pebble for each washed ore")
        .defineInRange("hostRockChance", 0.20D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue ORE_NUGGET_CHANCE = BUILDER
        .comment("Chance to produce an associated ore nugget for each washed ore")
        .defineInRange("oreNuggetChance", 0.03D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue GEM_CHANCE = BUILDER
        .comment("Chance to produce a configured gem for each washed ore")
        .defineInRange("gemChance", 0.005D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue DISABLE_DIRECT_ORE_MELTING = BUILDER
        .comment("Whether to disable direct melting of raw TFC ores in crucible/forge, forcing the crushing and washing pipeline")
        .define("disableDirectOreMelting", false);

    public enum PipelineMode {
        STANDARD,
        TEMPLATE_CLASSIC
    }

    public static final ModConfigSpec.EnumValue<PipelineMode> PIPELINE_MODE = BUILDER
        .comment("Mineral processing pipeline mode: STANDARD (concise single-step) or TEMPLATE_CLASSIC (multi-grade output with Create/IE/Quern/Panning)")
        .defineEnum("pipelineMode", PipelineMode.STANDARD);

    private static final Map<OreMaterial, ModConfigSpec.BooleanValue> ENABLED = new EnumMap<>(OreMaterial.class);
    private static final Map<OreMaterial, ModConfigSpec.BooleanValue> WASHING_ENABLED = new EnumMap<>(OreMaterial.class);
    private static final Map<OreMaterial, ModConfigSpec.DoubleValue> POWDER_OVERRIDES = new EnumMap<>(OreMaterial.class);
    private static final Map<OreMaterial, ModConfigSpec.DoubleValue> BYPRODUCT_OVERRIDES = new EnumMap<>(OreMaterial.class);

    static {
        BUILDER.push("ores");
        for (OreMaterial material : OreMaterial.values()) {
            String id = material.definition().id();
            // 没有 TFC 原版原生熔炼定义的外部扩展矿物（方铅、铝土、铬铁、沥青铀、锇）默认需外部模组支持
            boolean supportedByTfc = material != OreMaterial.GALENA
                && material != OreMaterial.BAUXITE
                && material != OreMaterial.CHROMITE
                && material != OreMaterial.URANINITE
                && material != OreMaterial.OSMIUM;
            ENABLED.put(material, BUILDER.comment("Whether this ore is available in the processing chain")
                .define(id + ".enabled", supportedByTfc));
            WASHING_ENABLED.put(material, BUILDER.define(id + ".washing", true));
            POWDER_OVERRIDES.put(material, BUILDER.defineInRange(id + ".powderYieldMultiplier", -1.0D, -1.0D, 16.0D));
            BYPRODUCT_OVERRIDES.put(material, BUILDER.defineInRange(id + ".byproductChanceMultiplier", -1.0D, -1.0D, 16.0D));
        }
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isOreEnabled(OreMaterial material) {
        return ENABLED.get(material).get();
    }

    public static boolean isWashingEnabled(OreMaterial material) {
        return isOreEnabled(material) && WASHING_ENABLED.get(material).get();
    }

    public static double powderMultiplier(OreMaterial material) {
        double override = POWDER_OVERRIDES.get(material).get();
        return override >= 0.0D ? override : POWDER_YIELD_MULTIPLIER.get();
    }

    public static double byproductMultiplier(OreMaterial material) {
        double override = BYPRODUCT_OVERRIDES.get(material).get();
        return override >= 0.0D ? override : BYPRODUCT_CHANCE_MULTIPLIER.get();
    }

    private Config() {
    }
}
