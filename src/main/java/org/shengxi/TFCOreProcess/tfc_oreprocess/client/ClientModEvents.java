package org.shengxi.TFCOreProcess.tfc_oreprocess.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantScreen;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModMenus;

/**
 * 客户端模组事件监听器。
 * 负责注册 GUI 屏幕与其他客户端渲染组件。
 */
@EventBusSubscriber(modid = Tfc_oreprocess.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALLOY_ASSISTANT.get(), AlloyAssistantScreen::new);
    }
}
