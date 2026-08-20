package org.shengxi.TFCOreProcess.tfc_oreprocess.washing;

import org.shengxi.TFCOreProcess.tfc_oreprocess.Config;
import org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreMaterial;
import net.minecraft.util.RandomSource;

/** 集中计算洗矿产量，保证矿粉与副产物倍率彼此独立。 */
public final class WashingYieldCalculator {
    private WashingYieldCalculator() {
    }

    /**
     * 计算一个 5 mB 基础单位应生成的矿粉份数。
     * 小数部分使用一次独立概率判定，因此默认 1.5 等价于 1+50%。
     */
    public static int powderCount(int basePowderCount, double randomValue) {
        return powderCount(basePowderCount, null, randomValue, null);
    }

    public static int powderCount(int basePowderCount, OreMaterial material, RandomSource random) {
        return powderCount(basePowderCount, material, 0.0D, random);
    }

    private static int powderCount(int basePowderCount, OreMaterial material, double randomValue, RandomSource random) {
        if (basePowderCount <= 0) {
            return 0;
        }
        double multiplier = material == null ? Config.POWDER_YIELD_MULTIPLIER.get() : Config.powderMultiplier(material);
        int guaranteed = (int) Math.floor(multiplier);
        double fractionalChance = multiplier - guaranteed;
        int result = basePowderCount * guaranteed;
        for (int i = 0; i < basePowderCount; i++) {
            double unitRandom = random == null ? randomValueForUnit(randomValue, i) : random.nextDouble();
            if (unitRandom < fractionalChance) {
                result++;
            }
        }
        return result;
    }

    /** 为每个基础单位派生稳定的伪随机值，实际游戏调用会传入随机源值。 */
    private static double randomValueForUnit(double randomValue, int unitIndex) {
        long bits = Double.doubleToLongBits(randomValue) + unitIndex * 0x9E3779B97F4A7C15L;
        bits ^= bits >>> 30;
        bits *= 0xBF58476D1CE4E5B9L;
        bits ^= bits >>> 27;
        bits *= 0x94D049BB133111EBL;
        bits ^= bits >>> 31;
        return (bits >>> 11) * 0x1.0p-53;
    }

    /** 返回应用独立副产物倍率后的概率，并限制在合法概率范围内。 */
    public static double byproductChance(double baseChance) {
        if (!Config.ENABLE_BYPRODUCTS.get() || baseChance <= 0.0D) {
            return 0.0D;
        }
        return Math.min(1.0D, baseChance * Config.BYPRODUCT_CHANCE_MULTIPLIER.get());
    }
}
