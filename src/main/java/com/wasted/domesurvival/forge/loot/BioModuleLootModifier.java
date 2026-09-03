package com.wasted.domesurvival.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.item.BioModuleItem;
import com.wasted.domesurvival.forge.item.CryocapsuleItem;
import com.wasted.domesurvival.forge.item.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Adds zero or one unidentified module to eligible generated chest loot. */
public final class BioModuleLootModifier extends LootModifier {
    public static final Codec<BioModuleLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, BioModuleLootModifier::new));

    public BioModuleLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                  LootContext context) {
        // Other datapacks or the fixed-map placement layer may already have supplied one.
        boolean alreadyPresent = generatedLoot.stream().anyMatch(stack ->
                stack.is(ModItems.BIO_MODULE.get()) || stack.getItem() instanceof CryocapsuleItem);
        if (alreadyPresent) return generatedLoot;

        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null) return generatedLoot;

        BlockPos chestPos = null;
        if (context.hasParam(LootContextParams.ORIGIN)) {
            chestPos = BlockPos.containing(context.getParam(LootContextParams.ORIGIN));
        }
        BioLootData.SpawnChoice choice = BioLootData.roll(
                table, context.getRandom(), context.getLevel(), chestPos);
        if (choice != null) {
            generatedLoot.add(BioModuleItem.create(choice.entityId(), choice.damaged()));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.BIO_MODULE.get();
    }
}
