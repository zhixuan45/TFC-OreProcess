package org.shengxi.TFCOreProcess.tfc_oreprocess.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantBlockEntity;
import org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyTargetConfig;

/**
 * 客户端配置同步数据包。
 * 用于将玩家在 GUI 中选中的目标合金配方、目标熔炼量及运行模式同步到服务端方块实体。
 */
public record AlloyAssistantPayload(
    BlockPos pos,
    String recipeId,
    int targetBatch,
    int redstoneModeOrdinal,
    boolean autoFeed
) implements CustomPacketPayload {

    public static final Type<AlloyAssistantPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Tfc_oreprocess.MODID, "alloy_assistant_config")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyAssistantPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, AlloyAssistantPayload::pos,
        ByteBufCodecs.STRING_UTF8, AlloyAssistantPayload::recipeId,
        ByteBufCodecs.VAR_INT, AlloyAssistantPayload::targetBatch,
        ByteBufCodecs.VAR_INT, AlloyAssistantPayload::redstoneModeOrdinal,
        ByteBufCodecs.BOOL, AlloyAssistantPayload::autoFeed,
        AlloyAssistantPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AlloyAssistantPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.pos()) instanceof AlloyAssistantBlockEntity be) {
                if (payload.recipeId().isEmpty()) {
                    be.getConfig().setTargetRecipeId(null);
                } else {
                    be.getConfig().setTargetRecipeId(ResourceLocation.tryParse(payload.recipeId()));
                }
                be.getConfig().setTargetBatchAmount(payload.targetBatch());
                if (payload.redstoneModeOrdinal() >= 0 && payload.redstoneModeOrdinal() < AlloyTargetConfig.RedstoneMode.values().length) {
                    be.getConfig().setRedstoneMode(AlloyTargetConfig.RedstoneMode.values()[payload.redstoneModeOrdinal()]);
                }
                be.getConfig().setAutoFeedEnabled(payload.autoFeed());
                be.setChanged();
            }
        });
    }
}
