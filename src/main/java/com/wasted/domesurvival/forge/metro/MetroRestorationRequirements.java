package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Stage-3 restoration requirements.
 *
 * V12 uses only items confirmed to exist in the current DomeSurvival build.
 * No tin rod/tin wire or other speculative item IDs are required.
 */
public final class MetroRestorationRequirements {
    public static final Requirement STEEL_ROD = req(
            "steel_rod",
            "gui.domesurvival.metro.restoration.resource.steel_rod",
            "domesurvival:steel_rod",
            16
    );
    public static final Requirement COPPER_INGOT = req(
            "copper_ingot",
            "gui.domesurvival.metro.restoration.resource.copper_ingot",
            "minecraft:copper_ingot",
            24
    );
    public static final Requirement COPPER_WIRE = req(
            "copper_wire",
            "gui.domesurvival.metro.restoration.resource.copper_wire",
            "domesurvival:copper_wire",
            24
    );
    public static final Requirement STEEL_WIRE = req(
            "steel_wire",
            "gui.domesurvival.metro.restoration.resource.steel_wire",
            "domesurvival:steel_wire",
            16
    );
    public static final Requirement SILVER_ROD = req(
            "silver_rod",
            "gui.domesurvival.metro.restoration.resource.silver_rod",
            "domesurvival:silver_rod",
            8
    );
    public static final Requirement VOLTARIUM_ROD = req(
            "voltarium_rod",
            "gui.domesurvival.metro.restoration.resource.voltarium_rod",
            "domesurvival:voltarium_rod",
            6
    );
    public static final Requirement REDSTONE_DUST = req(
            "redstone_dust",
            "gui.domesurvival.metro.restoration.resource.redstone_dust",
            "minecraft:redstone",
            16
    );
    public static final Requirement AIRLOCK_GATE = req(
            "airlock_gate",
            "gui.domesurvival.metro.restoration.resource.airlock_gate",
            "domesurvival:airlock_gate",
            25
    );
    public static final Requirement AIRLOCK_CONTROL_PANEL = req(
            "airlock_control_panel",
            "gui.domesurvival.metro.restoration.resource.airlock_control_panel",
            "domesurvival:airlock_control_panel",
            2
    );

    private static final List<Requirement> ALL = List.of(
            STEEL_ROD,
            COPPER_INGOT,
            COPPER_WIRE,
            STEEL_WIRE,
            SILVER_ROD,
            VOLTARIUM_ROD,
            REDSTONE_DUST,
            AIRLOCK_GATE,
            AIRLOCK_CONTROL_PANEL
    );

    private MetroRestorationRequirements() {
    }

    private static Requirement req(String id, String translationKey, String itemId, int required) {
        return new Requirement(id, translationKey, new ResourceLocation(itemId), required);
    }

    public static List<Requirement> all() {
        return ALL;
    }

    public static Requirement byId(String id) {
        for (Requirement requirement : ALL) {
            if (requirement.id().equals(id)) return requirement;
        }
        return null;
    }

    public static int count(Inventory inventory, Requirement requirement) {
        int found = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (requirement.matches(stack)) found += stack.getCount();
        }
        return found;
    }

    public static boolean isComplete(MetroStationRecord record) {
        for (Requirement requirement : ALL) {
            if (record.restorationDeposit(requirement.id()) < requirement.required()) return false;
        }
        return true;
    }

    public static boolean hasAnythingToDeposit(Inventory inventory, MetroStationRecord record) {
        return planDeposit(inventory, record) != null;
    }

    public static DepositPlan planDeposit(Inventory inventory, MetroStationRecord record) {
        int size = inventory.getContainerSize();
        int[] remainingInSlot = new int[size];
        for (int slot = 0; slot < size; slot++) {
            remainingInSlot[slot] = inventory.getItem(slot).getCount();
        }

        List<Take> takes = new ArrayList<>();
        Map<String, Integer> contributions = new LinkedHashMap<>();

        for (Requirement requirement : ALL) {
            int deposited = record.restorationDeposit(requirement.id());
            int needed = Math.max(0, requirement.required() - deposited);
            if (needed <= 0) continue;

            int contributed = 0;
            for (int slot = 0; slot < size && needed > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (remainingInSlot[slot] <= 0 || !requirement.matches(stack)) continue;

                int take = Math.min(needed, remainingInSlot[slot]);
                takes.add(new Take(slot, take, requirement));
                remainingInSlot[slot] -= take;
                needed -= take;
                contributed += take;
            }

            if (contributed > 0) contributions.put(requirement.id(), contributed);
        }

        if (takes.isEmpty()) return null;
        return new DepositPlan(List.copyOf(takes), Map.copyOf(contributions));
    }

    public record Requirement(String id, String translationKey, ResourceLocation itemId, int required) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }

        public boolean registered() {
            return BuiltInRegistries.ITEM.containsKey(itemId);
        }

        public ItemStack displayStack() {
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        public Component displayName() {
            ItemStack stack = displayStack();
            if (!stack.isEmpty()) return stack.getHoverName();
            return Component.literal(switch (id) {
                case "steel_rod" -> "\u0421\u0442\u0430\u043b\u044c\u043d\u043e\u0439 \u0441\u0442\u0435\u0440\u0436\u0435\u043d\u044c";
                case "copper_ingot" -> "\u041c\u0435\u0434\u043d\u044b\u0439 \u0441\u043b\u0438\u0442\u043e\u043a";
                case "copper_wire" -> "\u041c\u0435\u0434\u043d\u0430\u044f \u043f\u0440\u043e\u0432\u043e\u043b\u043e\u043a\u0430";
                case "steel_wire" -> "\u0421\u0442\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0432\u043e\u043b\u043e\u043a\u0430";
                case "silver_rod" -> "\u0421\u0435\u0440\u0435\u0431\u0440\u044f\u043d\u044b\u0439 \u0441\u0442\u0435\u0440\u0436\u0435\u043d\u044c";
                case "voltarium_rod" -> "\u0412\u043e\u043b\u044c\u0442\u0430\u0440\u0438\u0443\u043c\u043d\u044b\u0439 \u0441\u0442\u0435\u0440\u0436\u0435\u043d\u044c";
                case "redstone_dust" -> "\u0420\u0435\u0434\u0441\u0442\u043e\u0443\u043d\u043e\u0432\u0430\u044f \u043f\u044b\u043b\u044c";
                case "airlock_gate" -> "\u0411\u043b\u043e\u043a \u0432\u043e\u0440\u043e\u0442 \u0448\u043b\u044e\u0437\u0430";
                case "airlock_control_panel" -> "\u041f\u0430\u043d\u0435\u043b\u044c \u0443\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u044f \u0448\u043b\u044e\u0437\u043e\u043c";
                default -> itemId.toString();
            });
        }
    }

    private record Take(int slot, int count, Requirement requirement) {
    }

    public static final class DepositPlan {
        private final List<Take> takes;
        private final Map<String, Integer> contributions;

        private DepositPlan(List<Take> takes, Map<String, Integer> contributions) {
            this.takes = takes;
            this.contributions = contributions;
        }

        public Map<String, Integer> contributions() {
            return contributions;
        }

        public boolean stillValid(Inventory inventory) {
            int[] reserved = new int[inventory.getContainerSize()];
            for (Take take : takes) {
                if (take.slot() < 0 || take.slot() >= inventory.getContainerSize()) return false;
                ItemStack stack = inventory.getItem(take.slot());
                if (!take.requirement().matches(stack)) return false;
                reserved[take.slot()] += take.count();
                if (reserved[take.slot()] > stack.getCount()) return false;
            }
            return true;
        }

        public void consume(Inventory inventory) {
            if (!stillValid(inventory)) {
                throw new IllegalStateException("Metro restoration deposit changed before commit");
            }
            for (Take take : takes) {
                inventory.getItem(take.slot()).shrink(take.count());
            }
            inventory.setChanged();
        }
    }
}
