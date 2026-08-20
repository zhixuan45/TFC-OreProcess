package org.shengxi.TFCOreProcess.tfc_oreprocess.alloy;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.recipes.AlloyRecipe;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.util.FluidAlloy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 合金助手方块实体。
 * 负责维护内部 9 格缓存槽、执行智能配料算法并向下方坩埚推送原料。
 */
public class AlloyAssistantBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INVENTORY_SIZE = 9;
    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final AlloyTargetConfig config = new AlloyTargetConfig();

    // 状态数据用于 GUI 同步（ContainerData）
    private int solverStatusOrdinal = AlloyAssistantSolver.SolveStatus.NO_MATERIALS.ordinal();
    private int currentCrucibleAmount = 0;
    private int currentCrucibleTemp = 0;
    private boolean lastPoweredState = false;
    private int tickCooldown = 0;
    private String lastStatusMessage = "";

    protected final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> solverStatusOrdinal;
                case 1 -> currentCrucibleAmount;
                case 2 -> currentCrucibleTemp;
                case 3 -> config.getTargetBatchAmount();
                case 4 -> config.getRedstoneMode().ordinal();
                case 5 -> config.isAutoFeedEnabled() ? 1 : 0;
                case 6 -> getTargetRecipeIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> solverStatusOrdinal = value;
                case 1 -> currentCrucibleAmount = value;
                case 2 -> currentCrucibleTemp = value;
                case 3 -> config.setTargetBatchAmount(value);
                case 4 -> {
                    if (value >= 0 && value < AlloyTargetConfig.RedstoneMode.values().length) {
                        config.setRedstoneMode(AlloyTargetConfig.RedstoneMode.values()[value]);
                    }
                }
                case 5 -> config.setAutoFeedEnabled(value != 0);
                case 6 -> setTargetRecipeByIndex(value);
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public int getTargetRecipeIndex() {
        if (level == null || config.getTargetRecipeId() == null) {
            return 0;
        }
        List<RecipeHolder<AlloyRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.ALLOY.get());
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(config.getTargetRecipeId())) {
                return i;
            }
        }
        return 0;
    }

    public void setTargetRecipeByIndex(int index) {
        if (level == null) return;
        List<RecipeHolder<AlloyRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.ALLOY.get());
        if (index >= 0 && index < recipes.size()) {
            config.setTargetRecipeId(recipes.get(index).id());
            setChanged();
        }
    }

    public AlloyAssistantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_ASSISTANT.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public AlloyTargetConfig getConfig() {
        return config;
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public String getLastStatusMessage() {
        return lastStatusMessage;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlloyAssistantBlockEntity entity) {
        entity.tickCooldown--;
        if (entity.tickCooldown > 0) {
            return;
        }
        entity.tickCooldown = 15; // 每 15 ticks（0.75秒）检测一次

        boolean powered = state.getValue(AlloyAssistantBlock.POWERED);
        boolean pulseTriggered = powered && !entity.lastPoweredState;
        entity.lastPoweredState = powered;

        // 检查红石触发条件
        boolean shouldRun = switch (entity.config.getRedstoneMode()) {
            case IGNORE -> entity.config.isAutoFeedEnabled();
            case REQUIRE_SIGNAL -> powered && entity.config.isAutoFeedEnabled();
            case PULSE_ONCE -> pulseTriggered;
        };

        // 自动发现相邻坩埚（优先正下方，其次相邻水平方向）
        CrucibleBlockEntity crucible = entity.findCrucible(level, pos);
        if (crucible == null) {
            entity.solverStatusOrdinal = AlloyAssistantSolver.SolveStatus.NO_MATERIALS.ordinal();
            entity.currentCrucibleAmount = 0;
            entity.currentCrucibleTemp = 0;
            entity.updateActiveState(state, false);
            return;
        }

        FluidAlloy crucibleAlloy = crucible.getAlloy();
        int liquidAmount = crucibleAlloy != null ? crucibleAlloy.getAmount() : 0;
        entity.currentCrucibleTemp = (int) crucible.getTemperature();

        // 收集坩埚内所有金属成分（包括已熔化的液体和槽内待熔化的固体金属当量）
        Map<Fluid, Integer> crucibleFluids = entity.collectCrucibleFluids(crucible);
        int totalEffectiveMb = 0;
        for (int val : crucibleFluids.values()) {
            totalEffectiveMb += val;
        }
        entity.currentCrucibleAmount = totalEffectiveMb;

        // 若尚未指定配方，自动从可用配方库中选取首个配方作为默认配方
        if (entity.config.getTargetRecipeId() == null) {
            List<RecipeHolder<AlloyRecipe>> allRecipes = level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.ALLOY.get());
            if (!allRecipes.isEmpty()) {
                entity.config.setTargetRecipeId(allRecipes.get(0).id());
                entity.setChanged();
            }
        }

        if (!shouldRun || entity.config.getTargetRecipeId() == null) {
            entity.updateActiveState(state, false);
            return;
        }

        // 获取目标配方
        AlloyRecipe targetRecipe = entity.findTargetRecipe(level, entity.config.getTargetRecipeId());
        if (targetRecipe == null) {
            entity.solverStatusOrdinal = AlloyAssistantSolver.SolveStatus.NO_MATERIALS.ordinal();
            entity.lastStatusMessage = "未找到选定的合金配方";
            entity.updateActiveState(state, false);
            return;
        }

        // 收集可用原料（内部 9 格 + 所有相邻外部容器）
        List<AlloyAssistantSolver.AvailableSourceItem> availableItems = entity.collectAvailableItems(level, pos, crucible.getBlockPos());

        // 构建全部已知合金配方索引（按产物流体映射），供中间件合金流体递归展开
        Map<Fluid, AlloyRecipe> allRecipesByResult = new HashMap<>();
        for (RecipeHolder<AlloyRecipe> holder : level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.ALLOY.get())) {
            if (holder.value().result() != null) {
                allRecipesByResult.put(holder.value().result(), holder.value());
            }
        }

        // 计算 Crucible 剩余输入空位（每个槽位上限为 1 个物品）
        int availableSlots = entity.countAvailableCrucibleSlots(crucible);

        // 求解投料
        AlloyAssistantSolver.SolveResult result = AlloyAssistantSolver.solve(
            crucibleFluids,
            targetRecipe,
            availableItems,
            entity.config.getTargetBatchAmount(),
            availableSlots,
            allRecipesByResult
        );

        entity.solverStatusOrdinal = result.status.ordinal();
        entity.lastStatusMessage = result.statusMessage;

        if (result.status == AlloyAssistantSolver.SolveStatus.SUCCESS && !result.plannedItems.isEmpty()) {
            // 执行投料转移
            boolean fedAny = entity.executeFeed(level, crucible, result.plannedItems);
            entity.updateActiveState(state, fedAny);
            entity.setChanged();
        } else {
            entity.updateActiveState(state, false);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AlloyAssistantBlockEntity entity) {
    }

    private void updateActiveState(BlockState state, boolean active) {
        if (state.getValue(AlloyAssistantBlock.ACTIVE) != active && level != null) {
            level.setBlock(worldPosition, state.setValue(AlloyAssistantBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private CrucibleBlockEntity findCrucible(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof CrucibleBlockEntity crucible) {
            return crucible;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof CrucibleBlockEntity crucible) {
                return crucible;
            }
        }
        return null;
    }

    @Nullable
    private AlloyRecipe findTargetRecipe(Level level, ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }
        for (RecipeHolder<AlloyRecipe> holder : level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.ALLOY.get())) {
            if (holder.id().equals(recipeId)) {
                return holder.value();
            }
        }
        return null;
    }

    /** 收集坩埚内所有金属成分（包括已熔化的液体和槽内待熔化的固体金属当量） */
    private Map<Fluid, Integer> collectCrucibleFluids(CrucibleBlockEntity crucible) {
        Map<Fluid, Integer> fluids = new HashMap<>();

        // 1. 已熔化的液体金属
        FluidAlloy crucibleAlloy = crucible.getAlloy();
        if (crucibleAlloy != null && crucibleAlloy.getAmount() > 0) {
            int totalMb = crucibleAlloy.getAmount();
            Object2DoubleMap<Fluid> content = crucibleAlloy.getContent();

            // 自适应量纲检测：判断 content 中的数值是实际毫桶数 mB、百分比（0~100）还是标准比例（0~1）
            double sumValues = 0.0;
            for (double val : content.values()) {
                sumValues += val;
            }

            for (Object2DoubleMap.Entry<Fluid> entry : content.object2DoubleEntrySet()) {
                double rawVal = entry.getDoubleValue();
                int mb;
                if (Math.abs(sumValues - totalMb) < 1.0) {
                    // 数值本身即为各金属实际毫桶数 mB（各组分之和等于 totalMb）
                    mb = (int) Math.round(rawVal);
                } else if (Math.abs(sumValues - 100.0) < 1.0) {
                    // 数值是百分比 0~100（各组分之和为 100）
                    mb = (int) Math.round(rawVal / 100.0 * totalMb);
                } else {
                    // 数值是标准比例 0~1（各组分之和为 1.0）
                    mb = (int) Math.round(rawVal * totalMb);
                }

                if (mb > 0) {
                    fluids.put(entry.getKey(), fluids.getOrDefault(entry.getKey(), 0) + mb);
                }
            }
        }

        // 2. 坩埚输入槽中待熔化的固体物品
        IItemHandlerModifiable crucibleInv = crucible.getInventory();
        for (int i = CrucibleBlockEntity.SLOT_INPUT_START; i <= CrucibleBlockEntity.SLOT_INPUT_END; i++) {
            ItemStack stack = crucibleInv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                FluidStack fs = AlloyAssistantSolver.extractFluidOutput(stack);
                if (!fs.isEmpty() && fs.getAmount() > 0) {
                    int amount = fs.getAmount() * stack.getCount();
                    fluids.put(fs.getFluid(), fluids.getOrDefault(fs.getFluid(), 0) + amount);
                }
            }
        }

        return fluids;
    }

    /** 扫描内部槽位及所有相邻外部容器中的可用原料 */
    private List<AlloyAssistantSolver.AvailableSourceItem> collectAvailableItems(Level level, BlockPos pos, BlockPos cruciblePos) {
        List<AlloyAssistantSolver.AvailableSourceItem> list = new ArrayList<>();

        // 1. 扫描内部 9 格
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                FluidStack fs = AlloyAssistantSolver.extractFluidOutput(stack);
                if (!fs.isEmpty() && fs.getAmount() > 0) {
                    list.add(new AlloyAssistantSolver.AvailableSourceItem(i, stack, fs.getFluid(), fs.getAmount(), true, pos));
                }
            }
        }

        // 2. 扫描所有相邻外部容器（如上方、四周）
        for (Direction dir : Direction.values()) {
            BlockPos targetPos = pos.relative(dir);
            if (targetPos.equals(cruciblePos)) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, dir.getOpposite());
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        FluidStack fs = AlloyAssistantSolver.extractFluidOutput(stack);
                        if (!fs.isEmpty() && fs.getAmount() > 0) {
                            list.add(new AlloyAssistantSolver.AvailableSourceItem(i, stack, fs.getFluid(), fs.getAmount(), false, targetPos));
                        }
                    }
                }
            }
        }

        return list;
    }

    private int countAvailableCrucibleSlots(CrucibleBlockEntity crucible) {
        IItemHandlerModifiable crucibleInv = crucible.getInventory();
        int free = 0;
        for (int i = CrucibleBlockEntity.SLOT_INPUT_START; i <= CrucibleBlockEntity.SLOT_INPUT_END; i++) {
            ItemStack stack = crucibleInv.getStackInSlot(i);
            if (stack.isEmpty()) {
                free++;
            }
        }
        return free;
    }

    /** 向 Crucible 输入槽转移物品 */
    private boolean executeFeed(Level level, CrucibleBlockEntity crucible, List<AlloyAssistantSolver.PlannedFeedItem> plannedItems) {
        IItemHandlerModifiable crucibleInv = crucible.getInventory();
        boolean anyMoved = false;

        for (AlloyAssistantSolver.PlannedFeedItem item : plannedItems) {
            int remainingToMove = item.count;
            if (item.isInternalSlot) {
                ItemStack inSlot = inventory.getStackInSlot(item.slotIndex);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, item.itemSample)) {
                    int moveCount = Math.min(remainingToMove, inSlot.getCount());
                    ItemStack movingStack = inventory.extractItem(item.slotIndex, moveCount, false);
                    ItemStack leftOver = insertIntoCrucible(crucible, crucibleInv, movingStack);
                    if (!leftOver.isEmpty()) {
                        inventory.insertItem(item.slotIndex, leftOver, false);
                    }
                    if (movingStack.getCount() > leftOver.getCount()) {
                        anyMoved = true;
                    }
                }
            } else if (item.containerPos != null) {
                IItemHandler sourceHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, item.containerPos, null);
                if (sourceHandler != null && item.slotIndex < sourceHandler.getSlots()) {
                    ItemStack inSlot = sourceHandler.getStackInSlot(item.slotIndex);
                    if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, item.itemSample)) {
                        int moveCount = Math.min(remainingToMove, inSlot.getCount());
                        ItemStack movingStack = sourceHandler.extractItem(item.slotIndex, moveCount, false);
                        ItemStack leftOver = insertIntoCrucible(crucible, crucibleInv, movingStack);
                        if (!leftOver.isEmpty()) {
                            sourceHandler.insertItem(item.slotIndex, leftOver, false);
                        }
                        if (movingStack.getCount() > leftOver.getCount()) {
                            anyMoved = true;
                        }
                    }
                }
            }
        }

        if (anyMoved) {
            crucible.setChanged();
            crucible.markForSync();
        }

        return anyMoved;
    }

    private ItemStack insertIntoCrucible(CrucibleBlockEntity crucible, IItemHandlerModifiable crucibleInv, ItemStack stack) {
        for (int i = CrucibleBlockEntity.SLOT_INPUT_START; i <= CrucibleBlockEntity.SLOT_INPUT_END; i++) {
            stack = crucibleInv.insertItem(i, stack, false);
            crucible.setAndUpdateSlots(i);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tfc_oreprocess.alloy_assistant");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlloyAssistantMenu(containerId, playerInventory, this, this.containerData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Config", config.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Config")) {
            config.load(tag.getCompound("Config"), registries);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Config", config.save(registries));
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
