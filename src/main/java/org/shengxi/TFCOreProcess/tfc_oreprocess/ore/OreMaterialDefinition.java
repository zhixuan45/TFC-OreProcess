package org.shengxi.TFCOreProcess.tfc_oreprocess.ore;

/** 编译期矿物定义，保存加工流程需要的稳定数据。 */
public record OreMaterialDefinition(
    String id,
    String tfcSourceTag,
    String sourceItemId,
    String moltenFluidId,
    int baseMetalMb,
    float meltingTemperature
) {
    public static final int ROCKY_CRUSHED_ORE_METAL_MB = 5;
    public static final int CRUSHED_ORE_METAL_MB = 5;
    public static final int DIRTY_DUST_METAL_MB = 20;
    public static final int DIRTY_PILE_METAL_MB = 5;
    public static final int POWDER_METAL_MB = 5;
    public static final int SMALL_PELLET_METAL_MB = 25;
    public static final int LARGE_PELLET_METAL_MB = 100;

    public OreMaterialDefinition {
        if (id.isBlank() || tfcSourceTag.isBlank()) {
            throw new IllegalArgumentException("矿物标识和 TFC 来源标签不能为空");
        }
        if (sourceItemId.isBlank() || moltenFluidId.isBlank()) {
            throw new IllegalArgumentException("矿物来源物品和熔融流体不能为空");
        }
        if (baseMetalMb <= 0 || baseMetalMb % POWDER_METAL_MB != 0) {
            throw new IllegalArgumentException("基础金属量必须是 5 mB 的正整数倍");
        }
        if (meltingTemperature <= 0.0F) {
            throw new IllegalArgumentException("熔化温度必须大于零");
        }
    }

    /** 返回原矿对应的基础矿粉份数。 */
    public int basePowderCount() {
        return baseMetalMb / POWDER_METAL_MB;
    }
}
