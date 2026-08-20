package org.shengxi.TFCOreProcess.tfc_oreprocess.alloy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dries007.tfc.common.recipes.AlloyRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.util.AlloyRange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.shengxi.TFCOreProcess.tfc_oreprocess.Tfc_oreprocess;
import org.shengxi.TFCOreProcess.tfc_oreprocess.network.AlloyAssistantPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 合金助手客户端配置与监控界面。
 * 支持浏览选择游戏内所有 TFC 合金配方、调节目标容量、切换运行模式，并实时可视化坩埚流体状态。
 */
public class AlloyAssistantScreen extends AbstractContainerScreen<AlloyAssistantMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tfc_oreprocess.MODID, "textures/gui/alloy_assistant.png");

    private final List<RecipeHolder<AlloyRecipe>> availableAlloyRecipes = new ArrayList<>();
    private int selectedRecipeIndex = -1;
    private int recipeScrollOffset = 0;

    private Button btnPrevRecipe;
    private Button btnNextRecipe;
    private Button btnToggleAutoFeed;
    private Button btnCycleRedstone;
    private Button btnAdjustBatch;

    public AlloyAssistantScreen(AlloyAssistantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = 82;
    }

    @Override
    protected void init() {
        super.init();
        loadAlloyRecipes();

        // 切换配方按钮
        btnPrevRecipe = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (!availableAlloyRecipes.isEmpty()) {
                selectedRecipeIndex = (selectedRecipeIndex - 1 + availableAlloyRecipes.size()) % availableAlloyRecipes.size();
                syncConfigToServer();
            }
        }).bounds(leftPos + 8, topPos + 18, 14, 14).build());

        btnNextRecipe = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (!availableAlloyRecipes.isEmpty()) {
                selectedRecipeIndex = (selectedRecipeIndex + 1) % availableAlloyRecipes.size();
                syncConfigToServer();
            }
        }).bounds(leftPos + 96, topPos + 18, 14, 14).build());

        // 自动投料开关按钮
        btnToggleAutoFeed = this.addRenderableWidget(Button.builder(
            getAutoFeedText(),
            b -> {
                boolean nextState = !menu.isAutoFeedEnabled();
                syncConfigToServer(nextState, menu.getTargetBatchAmount(), menu.getRedstoneMode());
            }
        ).bounds(leftPos + 8, topPos + 52, 48, 14).build());

        // 红石模式按钮
        btnCycleRedstone = this.addRenderableWidget(Button.builder(
            getRedstoneText(),
            b -> {
                AlloyTargetConfig.RedstoneMode[] modes = AlloyTargetConfig.RedstoneMode.values();
                int nextOrd = (menu.getRedstoneMode().ordinal() + 1) % modes.length;
                syncConfigToServer(menu.isAutoFeedEnabled(), menu.getTargetBatchAmount(), modes[nextOrd]);
            }
        ).bounds(leftPos + 58, topPos + 52, 52, 14).build());

        // 目标容量按钮
        btnAdjustBatch = this.addRenderableWidget(Button.builder(
            Component.literal(menu.getTargetBatchAmount() + "mB"),
            b -> {
                int[] batches = {500, 1000, 1500, 2000, 3000};
                int current = menu.getTargetBatchAmount();
                int next = 3000;
                for (int i = 0; i < batches.length; i++) {
                    if (batches[i] > current) {
                        next = batches[i];
                        break;
                    }
                    if (i == batches.length - 1) {
                        next = batches[0];
                    }
                }
                syncConfigToServer(menu.isAutoFeedEnabled(), next, menu.getRedstoneMode());
            }
        ).bounds(leftPos + 8, topPos + 68, 56, 14).build());
    }

    private void loadAlloyRecipes() {
        availableAlloyRecipes.clear();
        if (minecraft != null && minecraft.level != null) {
            List<RecipeHolder<AlloyRecipe>> list = minecraft.level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.ALLOY.get());
            availableAlloyRecipes.addAll(list);

            // 还原当前方块实体已选定的配方
            if (menu.getBlockEntity() != null && menu.getBlockEntity().getConfig().getTargetRecipeId() != null) {
                ResourceLocation currentId = menu.getBlockEntity().getConfig().getTargetRecipeId();
                for (int i = 0; i < availableAlloyRecipes.size(); i++) {
                    if (availableAlloyRecipes.get(i).id().equals(currentId)) {
                        selectedRecipeIndex = i;
                        break;
                    }
                }
            } else if (!availableAlloyRecipes.isEmpty() && selectedRecipeIndex < 0) {
                selectedRecipeIndex = 0;
            }
        }
    }

    private Component getAutoFeedText() {
        return menu.isAutoFeedEnabled() ? Component.translatable("gui.tfc_oreprocess.autofeed.on") : Component.translatable("gui.tfc_oreprocess.autofeed.off");
    }

    private Component getRedstoneText() {
        return switch (menu.getRedstoneMode()) {
            case IGNORE -> Component.translatable("gui.tfc_oreprocess.redstone.ignore");
            case REQUIRE_SIGNAL -> Component.translatable("gui.tfc_oreprocess.redstone.require");
            case PULSE_ONCE -> Component.translatable("gui.tfc_oreprocess.redstone.pulse");
        };
    }

    private void syncConfigToServer() {
        syncConfigToServer(menu.isAutoFeedEnabled(), menu.getTargetBatchAmount(), menu.getRedstoneMode());
    }

    private void syncConfigToServer(boolean autoFeed, int targetBatch, AlloyTargetConfig.RedstoneMode redstoneMode) {
        String recipeIdStr = "";
        if (selectedRecipeIndex >= 0 && selectedRecipeIndex < availableAlloyRecipes.size()) {
            recipeIdStr = availableAlloyRecipes.get(selectedRecipeIndex).id().toString();
        }

        PacketDistributor.sendToServer(new AlloyAssistantPayload(
            menu.getBlockPos(),
            recipeIdStr,
            targetBatch,
            redstoneMode.ordinal(),
            autoFeed
        ));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制主背景底板
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 绘制 Crucible 流体容量指示条底槽
        int barX = x + 116;
        int barY = y + 76;
        int barWidth = 52;
        int barHeight = 5;
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

        // 填充当前流体量
        int curMb = menu.getCurrentCrucibleAmount();
        int filledWidth = Math.min(barWidth, (int) Math.round((double) curMb / 3000.0 * barWidth));
        if (filledWidth > 0) {
            graphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFFD4AF37);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 更新按钮文本状态
        btnToggleAutoFeed.setMessage(getAutoFeedText());
        btnCycleRedstone.setMessage(getRedstoneText());
        btnAdjustBatch.setMessage(Component.literal(menu.getTargetBatchAmount() + "mB"));

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // 绘制目标合金信息与比例
        renderAlloyInfo(graphics);
    }

    private void renderAlloyInfo(GuiGraphics graphics) {
        int x = leftPos + 24;
        int y = topPos + 21;

        if (selectedRecipeIndex >= 0 && selectedRecipeIndex < availableAlloyRecipes.size()) {
            RecipeHolder<AlloyRecipe> holder = availableAlloyRecipes.get(selectedRecipeIndex);
            AlloyRecipe recipe = holder.value();

            // 显示合金产物名称
            Component alloyName = Component.translatable(recipe.result().defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
            graphics.drawString(this.font, alloyName, x, y, 0x333333, false);

            // 显示配方成分要求
            int reqY = topPos + 34;
            StringBuilder sb = new StringBuilder();
            for (AlloyRange range : recipe.contents()) {
                String fluidName = range.fluid().defaultFluidState().createLegacyBlock().getBlock().getName().getString();
                int minPct = (int) Math.round(range.min() * 100);
                int maxPct = (int) Math.round(range.max() * 100);
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }
                sb.append(minPct).append("~").append(maxPct).append("%");
            }
            graphics.drawString(this.font, Component.literal(sb.toString()), leftPos + 8, reqY, 0x666666, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.tfc_oreprocess.no_recipe_selected"), x, y, 0x888888, false);
        }

        // 显示求解与坩埚状态
        int statusY = topPos + 84;
        Component statusText = switch (menu.getSolverStatus()) {
            case SUCCESS -> Component.translatable("gui.tfc_oreprocess.status.success");
            case ALREADY_MATCHED -> Component.translatable("gui.tfc_oreprocess.status.already_matched");
            case IMPURITY_DETECTED -> Component.translatable("gui.tfc_oreprocess.status.impurity");
            case NO_MATERIALS -> Component.translatable("gui.tfc_oreprocess.status.no_materials");
            case INSUFFICIENT_MATERIALS -> Component.translatable("gui.tfc_oreprocess.status.insufficient");
            case CRUCIBLE_FULL -> Component.translatable("gui.tfc_oreprocess.status.crucible_full");
        };
        int statusColor = switch (menu.getSolverStatus()) {
            case SUCCESS, ALREADY_MATCHED -> 0x2E7D32;
            case IMPURITY_DETECTED -> 0xC62828;
            default -> 0xEF6C00;
        };
        graphics.drawString(this.font, statusText, leftPos + 68, topPos + 68, statusColor, false);
    }
}
