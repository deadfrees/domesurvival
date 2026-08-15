package com.wasted.domesurvival.forge.pipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PipeWrenchReliableInteractionEvents {
    private static final ResourceLocation MACHINE_WRENCH_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "machine_wrench");

    private PipeWrenchReliableInteractionEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        if (!isEngineerWrench(event.getItemStack())) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Direction side = findTargetSide(level, pos, event.getHitVec());
        if (side == null) return;

        // Prevent Item#useOn / Block#use from cycling connector modes after we split the pipe.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));

        if (level instanceof ServerLevel serverLevel) {
            PipeWrenchConnectionService.toggle(serverLevel, pos, side, player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        if (!isEngineerWrench(event.getItemStack())) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Direction side = findLeftClickSide(level, pos, event.getFace());
        if (side == null) return;

        // Also prevents the wrench gesture from mining/destroying the pipe.
        event.setCanceled(true);

        if (level instanceof ServerLevel serverLevel) {
            PipeWrenchConnectionService.toggle(serverLevel, pos, side, player);
        }
    }

    private static boolean isEngineerWrench(ItemStack stack) {
        return !stack.isEmpty()
                && MACHINE_WRENCH_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static Direction findTargetSide(Level level, BlockPos pos, BlockHitResult hit) {
        List<Direction> connected = sameFamilySides(level, pos);
        if (connected.isEmpty()) return null;
        if (connected.size() == 1) return connected.get(0);

        // If Minecraft's face is an actual pipe-to-pipe boundary, respect it first.
        Direction face = hit.getDirection();
        if (connected.contains(face)) return face;

        // Otherwise choose the real pipe arm nearest to the click location. This is the
        // important fix for thin pipe models inside a full Minecraft block position.
        Vec3 local = hit.getLocation().subtract(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );

        Direction best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : connected) {
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            Vec3 pickPoint = normal.scale(0.46D);
            double distance = local.distanceToSqr(pickPoint);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = direction;
            }
        }
        return best;
    }

    private static Direction findLeftClickSide(Level level, BlockPos pos, Direction face) {
        List<Direction> connected = sameFamilySides(level, pos);
        if (connected.isEmpty()) return null;
        if (face != null && connected.contains(face)) return face;
        if (connected.size() == 1) return connected.get(0);
        return null;
    }

    private static List<Direction> sameFamilySides(Level level, BlockPos pos) {
        List<Direction> result = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            if (PipeWrenchConnectionService.hasSameFamilyPipe(level, pos, direction)) {
                result.add(direction);
            }
        }
        return result;
    }
}
