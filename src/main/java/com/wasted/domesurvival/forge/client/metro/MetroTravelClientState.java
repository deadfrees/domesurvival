package com.wasted.domesurvival.forge.client.metro;

import com.wasted.domesurvival.forge.network.MetroTravelVisualPacket;

public final class MetroTravelClientState {
    private static Mode mode = Mode.NONE;
    private static String destination = "";
    private static String detail = "";
    private static long startedNanos;
    private static long endNanos;
    private static long messageEndNanos;

    private MetroTravelClientState() {
    }

    public static void handle(MetroTravelVisualPacket packet) {
        long now = System.nanoTime();

        switch (packet.phase()) {
            case START -> {
                mode = Mode.TRAVEL;
                destination = packet.destinationName();
                detail = "";
                startedNanos = now;
                long durationNanos = Math.max(1L, packet.durationTicks()) * 50_000_000L;
                endNanos = now + durationNanos;
                messageEndNanos = 0L;
            }
            case ARRIVE -> {
                mode = Mode.ARRIVAL;
                destination = packet.destinationName();
                detail = "";
                startedNanos = now;
                endNanos = now;
                messageEndNanos = now + 3_000_000_000L;
            }
            case CANCEL -> {
                mode = Mode.CANCELLED;
                destination = "";
                detail = packet.detail();
                startedNanos = now;
                endNanos = now;
                messageEndNanos = now + 3_500_000_000L;
            }
        }
    }

    public static Snapshot snapshot() {
        long now = System.nanoTime();

        if (mode == Mode.TRAVEL) {
            if (now >= endNanos) {
                return new Snapshot(mode, destination, detail, 0.0D, 0L);
            }

            long total = Math.max(1L, endNanos - startedNanos);
            long remaining = Math.max(0L, endNanos - now);
            double progress = 1.0D - (double) remaining / (double) total;
            long remainingMillis = remaining / 1_000_000L;
            return new Snapshot(mode, destination, detail, progress, remainingMillis);
        }

        if ((mode == Mode.ARRIVAL || mode == Mode.CANCELLED) && now >= messageEndNanos) {
            clear();
        }

        return new Snapshot(mode, destination, detail, 1.0D, 0L);
    }

    public static void clear() {
        mode = Mode.NONE;
        destination = "";
        detail = "";
        startedNanos = 0L;
        endNanos = 0L;
        messageEndNanos = 0L;
    }

    public enum Mode {
        NONE,
        TRAVEL,
        ARRIVAL,
        CANCELLED
    }

    public record Snapshot(
            Mode mode,
            String destination,
            String detail,
            double progress,
            long remainingMillis
    ) {
    }
}
