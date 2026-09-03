package com.wasted.domesurvival.forge.client.music;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side entry point for scripted music events.
 *
 * Future quest/structure/network integrations should call these methods on the
 * client thread (for network packets: from the packet's client enqueueWork).
 */
public final class DomeMusicSignals {
    private static final AtomicInteger STORY_REQUESTS = new AtomicInteger();
    private static final AtomicInteger DISCOVERY_REQUESTS = new AtomicInteger();

    private DomeMusicSignals() {
    }

    public static void requestStoryOverride() {
        STORY_REQUESTS.incrementAndGet();
    }

    public static void requestDiscovery() {
        DISCOVERY_REQUESTS.incrementAndGet();
    }

    static boolean pollStoryOverride() {
        return consumeOne(STORY_REQUESTS);
    }

    static boolean pollDiscovery() {
        return consumeOne(DISCOVERY_REQUESTS);
    }

    static void clear() {
        STORY_REQUESTS.set(0);
        DISCOVERY_REQUESTS.set(0);
    }

    private static boolean consumeOne(AtomicInteger counter) {
        while (true) {
            int value = counter.get();
            if (value <= 0) {
                return false;
            }
            if (counter.compareAndSet(value, value - 1)) {
                return true;
            }
        }
    }
}
