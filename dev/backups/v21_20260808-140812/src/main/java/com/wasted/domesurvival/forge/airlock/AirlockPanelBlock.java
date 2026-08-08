package com.wasted.domesurvival.forge.airlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AirlockPanelBlock extends Block {
    public AirlockPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            AirlockService.handlePanelUse(serverLevel, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
