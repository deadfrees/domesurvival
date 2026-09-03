package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.core.dome.BlockPoint;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeStructurePlanner;
import com.wasted.domesurvival.core.dome.PlannedBlock;
import com.wasted.domesurvival.core.dome.StructureMaterial;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.AirlockPanelRegistry;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Makes the authored starter-dome structure gameplay-indestructible.
 *
 * Protected:
 * - reinforced glass, frame and foundation at authored starter-dome positions;
 * - both generated 5x5 airlock gates;
 * - all three generated/migrated gate control panels.
 *
 * Player-placed copies of these blocks elsewhere remain normal blocks.
 * Creative mining is intentionally blocked too. Administrative world-editing
 * commands are not player mining and therefore remain available for maintenance.
 *
 * The protection mask is built once from the same pure-Java planner that builds
 * the starter dome. There is no world scan and no per-tick work.
 */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StarterDomeProtection {
    private static final ProtectionMask PROTECTION_MASK = buildProtectionMask();

    private StarterDomeProtection() {
    }

    /**
     * Cancel player mining for both survival and creative players.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (isProtectedStructureBlock(level, event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
    }

    /**
     * TNT, creepers and modded explosions may still damage entities, but authored
     * starter-dome blocks are removed from the explosion block-destruction list.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !DomeSavedData.get(level).isGenerated()) {
            return;
        }

        event.getAffectedBlocks().removeIf(pos ->
                isProtectedPositionAndState(level, pos, level.getBlockState(pos))
        );
    }

    /**
     * Covers wither/dragon/zombie-style entity block destruction exposed by
     * Forge's LivingDestroyBlockEvent.
     */
    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        if (isProtectedStructureBlock(level, event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
    }

    /**
     * Pistons cannot move or destroy any authored starter-dome structural block
     * or airlock hardware.
     */
    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !DomeSavedData.get(level).isGenerated()) {
            return;
        }

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            return;
        }

        for (BlockPos pos : resolver.getToPush()) {
            if (isProtectedPositionAndState(level, pos, level.getBlockState(pos))) {
                event.setCanceled(true);
                return;
            }
        }

        for (BlockPos pos : resolver.getToDestroy()) {
            if (isProtectedPositionAndState(level, pos, level.getBlockState(pos))) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * Public helper for other DomeSurvival systems that need the same structure
     * protection predicate.
     */
    public static boolean isProtectedStructureBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        return DomeSavedData.get(level).isGenerated()
                && isProtectedPositionAndState(level, pos, state);
    }

    private static boolean isProtectedPositionAndState(ServerLevel level, BlockPos pos, BlockState state) {
        DomeSpec legacy = DomeSpec.wastedV1();
        DomeSpec current = DomeSavedData.get(level).domeSpec();
        long packedPos = pos.offset(
                legacy.centerX() - current.centerX(),
                legacy.baseY() - current.baseY(),
                legacy.centerZ() - current.centerZ()
        ).asLong();

        if (PROTECTION_MASK.structurePositions().contains(packedPos)) {
            return isAuthoredStructureState(state);
        }

        return PROTECTION_MASK.panelMountPositions().contains(packedPos)
                && state.is(AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get());
    }

    private static boolean isAuthoredStructureState(BlockState state) {
        return state.is(ModBlocks.REINFORCED_GLASS.get())
                || state.is(ModBlocks.DOME_FRAME.get())
                || state.is(ModBlocks.DOME_FOUNDATION.get())
                || state.is(AirlockGateRegistry.AIRLOCK_GATE.get())
                || state.is(ModBlocks.AIRLOCK_DOOR.get())
                || state.is(AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get())
                || state.is(ModBlocks.AIRLOCK_PANEL.get());
    }

    /**
     * Builds two primitive-long lookup masks:
     * 1) exact authored full-block structure positions;
     * 2) the four possible horizontal mount positions around each authored
     *    control-panel support, so V58 panels migrated on a non-default side
     *    remain protected too.
     */
    private static ProtectionMask buildProtectionMask() {
        LongSet structurePositions = new LongOpenHashSet();
        LongSet panelMountPositions = new LongOpenHashSet();

        for (PlannedBlock planned :
                DomeStructurePlanner.planFullV23(DomeSpec.wastedV1())) {
            StructureMaterial material = planned.material();

            if (material != StructureMaterial.GLASS
                    && material != StructureMaterial.FRAME
                    && material != StructureMaterial.FOUNDATION
                    && material != StructureMaterial.AIRLOCK_DOOR
                    && material != StructureMaterial.AIRLOCK_PANEL) {
                continue;
            }

            BlockPoint point = planned.point();
            BlockPos pos = new BlockPos(point.x(), point.y(), point.z());
            structurePositions.add(pos.asLong());

            if (material == StructureMaterial.AIRLOCK_PANEL) {
                panelMountPositions.add(new BlockPos(point.x() - 1, point.y(), point.z()).asLong());
                panelMountPositions.add(new BlockPos(point.x() + 1, point.y(), point.z()).asLong());
                panelMountPositions.add(new BlockPos(point.x(), point.y(), point.z() - 1).asLong());
                panelMountPositions.add(new BlockPos(point.x(), point.y(), point.z() + 1).asLong());
            }
        }

        return new ProtectionMask(structurePositions, panelMountPositions);
    }

    private record ProtectionMask(
            LongSet structurePositions,
            LongSet panelMountPositions
    ) {
    }
}
