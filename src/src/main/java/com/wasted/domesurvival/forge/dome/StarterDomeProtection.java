package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.core.dome.BlockPoint;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeStructurePlanner;
import com.wasted.domesurvival.core.dome.PlannedBlock;
import com.wasted.domesurvival.core.dome.StructureMaterial;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Protects only the authored structural shell of the starter dome.
 *
 * Player-placed copies of reinforced_glass / dome_frame / dome_foundation
 * outside the starter structure remain normal breakable blocks. Creative mode
 * is intentionally allowed to edit the starter structure for administration.
 *
 * The protected position mask is derived once from the same pure-Java planner
 * that builds the starter dome, so there is no world scan and no per-tick work.
 */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class StarterDomeProtection {
    private static final Set<Long> PROTECTED_POSITIONS = buildProtectedPositions();

    private StarterDomeProtection() {
    }

    /**
     * Prevent normal/fake survival players from mining authored structure.
     * Forge BreakEvent explicitly supports cancellation to stop player breaking.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.getPlayer().isCreative()) {
            return;
        }

        if (isProtectedStructureBlock(level, event.getPos(), event.getState())) {
            event.setCanceled(true);
        }
    }

    /**
     * TNT, creepers and modded explosions may still damage entities, but the
     * starter-dome structural blocks are removed from the explosion block list.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !DomeSavedData.get(level).isGenerated()) {
            return;
        }

        event.getAffectedBlocks().removeIf(pos ->
                isProtectedStructureBlock(level, pos, level.getBlockState(pos))
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
     * Pistons must not move or destroy a protected wall/foundation/glass block.
     * This runs only when a piston attempts movement; there is no tick cost.
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
            if (isProtectedStructureBlock(level, pos, level.getBlockState(pos))) {
                event.setCanceled(true);
                return;
            }
        }

        for (BlockPos pos : resolver.getToDestroy()) {
            if (isProtectedStructureBlock(level, pos, level.getBlockState(pos))) {
                event.setCanceled(true);
                return;
            }
        }
    }

    public static boolean isProtectedStructureBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (!DomeSavedData.get(level).isGenerated()) {
            return false;
        }

        if (!PROTECTED_POSITIONS.contains(pos.asLong())) {
            return false;
        }

        return state.is(ModBlocks.REINFORCED_GLASS.get())
                || state.is(ModBlocks.DOME_FRAME.get())
                || state.is(ModBlocks.DOME_FOUNDATION.get());
    }

    private static Set<Long> buildProtectedPositions() {
        Set<Long> positions = new HashSet<>();

        for (PlannedBlock planned :
                DomeStructurePlanner.planFullV23(DomeSpec.wastedV1())) {
            StructureMaterial material = planned.material();

            /*
             * AIRLOCK_PANEL is included in the mask because V58 converts those
             * old full-block panel support slots into normal DOME_FOUNDATION
             * wall blocks before mounting the thin panel on the outside face.
             */
            if (material != StructureMaterial.GLASS
                    && material != StructureMaterial.FRAME
                    && material != StructureMaterial.FOUNDATION
                    && material != StructureMaterial.AIRLOCK_PANEL) {
                continue;
            }

            BlockPoint point = planned.point();
            positions.add(new BlockPos(point.x(), point.y(), point.z()).asLong());
        }

        return Set.copyOf(positions);
    }
}
