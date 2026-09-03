package com.wasted.domesurvival.forge.horror;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HorrorMobMechanics {
    private static final UUID MOVEMENT_MODIFIER_ID =
            UUID.fromString("a62b3d49-091d-49e2-8df1-395f11b64f21");

    private HorrorMobMechanics() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)
                || !HorrorMobProfiles.isNativeHorrorTarget(living)) {
            return;
        }

        double amount = HorrorMobProfiles.movementSpeedModifier(living);
        if (Double.isNaN(amount)) {
            return;
        }

        AttributeInstance movement = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        // Defensive removal makes chunk reloads / dimension changes idempotent.
        movement.removeModifier(MOVEMENT_MODIFIER_ID);

        if (amount != 0.0D) {
            movement.addTransientModifier(new AttributeModifier(
                    MOVEMENT_MODIFIER_ID,
                    "domesurvival.horror_mob_movement",
                    amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
}
