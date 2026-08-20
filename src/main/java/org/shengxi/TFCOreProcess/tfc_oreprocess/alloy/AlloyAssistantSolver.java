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
        public final net.minecraft.core.BlockPos containerPos;

        public AvailableSourceItem(int slotIndex, ItemStack stack, Fluid fluid, int amountPerItem, boolean isInternalSlot, net.minecraft.core.BlockPos containerPos) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.fluid = fluid;
            this.amountPerItem = amountPerItem;
            this.isInternalSlot = isInternalSlot;
            this.containerPos = containerPos;
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
        public final net.minecraft.core.BlockPos containerPos;

        public PlannedFeedItem(int slotIndex, boolean isInternalSlot, ItemStack itemSample, int count, Fluid fluid, int totalMb, net.minecraft.core.BlockPos containerPos) {
            this.slotIndex = slotIndex;
            this.isInternalSlot = isInternalSlot;
            this.itemSample = itemSample;
            this.count = count;
            this.fluid = fluid;
            this.totalMb = totalMb;
            this.containerPos = containerPos;
        }
    }

    /** 求解结果状态 */
    public enum SolveStatus {
        SUCCESS,                // 成功求得合法配料方案
        ALREADY_MATCHED,        // 坩埚内现有熔液已完全符合目标配方且无需补充
        IMPURITY_DETECTED,      // 坩埚内存在配方外的杂质流体
        NO_MATERIALS,           // 未检测到可用的配方所需原料
        INSUFFICIENT_MATERIALS, // 材料不足以配平至合法区间
        CRUCIBLE_FULL,          // 坩埚熔液已达目标容量
        WAITING_FOR_MELT        // 坩埚输入槽已满，正在等待原料加热熔化释放槽位
    }

    /** 统一提取物品对应的熔化金属流体与单件金属量 */
    public static FluidStack extractFluidOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        // 1. 若具有 OreProcessData 数据组件
        if (stack.has(org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents.ORE_PROCESS.get())) {
            org.shengxi.TFCOreProcess.tfc_oreprocess.ore.OreProcessData data = stack.get(org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModDataComponents.ORE_PROCESS.get());
            if (data != null) {
                Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(data.moltenFluidId());
                if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    int amount = data.metalAmountMb();
                    if (amount > 0) {
                        return new FluidStack(fluid, amount);
                    }
                }
            }
        }
        // 2. 查询 TFC HeatingRecipe
        HeatingRecipe hr = HeatingRecipe.getRecipe(stack);
        if (hr != null) {
            if (hr instanceof org.shengxi.TFCOreProcess.tfc_oreprocess.recipe.DynamicHeatingRecipe dynamicHr) {
                return dynamicHr.assembleFluid(stack);
            }
            FluidStack fs = hr.getDisplayOutputFluid();
            if (!fs.isEmpty() && fs.getAmount() > 0) {
                return fs;
            }
        }
        return FluidStack.EMPTY;
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
     * 执行智能合金配料计算（支持直接传入坩埚当前的流体/固体综合成分映射）
     */
    public static SolveResult solve(
        Map<Fluid, Integer> currentFluids,
        AlloyRecipe targetRecipe,
        List<AvailableSourceItem> availableItems,
        int targetCapacity,
        int availableCrucibleSlots
    ) {
        Map<Fluid, AlloyRecipe> map = new HashMap<>();
        if (targetRecipe != null && targetRecipe.result() != null) {
            map.put(targetRecipe.result(), targetRecipe);
        }
        return solve(currentFluids, targetRecipe, availableItems, targetCapacity, availableCrucibleSlots, map);
    }

    /**
     * 执行智能合金配料计算（支持所有已知中间件合金配方构成的有向图递归展开）
     */
    public static SolveResult solve(
        Map<Fluid, Integer> currentFluids,
        AlloyRecipe targetRecipe,
        List<AvailableSourceItem> availableItems,
        int targetCapacity,
        int availableCrucibleSlots,
        Map<Fluid, AlloyRecipe> allRecipesByResult
    ) {
        if (targetRecipe == null) {
            return SolveResult.failure(SolveStatus.NO_MATERIALS, "未指定目标合金配方");
        }

        List<AlloyRange> ranges = targetRecipe.contents();
        if (ranges.isEmpty()) {
            return SolveResult.failure(SolveStatus.NO_MATERIALS, "配方内容为空");
        }

        // 收集配方中允许的所有成分流体与区间
        Set<Fluid> allowedFluids = new HashSet<>();
        Map<Fluid, AlloyRange> rangeMap = new LinkedHashMap<>();
        for (AlloyRange range : ranges) {
            allowedFluids.add(range.fluid());
            rangeMap.put(range.fluid(), range);
        }

        // 确保目标配方自身也在配方查找表中
        Map<Fluid, AlloyRecipe> recipeMap = new HashMap<>(allRecipesByResult);
        if (targetRecipe.result() != null) {
            recipeMap.put(targetRecipe.result(), targetRecipe);
        }

        // 标准化坩埚现有成分：支持单质、目标合金产物流体及多级中间件合金流体的递归分解
        Map<Fluid, Integer> normalizedFluids = new HashMap<>();
        int currentTotalMb = 0;

        for (Map.Entry<Fluid, Integer> entry : currentFluids.entrySet()) {
            Fluid fluid = entry.getKey();
            int amount = entry.getValue();
            if (amount <= 0) continue;

            boolean decomposed = recursivelyDecomposeFluid(
                fluid,
                amount,
                allowedFluids,
                recipeMap,
                new HashSet<>(),
                normalizedFluids
            );

            if (!decomposed) {
                return SolveResult.failure(SolveStatus.IMPURITY_DETECTED, "坩埚中存在未知杂质金属或无法兼容的中间件流体，无法熔炼目标合金");
            }
            currentTotalMb += amount;
        }

        // 检查现有成分是否已完全符合目标配方比例
        boolean alreadyMatched = false;
        if (currentTotalMb > 0) {
            boolean allMatch = true;
            for (Map.Entry<Fluid, AlloyRange> entry : rangeMap.entrySet()) {
                int fMb = normalizedFluids.getOrDefault(entry.getKey(), 0);
                double ratio = (double) fMb / currentTotalMb;
                if (ratio < entry.getValue().min() - 1e-4 || ratio > entry.getValue().max() + 1e-4) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                alreadyMatched = true;
            }
        }

        int maxMb = Math.min(3000, targetCapacity);
        if (currentTotalMb >= maxMb) {
            if (alreadyMatched) {
                return new SolveResult(SolveStatus.ALREADY_MATCHED, null, currentFluids, currentTotalMb, "坩埚已达目标容量且比例符合配方");
            }
            return SolveResult.failure(SolveStatus.CRUCIBLE_FULL, "坩埚已满且比例不符合要求");
        }

        if (availableCrucibleSlots <= 0) {
            if (alreadyMatched) {
                return new SolveResult(SolveStatus.ALREADY_MATCHED, null, currentFluids, currentTotalMb, "坩埚输入槽已满，正在熔炼等待中");
            }
            return SolveResult.failure(SolveStatus.WAITING_FOR_MELT, "坩埚输入槽已满，等待原料加热熔化释放空槽");
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

        boolean hasAnyMaterial = itemsByFluid.values().stream().anyMatch(list -> !list.isEmpty());
        if (!hasAnyMaterial) {
            if (alreadyMatched) {
                return new SolveResult(SolveStatus.ALREADY_MATCHED, null, currentFluids, currentTotalMb, "坩埚现有熔液已符合配方比例");
            }
            return SolveResult.failure(SolveStatus.NO_MATERIALS, "缺少熔炼所需的金属原料");
        }

        // 核心组合配比搜索：在 availableCrucibleSlots 约束下寻找最优投料方案
        SolvePlan bestPlan = searchSlotConstrainedPlan(
            normalizedFluids,
            currentTotalMb,
            rangeMap,
            itemsByFluid,
            maxMb,
            availableCrucibleSlots
        );

        if (bestPlan == null || bestPlan.feedItems.isEmpty()) {
            if (alreadyMatched) {
                return new SolveResult(SolveStatus.ALREADY_MATCHED, null, normalizedFluids, currentTotalMb, "坩埚现有熔液已符合配方比例");
            }
            return SolveResult.failure(SolveStatus.INSUFFICIENT_MATERIALS, "现有原料不足以在当前空槽位内配平至目标合金比例");
        }

        Map<Fluid, Integer> finalFluids = new HashMap<>(normalizedFluids);
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

    /** 兼容旧接口的 solve 重载 */
    public static SolveResult solve(
        FluidAlloy currentAlloy,
        AlloyRecipe targetRecipe,
        List<AvailableSourceItem> availableItems,
        int targetCapacity,
        int availableCrucibleSlots
    ) {
        Map<Fluid, Integer> fluids = new HashMap<>();
        if (currentAlloy != null && currentAlloy.getAmount() > 0) {
            int totalMb = currentAlloy.getAmount();
            Object2DoubleMap<Fluid> content = currentAlloy.getContent();
            double sumValues = 0.0;
            for (double val : content.values()) {
                sumValues += val;
            }

            for (Object2DoubleMap.Entry<Fluid> entry : content.object2DoubleEntrySet()) {
                double rawVal = entry.getDoubleValue();
                int mb;
                if (Math.abs(sumValues - totalMb) < 1.0) {
                    mb = (int) Math.round(rawVal);
                } else if (Math.abs(sumValues - 100.0) < 1.0) {
                    mb = (int) Math.round(rawVal / 100.0 * totalMb);
                } else {
                    mb = (int) Math.round(rawVal * totalMb);
                }

                if (mb > 0) {
                    fluids.put(entry.getKey(), fluids.getOrDefault(entry.getKey(), 0) + mb);
                }
            }
        }
        return solve(fluids, targetRecipe, availableItems, targetCapacity, availableCrucibleSlots);
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
     * 在可用空槽位约束下（单次投料物品总数 <= maxSlots，最大 9），搜索最优投料方案
     */
    private static SolvePlan searchSlotConstrainedPlan(
        Map<Fluid, Integer> currentFluids,
        int currentTotalMb,
        Map<Fluid, AlloyRange> rangeMap,
        Map<Fluid, List<AvailableSourceItem>> itemsByFluid,
        int maxCapacityMb,
        int maxSlots
    ) {
        List<Fluid> fluidList = new ArrayList<>(rangeMap.keySet());
        int numFluids = fluidList.size();
        int maxItemsToFeed = Math.min(9, maxSlots);

        SolvePlan bestPlan = null;

        // 从最大可用槽位数向下遍历尝试
        for (int totalItems = maxItemsToFeed; totalItems >= 1; totalItems--) {
            List<int[]> distributions = new ArrayList<>();
            generateDistributions(numFluids, totalItems, new int[numFluids], 0, distributions);

            for (int[] dist : distributions) {
                SolvePlan plan = evaluateDistribution(
                    dist,
                    fluidList,
                    currentFluids,
                    currentTotalMb,
                    rangeMap,
                    itemsByFluid,
                    maxCapacityMb
                );

                if (plan != null) {
                    if (bestPlan == null || plan.finalTotalMb > bestPlan.finalTotalMb) {
                        bestPlan = plan;
                    }
                }
            }

            // 若在较大批次中已找到满足容量的方案，直接返回
            if (bestPlan != null && bestPlan.finalTotalMb >= maxCapacityMb - 50) {
                break;
            }
        }

        return bestPlan;
    }

    /**
     * 生成将 totalItems 分配给 numFluids 种成分的所有非负整数组合
     */
    private static void generateDistributions(int numFluids, int remainingItems, int[] currentDist, int index, List<int[]> result) {
        if (index == numFluids - 1) {
            currentDist[index] = remainingItems;
            result.add(currentDist.clone());
            return;
        }

        for (int i = 0; i <= remainingItems; i++) {
            currentDist[index] = i;
            generateDistributions(numFluids, remainingItems - i, currentDist, index + 1, result);
        }
    }

    /**
     * 评估某种物品数量分配是否能在可用库存中满足，且最终配比是否落在目标区间内
     */
    private static SolvePlan evaluateDistribution(
        int[] dist,
        List<Fluid> fluidList,
        Map<Fluid, Integer> currentFluids,
        int currentTotalMb,
        Map<Fluid, AlloyRange> rangeMap,
        Map<Fluid, List<AvailableSourceItem>> itemsByFluid,
        int maxCapacityMb
    ) {
        List<PlannedFeedItem> feedItems = new ArrayList<>();
        Map<Fluid, Integer> addedFluids = new HashMap<>();

        int totalAddedMb = 0;

        for (int i = 0; i < fluidList.size(); i++) {
            Fluid fluid = fluidList.get(i);
            int countNeeded = dist[i];

            if (countNeeded == 0) {
                addedFluids.put(fluid, 0);
                continue;
            }

            List<AvailableSourceItem> sources = itemsByFluid.get(fluid);
            if (sources == null || sources.isEmpty()) {
                return null;
            }

            int itemsAllocated = 0;
            int fluidAdded = 0;

            for (AvailableSourceItem source : sources) {
                if (itemsAllocated >= countNeeded) {
                    break;
                }
                int remItems = countNeeded - itemsAllocated;
                int take = Math.min(remItems, source.stack.getCount());
                if (take > 0) {
                    int mb = take * source.amountPerItem;
                    feedItems.add(new PlannedFeedItem(source.slotIndex, source.isInternalSlot, source.stack, take, fluid, mb, source.containerPos));
                    itemsAllocated += take;
                    fluidAdded += mb;
                }
            }

            if (itemsAllocated < countNeeded) {
                return null; // 库存物品数量不足以满足该分配
            }

            addedFluids.put(fluid, fluidAdded);
            totalAddedMb += fluidAdded;
        }

        if (totalAddedMb == 0) {
            return null;
        }

        int finalTotal = currentTotalMb + totalAddedMb;
        if (finalTotal <= 0 || finalTotal > maxCapacityMb) {
            return null;
        }

        // 校验每种流体的实际比例是否落在 [min, max]
        for (Map.Entry<Fluid, AlloyRange> entry : rangeMap.entrySet()) {
            Fluid f = entry.getKey();
            AlloyRange range = entry.getValue();
            int currentF = currentFluids.getOrDefault(f, 0);
            int addedF = addedFluids.getOrDefault(f, 0);
            double ratio = (double) (currentF + addedF) / finalTotal;
            if (ratio < range.min() - 1e-4 || ratio > range.max() + 1e-4) {
                return null;
            }
        }

        return new SolvePlan(feedItems, addedFluids, finalTotal);
    }

    /** 检查物品熔化后得到的流体与数量 */
    public static HeatingRecipe getHeatingOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return HeatingRecipe.getRecipe(stack);
    }

    /**
     * 递归将任意流体（单质、目标合金产物、或任意已知中间件合金流体）分解为目标配方所允许的底层成分。
     * 若该流体或其分解产物中包含目标配方未涵盖的未知金属，则返回 false。
     */
    private static boolean recursivelyDecomposeFluid(
        Fluid fluid,
        int amount,
        Set<Fluid> allowedFluids,
        Map<Fluid, AlloyRecipe> recipesByResult,
        Set<Fluid> visited,
        Map<Fluid, Integer> targetFluidsOutput
    ) {
        if (amount <= 0 || fluid == null) {
            return true;
        }

        // 1. 若该流体本身属于目标配方的直接合法成分
        if (allowedFluids.contains(fluid)) {
            targetFluidsOutput.put(fluid, targetFluidsOutput.getOrDefault(fluid, 0) + amount);
            return true;
        }

        // 2. 环路保护：防止配方循环依赖导致的无限递归
        if (visited.contains(fluid)) {
            return false;
        }
        visited.add(fluid);

        // 3. 查找以该流体为产物的合金配方（作为中间件展开）
        AlloyRecipe recipe = recipesByResult.get(fluid);
        if (recipe == null || recipe.contents().isEmpty()) {
            return false; // 既不是目标成分，也不是已知中间件合金 -> 杂质
        }

        // 4. 按该中间件合金各成分的名义中点比例进行数学分解
        List<AlloyRange> contents = recipe.contents();
        int allocated = 0;
        Fluid maxCompFluid = null;
        double maxCompRatio = -1.0;

        Map<Fluid, Integer> subComponents = new HashMap<>();

        for (AlloyRange range : contents) {
            double midRatio = (range.min() + range.max()) / 2.0;
            int compMb = (int) Math.round(amount * midRatio);
            subComponents.put(range.fluid(), subComponents.getOrDefault(range.fluid(), 0) + compMb);
            allocated += compMb;
            if (midRatio > maxCompRatio) {
                maxCompRatio = midRatio;
                maxCompFluid = range.fluid();
            }
        }

        // 弥补浮点数舍入误差到占比最大的主成分
        if (maxCompFluid != null && allocated != amount) {
            int diff = amount - allocated;
            subComponents.put(maxCompFluid, subComponents.getOrDefault(maxCompFluid, 0) + diff);
        }

        // 5. 对分解出的各子组分流体继续递归展开
        for (Map.Entry<Fluid, Integer> subEntry : subComponents.entrySet()) {
            boolean success = recursivelyDecomposeFluid(
                subEntry.getKey(),
                subEntry.getValue(),
                allowedFluids,
                recipesByResult,
                new HashSet<>(visited),
                targetFluidsOutput
            );
            if (!success) {
                return false;
            }
        }

        return true;
    }
}
