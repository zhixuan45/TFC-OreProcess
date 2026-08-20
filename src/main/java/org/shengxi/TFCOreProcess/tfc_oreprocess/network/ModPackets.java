package org.shengxi.TFCOreProcess.tfc_oreprocess.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;

/**
 * 模组网络数据包注册器。
 */
public final class ModPackets {
    private ModPackets() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Tfc_oreprocess.MODID).versioned("1.0.0");
        registrar.playToServer(
            AlloyAssistantPayload.TYPE,
            AlloyAssistantPayload.STREAM_CODEC,
            AlloyAssistantPayload::handle
        );
    }
}
