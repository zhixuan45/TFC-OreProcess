package org.shengxi.TFCOreProcess.tfc_oreprocess.alloy;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.common.recipes.AlloyRecipe;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.util.AlloyRange;
import net.dries007.tfc.util.FluidAlloy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.*;

/**
 * 合金助手智能配比数学求解器。
 * 负责分析坩埚现有金属熔液、目标合金配方合法比例区间，
 * 并对可用原料进行组合求解，得出合规的投料方案。
 */
public final class AlloyAssistantSolver {

    private AlloyAssistantSolver() {
    }

    /** 描述一个可用的输入物品来源 */
    public static class AvailableSourceItem {
        public final int slotIndex;
        public final ItemStack stack;
        public final Fluid fluid;
        public final int amountPerItem; // 单个物品熔化后产生的 mB
        public final boolean isInternalSlot;

        public AvailableSourceItem(int slotIndex, ItemStack stack, Fluid fluid, int amountPerItem, boolean isInternalSlot) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.fluid = fluid;
            this.amountPerItem = amountPerItem;
            this.isInternalSlot = isInternalSlot;
        }
    }

    /** 待投入坩埚的物品指令 */
    public static class PlannedFeedItem {
        public final int slotIndex;
        public final boolean isInternalSlot;
        public final ItemStack itemSample;
        public final int count;
        public final Fluid fluid;
        public final int totalMb;

        public PlannedFeedItem(int slotIndex, boolean isInternalSlot, ItemStack itemSample, int count, Fluid fluid, int totalMb) {
            this.slotIndex = slotIndex;
            this.isInternalSlot = isInternalSlot;
            this.itemSample = itemSample;
            this.count = count;
            this.fluid = fluid;
            this.totalMb = totalMb;
        }
    }

    /** 求解结果状态 */
    public enum SolveStatus {
        SUCCESS,                // 成功求得合法配料方案
        ALREADY_MATCHED,        // 坩埚内现有熔液已完全符合目标配方且无需补充
        IMPURITY_DETECTED,      // 坩埚内存在配方外的杂质流体
        NO_MATERIALS,           // 未检测到可用的配方所需原料
        INSUFFICIENT_MATERIALS, // 材料不足以配平至合法区间
        CRUCIBLE_FULL           // 坩埚已满
    }

    /** 求解输出结果 */
    public static class SolveResult {
        public final SolveStatus status;
        public final List<PlannedFeedItem> plannedItems;
        public final Map<Fluid, Integer> projectedFluidAmounts;
        public final int totalProjectedMb;
        public final String statusMessage;

        public SolveResult(SolveStatus status, List<PlannedFeedItem> plannedItems, Map<Fluid, Integer> projectedFluidAmounts, int totalProjectedMb, String statusMessage) {
            this.status = status;
            this.plannedItems = plannedItems == null ? Collections.emptyList() : plannedItems;
            this.projectedFluidAmounts = projectedFluidAmounts == null ? Collections.emptyMap() : projectedFluidAmounts;
            this.totalProjectedMb = totalProjectedMb;
            this.statusMessage = statusMessage;
        }

        public static SolveResult failure(SolveStatus status, String message) {
            return new SolveResult(status, null, null, 0, message);
        }
    }

    /**
     * 执行智能合金配料计算
     *
     * @param currentAlloy 坩埚当前合金流体
     * @param targetRecipe 目标合金配方
     * @param availableItems 扫描到的可用原料列表
     * @param targetCapacity 设定的最大批量（默认 3000 mB）
     * @param availableCrucibleSlots 坩埚当前可用输入槽位数
     * @return 求解结果
     */
    public static SolveResult solve(
        FluidAlloy currentAlloy,
        AlloyRecipe targetRecipe,
        List<AvailableSourceItem> availableItems,
        int targetCapacity,
        int availableCrucibleSlots
    ) {
        if (targetRecipe == null) {
            return SolveResult.failure(SolveStatus.NO_MATERIALS, "未指定目标合金配方");
        }

        List<AlloyRange> ranges = targetRecipe.contents();
        if (ranges.isEmpty()) {
            return SolveResult.failure(SolveStatus.NO_MATERIALS, "配方内容为空");
        }

        // 收集配方中允许的所有成分流体
        Set<Fluid> allowedFluids = new HashSet<>();
        Map<Fluid, AlloyRange> rangeMap = new HashMap<>();
        for (AlloyRange range : ranges) {
            allowedFluids.add(range.fluid());
            rangeMap.put(range.fluid(), range);
        }

        // 分析坩埚现有成分
        int currentTotalMb = currentAlloy != null ? currentAlloy.getAmount() : 0;
        Map<Fluid, Integer> currentFluids = new HashMap<>();
        if (currentAlloy != null && currentTotalMb > 0) {
            Object2DoubleMap<Fluid> content = currentAlloy.getContent();
            for (Object2DoubleMap.Entry<Fluid> entry : content.object2DoubleEntrySet()) {
                Fluid f = entry.getKey();
                double fraction = entry.getDoubleValue();
                int mb = (int) Math.round(fraction * currentTotalMb);
                if (mb > 0) {
                    if (!allowedFluids.contains(f)) {
                        return SolveResult.failure(SolveStatus.IMPURITY_DETECTED, "坩埚中存在未知杂质金属，无法熔炼目标合金");
                    }
                    currentFluids.put(f, mb);
                }
            }
        }

        // 过滤可用物品，仅保留属于目标合金组分的原料
        Map<Fluid, List<AvailableSourceItem>> itemsByFluid = new HashMap<>();
        for (Fluid f : allowedFluids) {
            itemsByFluid.put(f, new ArrayList<>());
        }

        for (AvailableSourceItem item : availableItems) {
            if (allowedFluids.contains(item.fluid) && item.amountPerItem > 0 && !item.stack.isEmpty()) {
                itemsByFluid.get(item.fluid).add(item);
            }
        }

        // 对每种流体的原料按单位金属量降序排序（100mB 锭 -> 25mB 矿团 -> 5mB 矿粉）
        for (List<AvailableSourceItem> list : itemsByFluid.values()) {
            list.sort((a, b) -> Integer.compare(b.amountPerItem, a.amountPerItem));
        }

        // 检查原料是否完全缺失
        boolean hasAnyMaterial = itemsByFluid.values().stream().anyMatch(list -> !list.isEmpty());
        if (!hasAnyMaterial && currentTotalMb == 0) {
            return SolveResult.failure(SolveStatus.NO_MATERIALS, "缺少熔炼所需的金属原料");
        }

        int maxMb = Math.min(3000, targetCapacity);
        int remainingCapacity = maxMb - currentTotalMb;
        if (remainingCapacity <= 0) {
            if (currentAlloy != null && currentAlloy.matches(targetRecipe)) {
                return new SolveResult(SolveStatus.ALREADY_MATCHED, null, currentFluids, currentTotalMb, "坩埚已达目标容量且比例符合配方");
            }
            return SolveResult.failure(SolveStatus.CRUCIBLE_FULL, "坩埚已满且比例不符合要求");
        }

        // 核心配比搜索：启发式离散搜索
        SolvePlan bestPlan = searchOptimalFeedingPlan(
            currentFluids,
            currentTotalMb,
            rangeMap,
            itemsByFluid,
            remainingCapacity,
            targetRecipe,
            availableCrucibleSlots
        );

        if (bestPlan == null || bestPlan.feedItems.isEmpty()) {
            if (currentAlloy != null && currentAlloy.matches(targetRecipe)) {
                return new SolveResult(SolveStatus.ALREADY_MATCHED, null, currentFluids, currentTotalMb, "坩埚现有熔液已符合配方比例");
            }
            return SolveResult.failure(SolveStatus.INSUFFICIENT_MATERIALS, "现有原料不足以将坩埚调配至目标合金比例");
        }

        Map<Fluid, Integer> finalFluids = new HashMap<>(currentFluids);
        for (Map.Entry<Fluid, Integer> entry : bestPlan.addedFluids.entrySet()) {
            finalFluids.put(entry.getKey(), finalFluids.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }

        return new SolveResult(
            SolveStatus.SUCCESS,
            bestPlan.feedItems,
            finalFluids,
            bestPlan.finalTotalMb,
            "成功计算最佳投料方案"
        );
    }

    private static class SolvePlan {
        final List<PlannedFeedItem> feedItems;
        final Map<Fluid, Integer> addedFluids;
        final int finalTotalMb;

        SolvePlan(List<PlannedFeedItem> feedItems, Map<Fluid, Integer> addedFluids, int finalTotalMb) {
            this.feedItems = feedItems;
            this.addedFluids = addedFluids;
            this.finalTotalMb = finalTotalMb;
        }
    }

    /**
     * 多策略启发式搜索投料方案
     */
    private static SolvePlan searchOptimalFeedingPlan(
        Map<Fluid, Integer> currentFluids,
        int currentTotalMb,
        Map<Fluid, AlloyRange> rangeMap,
        Map<Fluid, List<AvailableSourceItem>> itemsByFluid,
        int maxAddableMb,
        AlloyRecipe targetRecipe,
        int maxSlots
    ) {
        // 计算每种流体在配方中的中点目标比例
        Map<Fluid, Double> targetMidRatios = new HashMap<>();
        for (Map.Entry<Fluid, AlloyRange> entry : rangeMap.entrySet()) {
            AlloyRange r = entry.getValue();
            targetMidRatios.put(entry.getKey(), (r.min() + r.max()) / 2.0);
        }

        // 尝试从最小增量到最大允许增量步进搜索
        SolvePlan bestPlan = null;

        // 优先尝试利用小步长微调，寻找满足 matches 的组合
        // 1. 如果坩埚已有流体，先尝试精准平账所需的最小原料
        if (currentTotalMb > 0) {
            SolvePlan balancingPlan = findBalancingPlan(currentFluids, currentTotalMb, rangeMap, targetMidRatios, itemsByFluid, maxAddableMb, targetRecipe);
            if (balancingPlan != null) {
                return balancingPlan;
            }
        }

        // 2. 普适方案：从当前容量允许的最大值向下或向上搜索不同规模批次
        int step = 25; // 按 25 mB 为基准步长进行目标容量采样
        int minTarget = Math.max(25, currentTotalMb + 25);
        int maxTarget = currentTotalMb + maxAddableMb;

        for (int candidateTotal = maxTarget; candidateTotal >= minTarget; candidateTotal -= step) {
            SolvePlan plan = tryFormulatePlan(candidateTotal, currentFluids, currentTotalMb, rangeMap, targetMidRatios, itemsByFluid, targetRecipe);
            if (plan != null) {
                if (bestPlan == null || plan.finalTotalMb > bestPlan.finalTotalMb) {
                    bestPlan = plan;
                    // 找到足够大的批次即可返回
                    if (bestPlan.finalTotalMb >= maxTarget - 50) {
                        break;
                    }
                }
            }
        }

        return bestPlan;
    }

    /** 针对已有残余流体，尝试寻找最小填补平衡方案 */
    private static SolvePlan findBalancingPlan(
        Map<Fluid, Integer> currentFluids,
        int currentTotalMb,
        Map<Fluid, AlloyRange> rangeMap,
        Map<Fluid, Double> targetMidRatios,
        Map<Fluid, List<AvailableSourceItem>> itemsByFluid,
        int maxAddableMb,
        AlloyRecipe targetRecipe
    ) {
        // 计算使现有主要流体作为基准时，其他流体所需的理想补充量
        // 遍历所有可能的最终总体积
        for (int added = 25; added <= Math.min(maxAddableMb, 600); added += 25) {
            int candidateTotal = currentTotalMb + added;
            SolvePlan plan = tryFormulatePlan(candidateTotal, currentFluids, currentTotalMb, rangeMap, targetMidRatios, itemsByFluid, targetRecipe);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    /** 针对指定的期望总体积，尝试贪心匹配可用原料 */
    private static SolvePlan tryFormulatePlan(
        int targetTotalMb,
        Map<Fluid, Integer> currentFluids,
        int currentTotalMb,
        Map<Fluid, AlloyRange> rangeMap,
        Map<Fluid, Double> targetMidRatios,
        Map<Fluid, List<AvailableSourceItem>> itemsByFluid,
        AlloyRecipe targetRecipe
    ) {
        Map<Fluid, Integer> neededFluids = new HashMap<>();
        for (Map.Entry<Fluid, Double> entry : targetMidRatios.entrySet()) {
            Fluid f = entry.getKey();
            int idealMb = (int) Math.round(entry.getValue() * targetTotalMb);
            int currentMb = currentFluids.getOrDefault(f, 0);
            int diff = Math.max(0, idealMb - currentMb);
            neededFluids.put(f, diff);
        }

        List<PlannedFeedItem> feedList = new ArrayList<>();
        Map<Fluid, Integer> actualAdded = new HashMap<>();

        for (Map.Entry<Fluid, Integer> entry : neededFluids.entrySet()) {
            Fluid fluid = entry.getKey();
            int neededMb = entry.getValue();
            if (neededMb <= 0) {
                actualAdded.put(fluid, 0);
                continue;
            }

            List<AvailableSourceItem> sources = itemsByFluid.get(fluid);
            if (sources == null || sources.isEmpty()) {
                return null; // 无法提供所需流体原料
            }

            int fluidAccumulated = 0;
            for (AvailableSourceItem source : sources) {
                if (fluidAccumulated >= neededMb) {
                    break;
                }
                int rem = neededMb - fluidAccumulated;
                int countNeeded = Math.min(source.stack.getCount(), (int) Math.ceil((double) rem / source.amountPerItem));
                if (countNeeded > 0) {
                    int addedMb = countNeeded * source.amountPerItem;
                    feedList.add(new PlannedFeedItem(source.slotIndex, source.isInternalSlot, source.stack, countNeeded, fluid, addedMb));
                    fluidAccumulated += addedMb;
                }
            }

            if (fluidAccumulated == 0 && neededMb > 0) {
                return null;
            }
            actualAdded.put(fluid, fluidAccumulated);
        }

        // 验证最终配比是否真正符合 AlloyRecipe
        int finalTotal = currentTotalMb;
        for (int val : actualAdded.values()) {
            finalTotal += val;
        }

        if (finalTotal <= 0 || finalTotal > 3000) {
            return null;
        }

        // 检验每种流体的实际比例是否落在 [min, max]
        for (Map.Entry<Fluid, AlloyRange> entry : rangeMap.entrySet()) {
            Fluid f = entry.getKey();
            AlloyRange range = entry.getValue();
            int currentF = currentFluids.getOrDefault(f, 0);
            int addedF = actualAdded.getOrDefault(f, 0);
            double ratio = (double) (currentF + addedF) / finalTotal;
            if (ratio < range.min() - 1e-4 || ratio > range.max() + 1e-4) {
                return null;
            }
        }

        // 使用 FluidAlloy 进行最终严谨校验
        FluidAlloy simAlloy = FluidAlloy.empty();
        for (Map.Entry<Fluid, AlloyRange> entry : rangeMap.entrySet()) {
            Fluid f = entry.getKey();
            int totalF = currentFluids.getOrDefault(f, 0) + actualAdded.getOrDefault(f, 0);
            if (totalF > 0) {
                simAlloy.fill(new FluidStack(f, totalF), IFluidHandler.FluidAction.EXECUTE, null);
            }
        }

        if (simAlloy.matches(targetRecipe)) {
            return new SolvePlan(feedList, actualAdded, finalTotal);
        }

        return null;
    }

    /** 检查物品熔化后得到的流体与数量 */
    public static HeatingRecipe getHeatingOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return HeatingRecipe.getRecipe(stack);
    }
}
