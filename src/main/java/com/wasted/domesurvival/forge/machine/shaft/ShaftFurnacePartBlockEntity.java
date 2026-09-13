package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Exposes strict one-way handlers through the two physical side-port cells. */
public final class ShaftFurnacePartBlockEntity extends BlockEntity {
    public ShaftFurnacePartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_FURNACE_PART.get(), pos, state);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        BlockState state = getBlockState();
        if (cap == ForgeCapabilities.ITEM_HANDLER
                && (ShaftFurnacePartBlock.isInputPort(state) || ShaftFurnacePartBlock.isOutputPort(state))
                && level != null) {
            BlockEntity controller = level.getBlockEntity(ShaftFurnacePartBlock.controllerPosition(worldPosition, state));
            if (controller instanceof ShaftFurnaceBlockEntity furnace) {
                return (ShaftFurnacePartBlock.isInputPort(state)
                        ? furnace.getInputPortCapability()
                        : furnace.getOutputPortCapability()).cast();
            }
        }
        return super.getCapability(cap, side);
    }
}
