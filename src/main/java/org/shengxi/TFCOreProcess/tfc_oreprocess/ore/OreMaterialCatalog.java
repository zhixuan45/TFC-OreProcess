package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModOreItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 从编译期清单建立矿物目录，不扫描运行时配方。 */
public final class OreMaterialCatalog {
    private static final List<OreProcessData> MATERIALS = buildFixedMaterials();

    private OreMaterialCatalog() {
    }

    private static List<OreProcessData> buildFixedMaterials() {
        // 枚举是唯一数据入口，第三方配方变化不会改变矿物集合。
        return java.util.Arrays.stream(OreMaterial.values())
            .map(material -> {
                OreMaterialDefinition definition = material.definition();
                return new OreProcessData(
                    ResourceLocation.parse(definition.sourceItemId()),
                    ResourceLocation.parse(definition.moltenFluidId()),
                    definition.baseMetalMb(),
                    definition.meltingTemperature()
                );
            })
            .sorted(Comparator.comparing(data -> data.sourceItemId().toString()))
            .toList();
    }

    public static List<ItemStack> createCreativeStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (OreProcessData material : MATERIALS) {
            stacks.add(withMaterial(ModOreItems.ROCKY_CRUSHED_ORE.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.CRUSHED_ORE.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.DIRTY_DUST_ORE.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.DIRTY_PILE_ORE.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.PROCESSED_ORE.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.ORE_POWDER.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.ORE_PELLET_25MB.get().getDefaultInstance(), material));
            stacks.add(withMaterial(ModOreItems.ORE_PELLET_100MB.get().getDefaultInstance(), material));
        }
        return stacks;
    }

    public static List<ItemStack> createCrushedStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (OreProcessData material : MATERIALS) {
            ItemStack stack = ModOreItems.CRUSHED_ORE.get().getDefaultInstance();
            stack.set(ModDataComponents.ORE_PROCESS.get(), material);
            stacks.add(stack);
        }
        return stacks;
    }

    private static ItemStack withMaterial(ItemStack stack, OreProcessData material) {
        stack.set(ModDataComponents.ORE_PROCESS.get(), material);
        return stack;
    }
}
