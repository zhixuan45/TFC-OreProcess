package org.shengxi.TFCOreProcess.tfc_oreprocess;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModBlockEntities;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModBlocks;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModConditions;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModOreItems;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModRecipes;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreMaterial;
import org.slf4j.Logger;

/** 模组入口，仅负责连接注册器和生命周期配置。 */
@Mod(Tfc_oreprocess.MODID)
public final class Tfc_oreprocess {
    public static final String MODID = "tfc_oreprocess";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tfc_oreprocess"))
            .icon(() -> ModOreItems.forMaterial(OreMaterial.COPPER).crushed().get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.ALLOY_ASSISTANT_ITEM.get());
                for (OreMaterial material : OreMaterial.values()) {
                    ModOreItems.OreItemSet items = ModOreItems.forMaterial(material);
                    // 1. 碎块
                    output.accept(items.rockyCrushed().get());
                    // 2. 细碎
                    output.accept(items.crushed().get());
                    // 3. 精碎
                    output.accept(items.dirtyDust().get());
                    // 4. 残碎
                    output.accept(items.dirtyPile().get());
                    // 5. 粉末
                    output.accept(items.powder().get());
                    // 6. 25mB 矿团
                    output.accept(items.pellet25().get());
                    // 7. 100mB 矿团
                    output.accept(items.pellet100().get());
                }
            })
            .build()
    );

    public Tfc_oreprocess(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModOreItems.ITEMS.register(modEventBus);
        org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModMenus.MENUS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModConditions.CONDITION_CODECS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(org.shengxi.TFCOreProcess.tfc_oreprocess.network.ModPackets::register);
        modEventBus.addListener(org.shengxi.TFCOreProcess.tfc_oreprocess.client.ClientModEvents::registerScreens);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
