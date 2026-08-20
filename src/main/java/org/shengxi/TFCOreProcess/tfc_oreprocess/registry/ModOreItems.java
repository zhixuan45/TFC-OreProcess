package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreMaterial;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreMaterialDefinition;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreProcessItem;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;

import java.util.EnumMap;
import java.util.Map;

/** 矿石加工物品的独立延迟注册器。 */
public final class ModOreItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tfc_oreprocess.MODID);

    /** 通用加工物品 */
    public static final DeferredItem<Item> ROCKY_CRUSHED_ORE = ITEMS.registerSimpleItem("rocky_crushed_ore");
    public static final DeferredItem<Item> CRUSHED_ORE = ITEMS.registerSimpleItem("crushed_ore");
    public static final DeferredItem<Item> DIRTY_DUST_ORE = ITEMS.registerSimpleItem("dirty_dust_ore");
    public static final DeferredItem<Item> DIRTY_PILE_ORE = ITEMS.registerSimpleItem("dirty_pile_ore");
    public static final DeferredItem<Item> PROCESSED_ORE = ITEMS.registerSimpleItem("processed_ore");
    public static final DeferredItem<Item> ORE_POWDER = ITEMS.registerSimpleItem("ore_powder");
    public static final DeferredItem<Item> ORE_PELLET_25MB = ITEMS.registerSimpleItem("ore_pellet_25mb");
    public static final DeferredItem<Item> ORE_PELLET_100MB = ITEMS.registerSimpleItem("ore_pellet_100mb");

    /** 兼容旧引用字段 */
    public static DeferredItem<Item> ROCKY_CRUSHED_COPPER_ORE;
    public static DeferredItem<Item> CRUSHED_COPPER_ORE;
    public static DeferredItem<Item> COPPER_DIRTY_DUST_ORE;
    public static DeferredItem<Item> COPPER_DIRTY_PILE_ORE;
    public static DeferredItem<Item> COPPER_ORE_POWDER;
    public static DeferredItem<Item> COPPER_ORE_PELLET_25MB;
    public static DeferredItem<Item> COPPER_ORE_PELLET_100MB;

    private static final Map<OreMaterial, OreItemSet> FIXED_ITEMS = new EnumMap<>(OreMaterial.class);

    static {
        for (OreMaterial material : OreMaterial.values()) {
            String id = material.definition().id();
            FIXED_ITEMS.put(material, new OreItemSet(
                // 1. 碎块 (粗碎矿)
                ITEMS.register("rocky_crushed_" + id + "_ore", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.ROCKY_CRUSHED_ORE_METAL_MB)),
                // 2. 细碎矿
                ITEMS.register("crushed_" + id + "_ore", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.CRUSHED_ORE_METAL_MB)),
                // 3. 精碎矿 (研磨产物)
                ITEMS.register("dirty_dust_" + id + "_ore", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.DIRTY_DUST_METAL_MB)),
                // 4. 残碎矿 (洗矿副产物)
                ITEMS.register("dirty_pile_" + id + "_ore", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.DIRTY_PILE_METAL_MB)),
                // 5. 矿粉 (纯净矿粉)
                ITEMS.register(id + "_ore_powder", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.POWDER_METAL_MB)),
                // 6. 25mB 矿团
                ITEMS.register(id + "_ore_pellet_25mb", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.SMALL_PELLET_METAL_MB)),
                // 7. 100mB 矿团
                ITEMS.register(id + "_ore_pellet_100mb", () -> new OreProcessItem(
                    new Item.Properties(), material, OreMaterialDefinition.LARGE_PELLET_METAL_MB))
            ));
        }
        // 兼容旧 Java 调用点，但不再次向注册表提交重复 ID。
        OreItemSet copper = FIXED_ITEMS.get(OreMaterial.COPPER);
        ROCKY_CRUSHED_COPPER_ORE = copper.rockyCrushed();
        CRUSHED_COPPER_ORE = copper.crushed();
        COPPER_DIRTY_DUST_ORE = copper.dirtyDust();
        COPPER_DIRTY_PILE_ORE = copper.dirtyPile();
        COPPER_ORE_POWDER = copper.powder();
        COPPER_ORE_PELLET_25MB = copper.pellet25();
        COPPER_ORE_PELLET_100MB = copper.pellet100();

        // 注册旧模板模组（tfcorewashing）及旧版本物品别名映射以兼容旧存档
        LegacyModItemMigration.registerAliases(ITEMS);
    }

    public static OreItemSet forMaterial(OreMaterial material) {
        return FIXED_ITEMS.get(material);
    }

    public static Iterable<OreItemSet> fixedItems() {
        return FIXED_ITEMS.values();
    }

    public record OreItemSet(
        DeferredItem<Item> rockyCrushed,
        DeferredItem<Item> crushed,
        DeferredItem<Item> dirtyDust,
        DeferredItem<Item> dirtyPile,
        DeferredItem<Item> powder,
        DeferredItem<Item> pellet25,
        DeferredItem<Item> pellet100
    ) {
    }

    private ModOreItems() {
    }
}
