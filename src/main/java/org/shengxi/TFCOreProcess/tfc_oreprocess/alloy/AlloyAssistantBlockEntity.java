package org.shengxi.TFCOreProcess.tfc_oreprocess.alloy;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
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
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

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

        // 检测红石触发条件
        boolean shouldRun = switch (entity.config.getRedstoneMode()) {
            case IGNORE -> entity.config.isAutoFeedEnabled();
            case REQUIRE_SIGNAL -> powered && entity.config.isAutoFeedEnabled();
            case PULSE_ONCE -> pulseTriggered;
        };

        // 检查下方是否为坩埚
        BlockEntity belowBe = level.getBlockEntity(pos.below());
        if (!(belowBe instanceof CrucibleBlockEntity crucible)) {
            entity.solverStatusOrdinal = AlloyAssistantSolver.SolveStatus.NO_MATERIALS.ordinal();
            entity.currentCrucibleAmount = 0;
            entity.currentCrucibleTemp = 0;
            entity.updateActiveState(state, false);
            return;
        }

        FluidAlloy crucibleAlloy = crucible.getAlloy();
        entity.currentCrucibleAmount = crucibleAlloy != null ? crucibleAlloy.getAmount() : 0;
        entity.currentCrucibleTemp = (int) crucible.getTemperature();

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

        // 收集可用原料
        List<AlloyAssistantSolver.AvailableSourceItem> availableItems = entity.collectAvailableItems(level, pos);

        // 计算 Crucible 剩余输入空位
        int availableSlots = entity.countAvailableCrucibleSlots(crucible);

        // 求解投料
        AlloyAssistantSolver.SolveResult result = AlloyAssistantSolver.solve(
            crucibleAlloy,
            targetRecipe,
            availableItems,
            entity.config.getTargetBatchAmount(),
            availableSlots
        );

        entity.solverStatusOrdinal = result.status.ordinal();
        entity.lastStatusMessage = result.statusMessage;

        if (result.status == AlloyAssistantSolver.SolveStatus.SUCCESS && !result.plannedItems.isEmpty()) {
            // 执行投料转移
            boolean fedAny = entity.executeFeed(level, pos, crucible, result.plannedItems);
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
    private AlloyRecipe findTargetRecipe(Level level, ResourceLocation recipeId) {
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(recipeId);
        if (holder.isPresent() && holder.get().value() instanceof AlloyRecipe alloyRecipe) {
            return alloyRecipe;
        }
        return null;
    }

    /** 扫描内部槽位及上方连接的外部容器中的可用原料 */
    private List<AlloyAssistantSolver.AvailableSourceItem> collectAvailableItems(Level level, BlockPos pos) {
        List<AlloyAssistantSolver.AvailableSourceItem> list = new ArrayList<>();

        // 1. 扫描内部 9 格
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                HeatingRecipe hr = AlloyAssistantSolver.getHeatingOutput(stack);
                if (hr != null) {
                    FluidStack fs = hr.assembleFluid(stack);
                    if (!fs.isEmpty() && fs.getAmount() > 0) {
                        list.add(new AlloyAssistantSolver.AvailableSourceItem(i, stack, fs.getFluid(), fs.getAmount(), true));
                    }
                }
            }
        }

        // 2. 扫描上方外部容器
        IItemHandler topHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        if (topHandler != null) {
            for (int i = 0; i < topHandler.getSlots(); i++) {
                ItemStack stack = topHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    HeatingRecipe hr = AlloyAssistantSolver.getHeatingOutput(stack);
                    if (hr != null) {
                        FluidStack fs = hr.assembleFluid(stack);
                        if (!fs.isEmpty() && fs.getAmount() > 0) {
                            list.add(new AlloyAssistantSolver.AvailableSourceItem(i, stack, fs.getFluid(), fs.getAmount(), false));
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
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                free++;
            }
        }
        return free;
    }

    /** 向 Crucible 输入槽转移物品 */
    private boolean executeFeed(Level level, BlockPos pos, CrucibleBlockEntity crucible, List<AlloyAssistantSolver.PlannedFeedItem> plannedItems) {
        IItemHandlerModifiable crucibleInv = crucible.getInventory();
        IItemHandler topHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);

        boolean anyMoved = false;

        for (AlloyAssistantSolver.PlannedFeedItem item : plannedItems) {
            int remainingToMove = item.count;
            if (item.isInternalSlot) {
                ItemStack inSlot = inventory.getStackInSlot(item.slotIndex);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, item.itemSample)) {
                    int moveCount = Math.min(remainingToMove, inSlot.getCount());
                    ItemStack movingStack = inventory.extractItem(item.slotIndex, moveCount, false);
                    ItemStack leftOver = insertIntoCrucible(crucibleInv, movingStack);
                    if (!leftOver.isEmpty()) {
                        // 若未全部塞入则归还内部槽
                        inventory.insertItem(item.slotIndex, leftOver, false);
                    }
                    if (movingStack.getCount() > leftOver.getCount()) {
                        anyMoved = true;
                    }
                }
            } else if (topHandler != null) {
                ItemStack inSlot = topHandler.getStackInSlot(item.slotIndex);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, item.itemSample)) {
                    int moveCount = Math.min(remainingToMove, inSlot.getCount());
                    ItemStack movingStack = topHandler.extractItem(item.slotIndex, moveCount, false);
                    ItemStack leftOver = insertIntoCrucible(crucibleInv, movingStack);
                    if (!leftOver.isEmpty()) {
                        topHandler.insertItem(item.slotIndex, leftOver, false);
                    }
                    if (movingStack.getCount() > leftOver.getCount()) {
                        anyMoved = true;
                    }
                }
            }
        }

        return anyMoved;
    }

    private ItemStack insertIntoCrucible(IItemHandlerModifiable crucibleInv, ItemStack stack) {
        for (int i = CrucibleBlockEntity.SLOT_INPUT_START; i <= CrucibleBlockEntity.SLOT_INPUT_END; i++) {
            stack = crucibleInv.insertItem(i, stack, false);
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
