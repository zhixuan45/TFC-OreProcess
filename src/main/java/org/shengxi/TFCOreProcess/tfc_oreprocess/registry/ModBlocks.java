package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;

/** 方块注册器。 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tfc_oreprocess.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tfc_oreprocess.MODID);

    public static final net.neoforged.neoforge.registries.DeferredBlock<org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantBlock> ALLOY_ASSISTANT =
        BLOCKS.registerBlock(
            "alloy_assistant",
            org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantBlock::new,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
        );

    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> ALLOY_ASSISTANT_ITEM =
        ITEMS.registerSimpleBlockItem("alloy_assistant", ALLOY_ASSISTANT);

    private ModBlocks() {
    }
}
