package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.core.airlock.AirlockDoor;
import com.wasted.domesurvival.core.airlock.AirlockPressure;
import com.wasted.domesurvival.core.airlock.AirlockState;
import com.wasted.domesurvival.core.airlock.AirlockStateMachine;
import com.wasted.domesurvival.core.airlock.AirlockTransition;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public final class AirlockService {
    private AirlockService() {
    }

    public static void handlePanelUse(ServerLevel level, BlockPos panelPos, Player player) {
        AirlockDoor side = panelSide(panelPos);
        if (side == null) {
            player.displayClientMessage(Component.literal("[ШЛЮЗ] Неизвестная панель управления.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return;
        }

        AirlockTransition result = toggle(level, side);
        if (result.allowed()) {
            player.displayClientMessage(doorStatusComponent(side, result.state()), false);
        } else {
            player.displayClientMessage(interlockComponent(result.message()), false);
        }
    }

    public static AirlockTransition toggle(ServerLevel level, AirlockDoor side) {
        DomeSavedData data = DomeSavedData.get(level);
        AirlockState before = data.airlockState();
        AirlockTransition transition = AirlockStateMachine.toggle(before, side);
        if (!transition.allowed()) return transition;

        boolean opened = side == AirlockDoor.INNER
                ? transition.state().innerOpen()
                : transition.state().outerOpen();

        data.setAirlockState(transition.state());
        applyDoorVisual(level, side, opened);
        syncPanelVisuals(level, transition.state());
        playDoorSound(level, side, opened);
        return transition;
    }

    public static void reset(ServerLevel level) {
        DomeSavedData data = DomeSavedData.get(level);
        AirlockState before = data.airlockState();
        data.resetAirlock();
        applyDoorVisual(level, AirlockDoor.INNER, false);
        applyDoorVisual(level, AirlockDoor.OUTER, false);
        syncPanelVisuals(level, data.airlockState());

        if (before.innerOpen()) {
            playDoorSound(level, AirlockDoor.INNER, false);
        }
        if (before.outerOpen()) {
            playDoorSound(level, AirlockDoor.OUTER, false);
        }
    }

    /** Synchronizes panel indicators with the persisted door state. */
    public static void syncVisuals(ServerLevel level) {
        syncPanelVisuals(level, DomeSavedData.get(level).airlockState());
    }

    private static void syncPanelVisuals(ServerLevel level, AirlockState state) {
        DomeSpec spec = DomeSpec.wastedV1();
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerPanelZ()),
                state.innerOpen());
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerDomePanelZ()),
                state.innerOpen());
        applyPanelVisual(level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.outerPanelZ()),
                state.outerOpen());
    }

    private static void applyPanelVisual(ServerLevel level, BlockPos pos, boolean open) {
        var state = level.getBlockState(pos);
        if (state.is(ModBlocks.AIRLOCK_PANEL.get())) {
            level.setBlock(pos, state.setValue(AirlockPanelBlock.LIT, open), 3);
        }
    }

    public static boolean isBreathable(ServerLevel level) {
        return DomeSavedData.get(level).airlockState().breathable();
    }

    public static AirlockState state(ServerLevel level) {
        return DomeSavedData.get(level).airlockState();
    }

    /** One compact line for an explicit /dome airlock status request. */
    public static Component statusComponent(AirlockState state) {
        if (state.outerOpen() || state.pressure() == AirlockPressure.DEPRESSURIZED) {
            return Component.literal("[ШЛЮЗ] ВНЕШНЯЯ ОТКРЫТА • КАМЕРА РАЗГЕРМЕТИЗИРОВАНА")
                    .withStyle(ChatFormatting.RED);
        }
        if (state.innerOpen()) {
            return Component.literal("[ШЛЮЗ] ВНУТРЕННЯЯ ОТКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.literal("[ШЛЮЗ] ОБЕ ДВЕРИ ЗАКРЫТЫ • КАМЕРА ПОД ДАВЛЕНИЕМ")
                .withStyle(ChatFormatting.GREEN);
    }

    /** Compact action feedback for the door that was actually operated. */
    public static Component doorStatusComponent(AirlockDoor door, AirlockState state) {
        if (door == AirlockDoor.INNER) {
            if (state.innerOpen()) {
                return Component.literal("[ШЛЮЗ] ВНУТРЕННЯЯ ОТКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                        .withStyle(ChatFormatting.RED);
            }
            return Component.literal("[ШЛЮЗ] ВНУТРЕННЯЯ ЗАКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                    .withStyle(ChatFormatting.GREEN);
        }

        if (state.outerOpen() || state.pressure() == AirlockPressure.DEPRESSURIZED) {
            return Component.literal("[ШЛЮЗ] ВНЕШНЯЯ ОТКРЫТА • КАМЕРА РАЗГЕРМЕТИЗИРОВАНА")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.literal("[ШЛЮЗ] ВНЕШНЯЯ ЗАКРЫТА • КАМЕРА ПОД ДАВЛЕНИЕМ")
                .withStyle(ChatFormatting.GREEN);
    }

    public static Component interlockComponent(String detail) {
        return Component.literal("[ШЛЮЗ] БЛОКИРОВКА • " + detail)
                .withStyle(ChatFormatting.YELLOW);
    }

    private static AirlockDoor panelSide(BlockPos pos) {
        DomeSpec spec = DomeSpec.wastedV1();
        if (pos.getX() != spec.airlockPanelX() || pos.getY() != spec.airlockPanelY()) return null;
        if (pos.getZ() == spec.innerPanelZ() || pos.getZ() == spec.innerDomePanelZ()) {
            return AirlockDoor.INNER;
        }
        if (pos.getZ() == spec.outerPanelZ()) return AirlockDoor.OUTER;
        return null;
    }

    private static void applyDoorVisual(ServerLevel level, AirlockDoor side, boolean open) {
        DomeSpec spec = DomeSpec.wastedV1();
        int z = side == AirlockDoor.INNER ? spec.innerDoorZ() : spec.outerDoorZ();

        for (int x = spec.airlockCenterX() - spec.airlockDoorHalfWidth();
             x <= spec.airlockCenterX() + spec.airlockDoorHalfWidth(); x++) {
            for (int y = spec.airlockDoorMinY(); y <= spec.airlockDoorMaxY(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                level.setBlock(pos,
                        open ? Blocks.AIR.defaultBlockState() : ModBlocks.AIRLOCK_DOOR.get().defaultBlockState(),
                        3);
            }
        }
    }

    private static void playDoorSound(ServerLevel level, AirlockDoor side, boolean opened) {
        DomeSpec spec = DomeSpec.wastedV1();
        int z = side == AirlockDoor.INNER ? spec.innerDoorZ() : spec.outerDoorZ();
        BlockPos soundPos = new BlockPos(spec.airlockCenterX(), spec.baseY() + 2, z);

        level.playSound(
                null,
                soundPos,
                opened ? ModSounds.AIRLOCK_OPEN.get() : ModSounds.AIRLOCK_CLOSE.get(),
                SoundSource.BLOCKS,
                1.25F,
                1.0F
        );
    }

    /** Kept for debug/status output compatibility. */
    public static String localizedState(AirlockState state) {
        return "внутренняя=" + (state.innerOpen() ? "ОТКРЫТА" : "закрыта")
                + ", внешняя=" + (state.outerOpen() ? "ОТКРЫТА" : "закрыта")
                + ", камера=" + (state.pressure() == AirlockPressure.PRESSURIZED
                ? "ПОД ДАВЛЕНИЕМ" : "РАЗГЕРМЕТИЗИРОВАНА");
    }
}
