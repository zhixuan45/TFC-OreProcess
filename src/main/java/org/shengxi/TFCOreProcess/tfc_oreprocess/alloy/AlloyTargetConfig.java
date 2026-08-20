package org.shengxi.TFCOreProcess.tfc_oreprocess.alloy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 合金助手运行与目标配置数据结构。
 * 负责记录当前选定的目标合金配方、目标熔炼总量限制及运行模式。
 */
public class AlloyTargetConfig {
    private ResourceLocation targetRecipeId = null;
    private int targetBatchAmount = 3000; // 目标批量总量（毫桶 mB），默认满坩埚 3000 mB
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private boolean autoFeedEnabled = true; // 是否开启自动投料

    public enum RedstoneMode {
        IGNORE,         // 忽略红石信号，持续工作
        REQUIRE_SIGNAL, // 仅在有红石信号时工作
        PULSE_ONCE      // 接收到红石脉冲时仅执行一次完整配料投料
    }

    public AlloyTargetConfig() {
    }

    public ResourceLocation getTargetRecipeId() {
        return targetRecipeId;
    }

    public void setTargetRecipeId(ResourceLocation targetRecipeId) {
        this.targetRecipeId = targetRecipeId;
    }

    public int getTargetBatchAmount() {
        return targetBatchAmount;
    }

    public void setTargetBatchAmount(int targetBatchAmount) {
        this.targetBatchAmount = Math.max(10, Math.min(3000, targetBatchAmount));
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode == null ? RedstoneMode.IGNORE : redstoneMode;
    }

    public boolean isAutoFeedEnabled() {
        return autoFeedEnabled;
    }

    public void setAutoFeedEnabled(boolean autoFeedEnabled) {
        this.autoFeedEnabled = autoFeedEnabled;
    }

    /** 保存配置到 NBT */
    public CompoundTag save(HolderLookup.Provider lookupProvider) {
        CompoundTag tag = new CompoundTag();
        if (targetRecipeId != null) {
            tag.putString("TargetRecipeId", targetRecipeId.toString());
        }
        tag.putInt("TargetBatchAmount", targetBatchAmount);
        tag.putString("RedstoneMode", redstoneMode.name());
        tag.putBoolean("AutoFeedEnabled", autoFeedEnabled);
        return tag;
    }

    /** 从 NBT 读取配置 */
    public void load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        if (tag.contains("TargetRecipeId")) {
            targetRecipeId = ResourceLocation.tryParse(tag.getString("TargetRecipeId"));
        } else {
            targetRecipeId = null;
        }

        if (tag.contains("TargetBatchAmount")) {
            targetBatchAmount = tag.getInt("TargetBatchAmount");
        }

        if (tag.contains("RedstoneMode")) {
            try {
                redstoneMode = RedstoneMode.valueOf(tag.getString("RedstoneMode"));
            } catch (Exception ignored) {
                redstoneMode = RedstoneMode.IGNORE;
            }
        }

        if (tag.contains("AutoFeedEnabled")) {
            autoFeedEnabled = tag.getBoolean("AutoFeedEnabled");
        }
    }
}
