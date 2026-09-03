package com.wasted.domesurvival.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wasted.domesurvival.forge.technology.TechnologyUnlockService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import java.util.Iterator;

/**
 * Prevents generated structure chests from bypassing the shared technology
 * progression. Raw resources and components explicitly exempted by
 * {@code TechnologyRegistry} remain untouched. A locked finished product is
 * converted into a small amount of generic salvage, so a chest is never made
 * completely worthless merely because it was opened early.
 */
public final class TechnologySalvageLootModifier extends LootModifier {
    public static final Codec<TechnologySalvageLootModifier> CODEC =
            RecordCodecBuilder.create(instance ->
                    codecStart(instance).apply(instance, TechnologySalvageLootModifier::new));

    private static final int MAX_SALVAGE_ROLLS = 3;

    public TechnologySalvageLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                  LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (!isStructureChest(table)) {
            return generatedLoot;
        }

        int removedLockedStacks = 0;
        Iterator<ItemStack> iterator = generatedLoot.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (!stack.isEmpty() && !TechnologyUnlockService.isUnlocked(context.getLevel(), stack)) {
                iterator.remove();
                removedLockedStacks++;
            }
        }

        RandomSource random = context.getRandom();
        int salvageRolls = Math.min(removedLockedStacks, MAX_SALVAGE_ROLLS);
        for (int roll = 0; roll < salvageRolls; roll++) {
            generatedLoot.add(createSalvage(random));
        }
        return generatedLoot;
    }

    static boolean isStructureChest(ResourceLocation table) {
        if (table == null) {
            return false;
        }
        String path = "/" + table.getPath() + "/";
        return path.contains("/chests/");
    }

    private static ItemStack createSalvage(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> new ItemStack(Items.COPPER_INGOT, 1 + random.nextInt(2));
            case 1 -> new ItemStack(Items.REDSTONE, 1 + random.nextInt(3));
            case 2 -> new ItemStack(Items.COAL, 2 + random.nextInt(3));
            default -> new ItemStack(Items.IRON_NUGGET, 3 + random.nextInt(5));
        };
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.TECHNOLOGY_SALVAGE.get();
    }
}
