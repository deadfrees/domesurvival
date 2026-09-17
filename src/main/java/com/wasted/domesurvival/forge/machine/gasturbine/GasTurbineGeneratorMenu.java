package com.wasted.domesurvival.forge.machine.gasturbine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GasTurbineGeneratorMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public GasTurbineGeneratorMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory,
                new SimpleContainerData(GasTurbineGeneratorBlockEntity.DATA_COUNT),
                extraData.readBlockPos());
    }

    public GasTurbineGeneratorMenu(int id, Inventory playerInventory, GasTurbineGeneratorBlockEntity turbine) {
        this(id, playerInventory, turbine.getDataAccess(), turbine.getBlockPos());
    }

    private GasTurbineGeneratorMenu(int id, Inventory playerInventory, ContainerData data, BlockPos pos) {
        super(GasTurbineGeneratorRegistry.GAS_TURBINE_GENERATOR_MENU.get(), id);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        checkContainerDataCount(data, GasTurbineGeneratorBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, GasTurbineGeneratorRegistry.GAS_TURBINE_GENERATOR.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int energyStored() { return data.get(GasTurbineGeneratorBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(GasTurbineGeneratorBlockEntity.DATA_ENERGY_CAPACITY); }
    public int gasStored() { return data.get(GasTurbineGeneratorBlockEntity.DATA_GAS); }
    public int gasCapacity() { return data.get(GasTurbineGeneratorBlockEntity.DATA_GAS_CAPACITY); }
    public int status() { return data.get(GasTurbineGeneratorBlockEntity.DATA_STATUS); }
    public int generationPerTick() { return data.get(GasTurbineGeneratorBlockEntity.DATA_GENERATION); }
}
