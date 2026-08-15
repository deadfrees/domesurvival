package com.wasted.domesurvival.forge.airlock;

import com.mojang.brigadier.CommandDispatcher;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * One-time migration for the original start-dome airlock control.
 *
 * V53E deliberately does not guess the legacy block registry id. The player
 * looks directly at the old "Панель управления шлюзом" block and runs
 * /dsairlock migrate. That exact targeted block becomes dome wall, while the
 * new thin panel is mounted in front of that wall on the face the player hit.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AirlockMigrationCommands {
    private static final double TARGET_DISTANCE = 8.0D;

    private AirlockMigrationCommands() { }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("dsairlock")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("migrate")
                        .executes(context -> migrate(context.getSource()))));
    }

    private static int migrate(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Run this command as a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        HitResult hit = player.pick(TARGET_DISTANCE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable("message.domesurvival.airlock.look_at_legacy_panel"));
            return 0;
        }

        BlockPos legacyPos = blockHit.getBlockPos();
        Direction outward = blockHit.getDirection();
        if (!outward.getAxis().isHorizontal()) {
            source.sendFailure(Component.translatable("message.domesurvival.airlock.look_at_panel_front"));
            return 0;
        }

        BlockState legacyState = level.getBlockState(legacyPos);
        if (legacyState.isAir() || legacyState.getBlock() == AirlockPanelRegistry.block()) {
            source.sendFailure(Component.translatable("message.domesurvival.airlock.look_at_legacy_panel"));
            return 0;
        }

        BlockState wallState = inferDomeWallState(level, legacyPos);
        BlockPos panelPos = legacyPos.relative(outward);
        BlockState panelSpace = level.getBlockState(panelPos);

        if (!panelSpace.canBeReplaced()) {
            source.sendFailure(Component.translatable("message.domesurvival.airlock.panel_space_blocked"));
            return 0;
        }

        // 1) Restore the old full-block control position as normal dome wall.
        level.setBlock(legacyPos, wallState, Block.UPDATE_ALL);

        // 2) Mount the new thin panel in the adjacent air block, on the face
        //    selected by the player's crosshair.
        Block panel = AirlockPanelRegistry.block();
        BlockState panelState = panel.defaultBlockState()
                .setValue(AirlockControlPanelBlock.FACING, outward)
                .setValue(AirlockControlPanelBlock.ACTIVE, false);

        if (!panelState.canSurvive(level, panelPos)) {
            // Put the legacy block back if something unexpected prevents the
            // wall-mounted panel from surviving.
            level.setBlock(legacyPos, legacyState, Block.UPDATE_ALL);
            source.sendFailure(Component.translatable("message.domesurvival.airlock.panel_surface_not_found"));
            return 0;
        }

        level.setBlock(panelPos, panelState, Block.UPDATE_ALL);
        AirlockService.reset(level);

        source.sendSuccess(
                () -> Component.translatable("message.domesurvival.airlock.migrated_targeted")
                        .append(Component.literal(" [" + panelPos.toShortString() + "]")),
                true
        );
        return 1;
    }

    /**
     * Picks the most common nearby full collision block as dome-wall material.
     * This lets the migration follow the actual wall palette instead of
     * hardcoding the old control block id. Iron block remains a safe fallback
     * for the current start dome.
     */
    private static BlockState inferDomeWallState(ServerLevel level, BlockPos legacyPos) {
        Map<Block, Integer> counts = new HashMap<>();
        Map<Block, BlockState> samples = new HashMap<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = legacyPos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);

            if (neighbor.isAir()
                    || neighbor.getBlock() == AirlockPanelRegistry.block()
                    || neighbor.hasProperty(BlockStateProperties.OPEN)
                    || !neighbor.isCollisionShapeFullBlock(level, neighborPos)) {
                continue;
            }

            Block block = neighbor.getBlock();
            counts.merge(block, 1, Integer::sum);
            samples.putIfAbsent(block, neighbor);
        }

        Block bestBlock = null;
        int bestCount = -1;
        for (Map.Entry<Block, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestBlock = entry.getKey();
                bestCount = entry.getValue();
            }
        }

        return bestBlock != null
                ? samples.get(bestBlock)
                : Blocks.IRON_BLOCK.defaultBlockState();
    }
}
