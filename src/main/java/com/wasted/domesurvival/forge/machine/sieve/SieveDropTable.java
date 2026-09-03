package com.wasted.domesurvival.forge.machine.sieve;

import com.wasted.domesurvival.forge.item.SieveMeshItem;

/**
 * Single source of truth for sand-sifting probabilities.
 *
 * In dry mode {@code clayPercent} is the chance for one clay ball and all
 * listed results share one roll. In wet mode one clay ball is guaranteed,
 * {@code clayPercent} is the chance for a second ball, and the by-products
 * share a separate roll.
 */
public final class SieveDropTable {
    private SieveDropTable() {
    }

    public static Chances dry(SieveMeshItem.Tier tier) {
        return switch (tier) {
            case FIBER -> new Chances(12, 18, 10, 0, 0, 0, 0);
            case COPPER -> new Chances(15, 12, 8, 7, 4, 0, 0);
            case STEEL -> new Chances(18, 10, 7, 8, 5, 3, 2);
        };
    }

    public static Chances wet(SieveMeshItem.Tier tier) {
        return switch (tier) {
            case FIBER -> new Chances(30, 0, 15, 0, 0, 0, 0);
            case COPPER -> new Chances(50, 0, 12, 12, 8, 0, 0);
            case STEEL -> new Chances(70, 0, 10, 14, 10, 5, 3);
        };
    }

    public record Chances(
            int clayPercent,
            int flintPercent,
            int boneMealPercent,
            int rawCopperPercent,
            int ironNuggetPercent,
            int goldNuggetPercent,
            int redstonePercent
    ) {
    }
}
