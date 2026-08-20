package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;

import java.util.Map;

/**
 * 模板模组（tfcorewashing / tfc-ore-washing）旧存档物品兼容与别名迁移映射。
 * 利用 NeoForge 原生 DeferredRegister.addAlias 机制，
 * 在加载包含旧模组物品的旧世界/存档时自动将旧物品无损迁移为本模组的新对应物品。
 */
public final class LegacyModItemMigration {
    private static final String OLD_MODID = "tfcorewashing";

    // 旧模组矿石名称到本模组矿物标识的映射
    private static final Map<String, String> ORE_NAME_MAPPING = Map.ofEntries(
        Map.entry("copper", "copper"),
        Map.entry("gold", "gold"),
        Map.entry("silver", "silver"),
        Map.entry("cassiterite", "tin"),
        Map.entry("bismuthinite", "bismuth"),
        Map.entry("sphalerite", "zinc"),
        Map.entry("hematite", "iron"),
        Map.entry("garnierite", "nickel"),
        Map.entry("limonite", "limonite"),
        Map.entry("magnetite", "magnetite"),
        Map.entry("malachite", "malachite"),
        Map.entry("tetrahedrite", "tetrahedrite"),
        Map.entry("galena", "galena"),
        Map.entry("bauxite", "bauxite"),
        Map.entry("chromite", "chromite"),
        Map.entry("chromium", "chromite"),
        Map.entry("uraninite", "uraninite"),
        Map.entry("osmium", "osmium")
    );

    private LegacyModItemMigration() {
    }

    /**
     * 向物品注册器注入旧模组 ID 别名。
     */
    public static void registerAliases(DeferredRegister.Items items) {
        // 1. 迁移固定金属矿物的碎矿、矿粉与矿团
        for (Map.Entry<String, String> entry : ORE_NAME_MAPPING.entrySet()) {
            String oldOre = entry.getKey();
            String newOre = entry.getValue();

            // 碎矿与带岩粗碎矿
            alias(items, "chunks_" + oldOre, "crushed_" + newOre + "_ore");
            alias(items, "rocky_chunks_" + oldOre, "rocky_crushed_" + newOre + "_ore");

            // 精碎矿与残碎矿
            alias(items, "dirty_dust_" + oldOre, "dirty_dust_" + newOre + "_ore");
            alias(items, "dirty_pile_" + oldOre, "dirty_pile_" + newOre + "_ore");

            // 矿团
            alias(items, "pellet_" + oldOre, newOre + "_ore_pellet_25mb");
            alias(items, "briquet_" + oldOre, newOre + "_ore_pellet_100mb");

            // 兼容可能存在的 20mb/80mb 命名存档
            aliasInternal(items, newOre + "_ore_pellet_20mb", newOre + "_ore_pellet_25mb");
            aliasInternal(items, newOre + "_ore_pellet_80mb", newOre + "_ore_pellet_100mb");
        }

        // 2. 特殊独立命名项
        alias(items, "chromium_powder", "chromite_ore_powder");
        alias(items, "pellet_chromium", "chromite_ore_pellet_25mb");
        alias(items, "briquet_chromium", "chromite_ore_pellet_100mb");
        aliasInternal(items, "chromite_ore_pellet_20mb", "chromite_ore_pellet_25mb");
        aliasInternal(items, "chromite_ore_pellet_80mb", "chromite_ore_pellet_100mb");
        aliasInternal(items, "ore_pellet_20mb", "ore_pellet_25mb");
        aliasInternal(items, "ore_pellet_80mb", "ore_pellet_100mb");

        // 3. 非金属/特殊矿物迁移至通用加工物品
        String[] miscOres = {"cinnabar", "cryolite", "graphite", "sulfur"};
        for (String misc : miscOres) {
            alias(items, "chunks_" + misc, "crushed_ore");
            alias(items, "rocky_chunks_" + misc, "rocky_crushed_ore");
            alias(items, "dirty_dust_" + misc, "ore_powder");
            alias(items, "dirty_pile_" + misc, "ore_powder");
        }

        // 4. 沙子与杂项
        alias(items, "rock_powder", "ore_powder");
        String[] sandColors = {"black", "brown", "green", "pink", "red", "white", "yellow"};
        for (String color : sandColors) {
            aliasExternal(items, "pile_" + color + "_sand", ResourceLocation.fromNamespaceAndPath("tfc", "sand/" + color));
        }

        Tfc_oreprocess.LOGGER.info("已注册模板模组（tfcorewashing）与历史版本的旧存档物品别名映射表");
    }

    private static void aliasInternal(DeferredRegister.Items items, String oldName, String newName) {
        items.addAlias(
            ResourceLocation.fromNamespaceAndPath(Tfc_oreprocess.MODID, oldName),
            ResourceLocation.fromNamespaceAndPath(Tfc_oreprocess.MODID, newName)
        );
    }

    private static void alias(DeferredRegister.Items items, String oldName, String newName) {
        items.addAlias(
            ResourceLocation.fromNamespaceAndPath(OLD_MODID, oldName),
            ResourceLocation.fromNamespaceAndPath(Tfc_oreprocess.MODID, newName)
        );
    }

    private static void aliasExternal(DeferredRegister.Items items, String oldName, ResourceLocation target) {
        items.addAlias(
            ResourceLocation.fromNamespaceAndPath(OLD_MODID, oldName),
            target
        );
    }
}
