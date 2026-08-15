package com.wasted.domesurvival.forge.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public final class DomeProjectService {
    private DomeProjectService() {
    }

    /**
     * Takes only the resources still required by the shared workshop project.
     * Any excess remains in the player's inventory.
     */
    public static ContributionResult contributeWorkshop(ServerPlayer player) {
        DomeProgressSavedData data = DomeProgressSavedData.get(player.serverLevel());

        if (data.workshopComplete()) {
            return ContributionResult.alreadyCompletedResult();
        }

        int iron = consume(player, WorkshopProject::isIron, data.remainingIron());
        int copper = consume(player, WorkshopProject::isCopper, data.remainingCopper());
        int redstone = consume(player, WorkshopProject::isRedstone, data.remainingRedstone());

        int total = iron + copper + redstone;
        if (total > 0) {
            data.addWorkshopContribution(iron, copper, redstone);
        }

        return new ContributionResult(
                iron,
                copper,
                redstone,
                total,
                data.workshopComplete(),
                false
        );
    }

    private static int consume(ServerPlayer player, Predicate<ItemStack> matcher, int amountNeeded) {
        if (amountNeeded <= 0) {
            return 0;
        }

        Inventory inventory = player.getInventory();
        int consumed = 0;

        for (int slot = 0; slot < inventory.getContainerSize() && consumed < amountNeeded; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !matcher.test(stack)) {
                continue;
            }

            int take = Math.min(amountNeeded - consumed, stack.getCount());
            stack.shrink(take);
            consumed += take;
        }

        if (consumed > 0) {
            inventory.setChanged();
        }

        return consumed;
    }

    public record ContributionResult(
            int iron,
            int copper,
            int redstone,
            int total,
            boolean completedNow,
            boolean alreadyComplete
    ) {
        static ContributionResult alreadyCompletedResult() {
            return new ContributionResult(0, 0, 0, 0, false, true);
        }
    }
}
