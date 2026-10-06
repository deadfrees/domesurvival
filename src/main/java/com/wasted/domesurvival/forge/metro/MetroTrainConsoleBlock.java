package com.wasted.domesurvival.forge.metro;

import com.wasted.domesurvival.forge.metro.network.MetroNetworkMenu;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkSavedData;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** Opens the Stage-6 metro map using a bounded SavedData snapshot. */
public final class MetroTrainConsoleBlock extends Block {
    public MetroTrainConsoleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state,
                                 Level level,
                                 BlockPos pos,
                                 Player player,
                                 InteractionHand hand,
                                 BlockHitResult hit) {
        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {

            MetroNetworkService.synchronize(serverLevel.getServer());
            MetroNetworkSavedData network = MetroNetworkSavedData.get(serverLevel.getServer());

            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new MetroNetworkMenu(containerId, inventory, pos, network),
                            Component.literal("\u0421\u0435\u0442\u044c \u043c\u0435\u0442\u0440\u043e")
                    ),
                    buf -> MetroNetworkMenu.writeOpenData(buf, pos, network)
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
