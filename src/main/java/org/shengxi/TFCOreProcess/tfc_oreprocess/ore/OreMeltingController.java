package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Config;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;

import java.util.Collection;

/**
 * 原矿直接熔炼控制逻辑。
 * 当整合包作者在配置中开启 disableDirectOreMelting 时，
 * 自动拦截并移除 TFC 原矿的 HeatingRecipe 缓存，强制玩家必须走粉碎与洗矿加工流水线。
 */
@EventBusSubscriber(modid = Tfc_oreprocess.MODID)
public final class OreMeltingController {
    public static final TagKey<Item> METAL_ORES_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("tfc", "metal_ores")
    );
    public static final TagKey<Item> ORE_PIECES_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("tfc", "ore_pieces")
    );

    private OreMeltingController() {
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        applyMeltingFilter();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        applyMeltingFilter();
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        applyMeltingFilter();
    }

    /**
     * 检查配置，若禁用原矿直接熔炼，则从 TFC 加热配方缓存中过滤掉原矿条目。
     */
    public static void applyMeltingFilter() {
        if (!Config.DISABLE_DIRECT_ORE_MELTING.get()) {
            return;
        }

        int filteredCount = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            boolean isTfcOre = (key != null && "tfc".equals(key.getNamespace()) && key.getPath().startsWith("ore/"))
                || item.getDefaultInstance().is(METAL_ORES_TAG)
                || item.getDefaultInstance().is(ORE_PIECES_TAG);

            if (isTfcOre) {
                Collection<HeatingRecipe> recipes = HeatingRecipe.CACHE.getAll(item);
                if (recipes != null && !recipes.isEmpty()) {
                    recipes.clear();
                    filteredCount++;
                }
            }
        }

        if (filteredCount > 0) {
            Tfc_oreprocess.LOGGER.info("已根据配置禁用 {} 种 TFC 原矿的直接燃烧熔炼配方", filteredCount);
        }
    }
}
