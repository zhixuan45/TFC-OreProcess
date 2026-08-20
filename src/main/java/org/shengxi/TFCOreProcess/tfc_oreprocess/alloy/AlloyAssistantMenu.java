package org.shengxi.TFCOreProcess.tfc_oreprocess.alloy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModBlocks;
import org.shengxi.TFCOreProcess.tfc_oreprocess.registry.ModMenus;

/**
 * 合金助手容器菜单。
 * 处理内部 9 格槽位与玩家背包的物品同步，并同步求解器与坩埚状态数据。
 */
public class AlloyAssistantMenu extends AbstractContainerMenu {
    public static final int INTERNAL_SLOTS = 9;
    public static final int PLAYER_INVENTORY_START = INTERNAL_SLOTS;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    public static final int PLAYER_HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BlockPos pos;
    private final AlloyAssistantBlockEntity blockEntity;

    /** 客户端构造函数（由网络数据包打开） */
    public AlloyAssistantMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
            containerId,
            playerInventory,
            extraData.readBlockPos(),
            new SimpleContainerData(7)
        );
    }

    private AlloyAssistantMenu(int containerId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(ModMenus.ALLOY_ASSISTANT.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        this.blockEntity = be instanceof AlloyAssistantBlockEntity assistantBe ? assistantBe : null;

        addDataSlots(data);
        layoutSlots(playerInventory);
    }

    /** 服务端构造函数 */
    public AlloyAssistantMenu(int containerId, Inventory playerInventory, AlloyAssistantBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.ALLOY_ASSISTANT.get(), containerId);
        this.pos = blockEntity.getBlockPos();
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), pos);
        this.data = data;
        this.blockEntity = blockEntity;

        addDataSlots(data);
        layoutSlots(playerInventory);
    }

    private void layoutSlots(Inventory playerInventory) {
        // 内部 9 格原料槽（位于 GUI 右侧 3x3 矩阵，坐标 x=116, y=20）
        if (blockEntity != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotIndex = col + row * 3;
                    this.addSlot(new SlotItemHandler(blockEntity.getInventory(), slotIndex, 116 + col * 18, 20 + row * 18));
                }
            }
        } else {
            for (int i = 0; i < INTERNAL_SLOTS; i++) {
                this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(INTERNAL_SLOTS), i, 116 + (i % 3) * 18, 20 + (i / 3) * 18));
            }
        }

        // 玩家背包（3 行 x 9 列）
        int invX = 8;
        int invY = 94;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        // 玩家快捷栏（1 行 x 9 列）
        int hotbarY = 152;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invX + col * 18, hotbarY));
        }
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public AlloyAssistantBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public AlloyAssistantSolver.SolveStatus getSolverStatus() {
        int ord = data.get(0);
        AlloyAssistantSolver.SolveStatus[] values = AlloyAssistantSolver.SolveStatus.values();
        if (ord >= 0 && ord < values.length) {
            return values[ord];
        }
        return AlloyAssistantSolver.SolveStatus.NO_MATERIALS;
    }

    public int getCurrentCrucibleAmount() {
        return data.get(1);
    }

    public int getCurrentCrucibleTemp() {
        return data.get(2);
    }

    public int getTargetBatchAmount() {
        return data.get(3);
    }

    public AlloyTargetConfig.RedstoneMode getRedstoneMode() {
        int ord = data.get(4);
        AlloyTargetConfig.RedstoneMode[] values = AlloyTargetConfig.RedstoneMode.values();
        if (ord >= 0 && ord < values.length) {
            return values[ord];
        }
        return AlloyTargetConfig.RedstoneMode.IGNORE;
    }

    public boolean isAutoFeedEnabled() {
        return data.get(5) != 0;
    }

    public int getTargetRecipeIndex() {
        return data.get(6);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < INTERNAL_SLOTS) {
                // 从内部槽移入玩家背包
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移入内部槽
                if (!this.moveItemStackTo(stackInSlot, 0, INTERNAL_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ALLOY_ASSISTANT.get());
    }
}
