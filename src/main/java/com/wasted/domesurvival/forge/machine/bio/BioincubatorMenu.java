package com.wasted.domesurvival.forge.machine.bio;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.bio.BioModuleClientState;
import com.wasted.domesurvival.forge.bio.BioModuleData;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BioincubatorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_END = 9;
    private static final int PLAYER_START = 9;
    private static final int PLAYER_END = 36;
    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 45;
    private static final int MODE_BUTTON = 50;
    private static final int SIDE_BUTTON_BASE = 100;

    private int tab=201;
    public void setTab(int id){tab=id;}
    public boolean isMainPanelOpen(){return tab==201;}
    public boolean isSidePanelOpen(){return tab==202;}
    public boolean isModulePanelOpen(){return tab==200;}
    public int energyStored(){return getEnergy();}public int energyCapacity(){return getEnergyCapacity();}
    public int waterStored(){return getWater();}public int waterCapacity(){return getWaterCapacity();}
    public int progress(){return getProgress();}public int progressMax(){return getProgressMax();}public int status(){return getStatus();}
    public int recipeEnergy(){return data.get(BioincubatorBlockEntity.DATA_CYCLE_ENERGY);}
    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    @Nullable
    private final BioincubatorBlockEntity incubator;

    public BioincubatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                null,
                new ItemStackHandler(BioincubatorBlockEntity.SLOT_COUNT),
                new SimpleContainerData(BioincubatorBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    public BioincubatorMenu(int containerId, Inventory playerInventory, BioincubatorBlockEntity incubator) {
        this(
                containerId,
                playerInventory,
                incubator,
                incubator.getInventory(),
                incubator.getDataAccess(),
                incubator.getBlockPos()
        );
    }

    private BioincubatorMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable BioincubatorBlockEntity incubator,
            IItemHandler machineInventory,
            ContainerData data,
            BlockPos pos
    ) {
        super(ModMenuTypes.BIOINCUBATOR.get(), containerId);

        this.level = playerInventory.player.level();
        this.blockPos = pos;
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = data;
        this.incubator = incubator;

        checkContainerDataCount(data, BioincubatorBlockEntity.DATA_COUNT);
        addDataSlots(new ContainerData(){
            public int get(int i){return (data.get(i/2)>>>((i%2)*16))&65535;}
            public void set(int i,int value){int shift=i%2*16;data.set(i/2,(data.get(i/2)&~(65535<<shift))|((value&65535)<<shift));}
            public int getCount(){return data.getCount()*2;}
        });

        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_CAPSULE, 70, 98,
                BioincubatorBlockEntity.MODE_INCUBATION) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isActive() && isValidCapsuleForMode(stack, BioincubatorBlockEntity.MODE_INCUBATION);
            }
        });

        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_FEED, 98, 98,
                BioincubatorBlockEntity.MODE_INCUBATION) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isActive() && isKnownFeed(stack);
            }
        });

        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_CAPSULE, 70, 98,
                BioincubatorBlockEntity.MODE_REPAIR) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isActive() && isValidCapsuleForMode(stack, BioincubatorBlockEntity.MODE_REPAIR);
            }
        });
        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_FEED, 98, 98,
                BioincubatorBlockEntity.MODE_REPAIR) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isActive() && stack.is(ModItems.BIO_REPAIR_KIT.get());
            }
        });
        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_BIOGEL, 126, 98,
                BioincubatorBlockEntity.MODE_REPAIR) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isActive() && stack.is(ModItems.BIOGEL.get());
            }
        });
        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_NUTRIENT, 154, 98,
                BioincubatorBlockEntity.MODE_REPAIR) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isActive() && stack.is(ModItems.NUTRIENT_MIX.get());
            }
        });
        addSlot(new ModeSlot(machineInventory, BioincubatorBlockEntity.SLOT_OUTPUT, 186, 98,
                BioincubatorBlockEntity.MODE_REPAIR) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        IItemHandler moduleInventory=incubator==null?new ItemStackHandler(2):incubator.getModules();
        for(int moduleSlot=0;moduleSlot<2;moduleSlot++)addSlot(new SlotItemHandler(moduleInventory,moduleSlot,22,67+30*moduleSlot){
            public boolean isActive(){return isModulePanelOpen();}
            public boolean mayPlace(ItemStack stack){return isActive()&&stack.getItem() instanceof MachineModuleItem&&super.mayPlace(stack);}
            public int getMaxStackSize(){return 1;}
            public boolean mayPickup(Player player){return isActive()&&(!(getItem().getItem() instanceof MachineModuleItem m)||m.module().type()!=MachineModuleType.BUFFER||getEnergy()<=BioincubatorBlockEntity.ENERGY_CAPACITY);}
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        14 + column * 22,
                        161 + row * 22
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(
                    playerInventory,
                    column,
                    14 + column * 22,
                    229
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BIOINCUBATOR.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);

        if (slot == null || !slot.isActive() || !slot.mayPickup(player) || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOT_END) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if(stack.getItem() instanceof MachineModuleItem){
            if(!isModulePanelOpen()||!moveItemStackTo(stack,7,9,false))return ItemStack.EMPTY;
        } else if (isMainPanelOpen() && isValidCapsuleForMode(stack, getMode())) {
            int target = getMode() == BioincubatorBlockEntity.MODE_REPAIR ? 2 : 0;
            if (!moveItemStackTo(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isMainPanelOpen() && getMode() == BioincubatorBlockEntity.MODE_REPAIR
                && stack.is(ModItems.BIO_REPAIR_KIT.get())) {
            if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
        } else if (isMainPanelOpen() && getMode() == BioincubatorBlockEntity.MODE_REPAIR
                && stack.is(ModItems.BIOGEL.get())) {
            if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
        } else if (isMainPanelOpen() && getMode() == BioincubatorBlockEntity.MODE_REPAIR
                && stack.is(ModItems.NUTRIENT_MIX.get())) {
            if (!moveItemStackTo(stack, 5, 6, false)) return ItemStack.EMPTY;
        } else if (isMainPanelOpen() && getMode() == BioincubatorBlockEntity.MODE_INCUBATION && isKnownFeed(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_START && index < PLAYER_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    private boolean isKnownFeed(ItemStack stack) {
        var species = level.isClientSide
                ? BioModuleClientState.allSpecies()
                : BioLootData.allSpecies();
        return species.stream().anyMatch(value -> stack.is(ForgeRegistries.ITEMS.getValue(value.feedItem())));
    }

    private boolean isValidCapsuleForMode(ItemStack stack, int requestedMode) {
        BioModuleData.Sample sample = BioModuleData.sample(stack);
        // Never reject a real capsule at the GUI boundary. Wrong mode, locked
        // database and unsupported species are reported by machine status.
        return sample != null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if(!stillValid(player))return false;
        if(id==200||id==201||id==202){setTab(id);return true;}
        if(isMainPanelOpen()&&(id==52||id==53)){
            if(incubator!=null&&getMode()!=id-52)incubator.toggleMode();return true;
        }
        if (isMainPanelOpen() && id == MODE_BUTTON) {
            if (incubator != null) incubator.toggleMode();
            return true;
        }
        int sideIndex = id - SIDE_BUTTON_BASE;

        if (!isSidePanelOpen() || sideIndex < 0 || sideIndex >= RelativeSide.values().length) {
            return false;
        }

        RelativeSide side = RelativeSide.values()[sideIndex];

        if (!BioincubatorBlockEntity.isConfigurableSide(side)) {
            return false;
        }

        if (incubator != null) {
            incubator.cycleSideMode(side);
        }

        return true;
    }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    public static int modeButtonId() {
        return MODE_BUTTON;
    }

    public int getEnergy() {
        return data.get(BioincubatorBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(BioincubatorBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getWater() {
        return data.get(BioincubatorBlockEntity.DATA_WATER);
    }

    public int getWaterCapacity() {
        return data.get(BioincubatorBlockEntity.DATA_WATER_CAPACITY);
    }

    public int getProgress() {
        return data.get(BioincubatorBlockEntity.DATA_PROGRESS);
    }

    public int getProgressMax() {
        return data.get(BioincubatorBlockEntity.DATA_PROGRESS_MAX);
    }

    public int getStatus() {
        return data.get(BioincubatorBlockEntity.DATA_STATUS);
    }

    public int getSpecies() {
        return data.get(BioincubatorBlockEntity.DATA_SPECIES);
    }

    public int getMode() {
        return data.get(BioincubatorBlockEntity.DATA_MODE);
    }

    public SideMode getSideMode(RelativeSide relativeSide) {
        if (!BioincubatorBlockEntity.isConfigurableSide(relativeSide)) {
            return SideMode.DISABLED;
        }

        Direction worldDirection = relativeSide.resolve(getFacing());
        int ordinal = data.get(BioincubatorBlockEntity.DATA_SIDES_START + worldDirection.ordinal());

        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(BioincubatorBlock.FACING)
                ? state.getValue(BioincubatorBlock.FACING)
                : Direction.NORTH;
    }

    private class ModeSlot extends SlotItemHandler {
        private final int requiredMode;

        private ModeSlot(IItemHandler handler, int index, int x, int y, int requiredMode) {
            super(handler, index, x, y);
            this.requiredMode = requiredMode;
        }

        @Override
        public boolean isActive() {
            return isMainPanelOpen() && getMode() == requiredMode;
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && super.mayPickup(player);
        }
    }
}
