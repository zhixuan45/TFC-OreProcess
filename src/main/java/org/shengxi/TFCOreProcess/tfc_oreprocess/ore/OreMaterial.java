package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

import net.minecraft.resources.ResourceLocation;

/** 编译期固定的矿物清单；新增矿物必须显式加入这里。 */
public enum OreMaterial {
    COPPER("copper", "tfc:ores/native_copper", "tfc:ore/normal_native_copper", "tfc:metal/copper", 25, 1080.0F),
    GOLD("gold", "tfc:ores/native_gold", "tfc:ore/normal_native_gold", "tfc:metal/gold", 25, 1060.0F),
    SILVER("silver", "tfc:ores/native_silver", "tfc:ore/normal_native_silver", "tfc:metal/silver", 25, 961.0F),
    TIN("tin", "tfc:ores/cassiterite", "tfc:ore/normal_cassiterite", "tfc:metal/tin", 25, 230.0F),
    BISMUTH("bismuth", "tfc:ores/bismuthinite", "tfc:ore/normal_bismuthinite", "tfc:metal/bismuth", 25, 270.0F),
    ZINC("zinc", "tfc:ores/sphalerite", "tfc:ore/normal_sphalerite", "tfc:metal/zinc", 25, 420.0F),
    IRON("iron", "tfc:ores/hematite", "tfc:ore/normal_hematite", "tfc:metal/cast_iron", 25, 1535.0F),
    NICKEL("nickel", "tfc:ores/garnierite", "tfc:ore/normal_garnierite", "tfc:metal/nickel", 25, 1453.0F),
    LIMONITE("limonite", "tfc:ores/limonite", "tfc:ore/normal_limonite", "tfc:metal/cast_iron", 25, 1535.0F),
    MAGNETITE("magnetite", "tfc:ores/magnetite", "tfc:ore/normal_magnetite", "tfc:metal/cast_iron", 25, 1535.0F),
    MALACHITE("malachite", "tfc:ores/malachite", "tfc:ore/normal_malachite", "tfc:metal/copper", 25, 1080.0F),
    TETRAHEDRITE("tetrahedrite", "tfc:ores/tetrahedrite", "tfc:ore/normal_tetrahedrite", "tfc:metal/copper", 25, 1080.0F),
    GALENA("galena", "tfc:ores/galena", "tfc:ore/normal_galena", "tfc:metal/lead", 25, 327.0F),
    BAUXITE("bauxite", "tfc:ores/bauxite", "tfc:ore/normal_bauxite", "tfc:metal/aluminum", 25, 660.0F),
    CHROMITE("chromite", "tfc:ores/chromite", "tfc:ore/normal_chromite", "tfc:metal/chromium", 25, 1907.0F),
    URANINITE("uraninite", "tfc:ores/uraninite", "tfc:ore/normal_uraninite", "tfc:metal/uranium", 25, 1132.0F),
    // 兼容 MekaTFC 模组的原生锇矿，熔点对齐 MekaTFC 定义的 1540°C
    OSMIUM("osmium", "tfc:ores/native_osmium", "mekatfc:ore/normal_native_osmium", "mekatfc:metal/osmium", 25, 1540.0F);

    private final OreMaterialDefinition definition;

    OreMaterial(String id, String tfcSourceTag, String sourceItemId, String moltenFluidId,
                int baseMetalMb, float meltingTemperature) {
        this.definition = new OreMaterialDefinition(
            id, tfcSourceTag, sourceItemId, moltenFluidId, baseMetalMb, meltingTemperature
        );
    }

    public OreMaterialDefinition definition() {
        return definition;
    }

    public static OreMaterial fromId(String id) {
        for (OreMaterial material : values()) {
            if (material.definition.id().equals(id)) {
                return material;
            }
        }
        return null;
    }

    public static OreMaterial fromSourceItemId(ResourceLocation sourceItemId) {
        for (OreMaterial material : values()) {
            if (material.definition.sourceItemId().equals(sourceItemId.toString())) {
                return material;
            }
        }
        return null;
    }
}
