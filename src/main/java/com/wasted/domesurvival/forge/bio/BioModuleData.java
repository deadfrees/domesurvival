package com.wasted.domesurvival.forge.bio;

import com.wasted.domesurvival.forge.item.BioModuleItem;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.quest.QuestProgressService;
import com.wasted.domesurvival.forge.technology.TechnologyClientState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Shared compatibility layer for data-driven modules and the four legacy capsules. */
public final class BioModuleData {
    public static final String IDENTIFICATION_FLAG = "BIO_MODULE_IDENTIFICATION_UNLOCKED";

    private static final ResourceLocation CHICKEN = new ResourceLocation("minecraft", "chicken");
    private static final ResourceLocation SHEEP = new ResourceLocation("minecraft", "sheep");
    private static final ResourceLocation COW = new ResourceLocation("minecraft", "cow");
    private static final ResourceLocation PIG = new ResourceLocation("minecraft", "pig");

    private BioModuleData() {
    }

    public static boolean isIdentificationUnlocked(@Nullable Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return QuestProgressService.has(serverLevel, IDENTIFICATION_FLAG);
        }
        return level != null && level.isClientSide && TechnologyClientState.has(IDENTIFICATION_FLAG);
    }

    /** Used by item names, whose vanilla API does not provide a Level. */
    public static boolean isIdentificationUnlockedOnClient() {
        return TechnologyClientState.has(IDENTIFICATION_FLAG);
    }

    public static boolean isModule(ItemStack stack) {
        return sample(stack) != null;
    }

    @Nullable
    public static Sample sample(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.BIO_MODULE.get())) {
            ResourceLocation entityId = BioModuleItem.entityId(stack);
            return entityId == null ? null : new Sample(entityId, BioModuleItem.hasDamagedGenome(stack));
        }
        if (stack.is(ModItems.CHICKEN_CRYOCAPSULE.get())) {
            return new Sample(CHICKEN, false);
        }
        if (stack.is(ModItems.SHEEP_CRYOCAPSULE.get())) {
            return new Sample(SHEEP, false);
        }
        if (stack.is(ModItems.COW_CRYOCAPSULE.get())) {
            return new Sample(COW, false);
        }
        if (stack.is(ModItems.DAMAGED_PIG_CRYOCAPSULE.get())) {
            return new Sample(PIG, true);
        }
        return null;
    }

    public record Sample(ResourceLocation entityId, boolean damaged) {
    }
}
