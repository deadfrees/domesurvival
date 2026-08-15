package com.wasted.domesurvival.core.airlock;

public record AirlockTransition(
        boolean allowed,
        AirlockState state,
        String message
) {
}
