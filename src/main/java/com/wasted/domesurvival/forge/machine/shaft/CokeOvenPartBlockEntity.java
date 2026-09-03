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

/** Forwards item automation exclusively through the left input and right output ports. */
public final class CokeOvenPartBlockEntity extends BlockEntity {
    public CokeOvenPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN_PART.get(), pos, state);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        BlockState state = getBlockState();
        if (cap == ForgeCapabilities.ITEM_HANDLER
                && side != null
                && (CokeOvenPartBlock.isInputPort(state) || CokeOvenPartBlock.isOutputPort(state))
                && side == CokeOvenPartBlock.portSide(state)
                && level != null) {
            BlockEntity controller = level.getBlockEntity(CokeOvenPartBlock.controllerPosition(worldPosition, state));
            if (controller instanceof CokeOvenBlockEntity oven) {
                return (CokeOvenPartBlock.isInputPort(state)
                        ? oven.getInputPortCapability()
                        : oven.getOutputPortCapability()).cast();
            }
        }
        return super.getCapability(cap, side);
    }
}
