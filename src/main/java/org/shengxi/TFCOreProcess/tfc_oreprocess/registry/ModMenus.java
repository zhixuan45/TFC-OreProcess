package org.shengxi.TFCOreProcess.tfc_oreprocess.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.alloy.AlloyAssistantMenu;

/**
 * 模组菜单容器注册器。
 */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Tfc_oreprocess.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlloyAssistantMenu>> ALLOY_ASSISTANT =
        MENUS.register("alloy_assistant", () -> IMenuTypeExtension.create(AlloyAssistantMenu::new));

    private ModMenus() {
    }
}
