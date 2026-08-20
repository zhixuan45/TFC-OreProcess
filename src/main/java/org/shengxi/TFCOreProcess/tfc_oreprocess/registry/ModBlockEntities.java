package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;

/** 方块实体注册器。 */
public final class ModBlockEntities {
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Tfc_oreprocess.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<
        net.minecraft.world.level.block.entity.BlockEntityType<?>,
        net.minecraft.world.level.block.entity.BlockEntityType<org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantBlockEntity>
    > ALLOY_ASSISTANT = BLOCK_ENTITIES.register(
        "alloy_assistant",
        () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
            org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantBlockEntity::new,
            ModBlocks.ALLOY_ASSISTANT.get()
        ).build(null)
    );

    private ModBlockEntities() {
    }
}
