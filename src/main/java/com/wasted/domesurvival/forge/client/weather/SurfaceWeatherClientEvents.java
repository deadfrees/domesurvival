package com.wasted.domesurvival.forge.client.weather;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.config.SurfaceHazardConfig;
import com.wasted.domesurvival.forge.particle.ModParticles;
import com.wasted.domesurvival.forge.sound.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Client presentation for the lethal surface weather.
 *
 * V3.4.4 deliberately avoids a mandatory shader pack. The weather remains compatible with large
 * Forge modpacks by using viewport fog, registered particles, resource textures and local sounds.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SurfaceWeatherClientEvents {
    private static final DomeBounds START_DOME = new DomeBounds(DomeSpec.wastedV1());
    private static final DomeSpec DOME = START_DOME.spec();

    private static final DustParticleOptions SOLAR_DUST =
            new DustParticleOptions(new Vector3f(1.00F, 0.54F, 0.10F), 0.20F);
    private static final ResourceLocation SOLAR_HEAT_VIGNETTE = new ResourceLocation(
            DomeSurvival.MOD_ID, "textures/gui/solar_heat_vignette.png"
    );

    private static final int RAIN_AMBIENCE_REPLAY_TICKS = 420;
    private static final int SAND_AMBIENCE_REPLAY_TICKS = 1800;

    private static int clientTicks;
    private static int ambienceCooldown;
    private static int thunderCooldown;
    private static SurfaceWeatherType lastAudioWeather = SurfaceWeatherType.CLEAR;
    private static boolean lastAudioExposed;

    // Eased client values prevent the hard one-frame visual jump when crossing the glass shell.
    private static float weatherPresenceBlend;
    private static float weatherExposureBlend;
    private static float solarPresenceBlend;
    private static float solarExposureBlend;

    private SurfaceWeatherClientEvents() {
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (weatherPresenceBlend > 0.01F && ClientSurfaceWeatherState.weatherActive()) {
            float exposure = weatherExposureBlend;
            float protectedStrength;
            float exposedStrength;
            float red;
            float green;
            float blue;

            switch (ClientSurfaceWeatherState.weather()) {
                case ACID_RAIN -> {
                    // V3.4.4: the dome protects from damage, not from seeing the toxic atmosphere.
                    // Use the same green cast inside and outside so crossing the glass does not
                    // switch the world palette. Fog density is handled separately in onRenderFog.
                    red = 0.12F;
                    green = 0.60F;
                    blue = 0.10F;
                    protectedStrength = 0.43F;
                    exposedStrength = 0.43F;
                }
                case ACID_THUNDERSTORM -> {
                    red = 0.06F;
                    green = 0.43F;
                    blue = 0.055F;
                    protectedStrength = 0.56F;
                    exposedStrength = 0.56F;
                }
                case SANDSTORM -> {
                    // The sandstorm colour grade is now identical on both sides of the dome glass.
                    // The dome still protects from damage and can retain a longer view distance.
                    red = 0.90F;
                    green = 0.49F;
                    blue = 0.12F;
                    protectedStrength = 0.88F;
                    exposedStrength = 0.88F;
                }
                case CLEAR -> {
                    return;
                }
                default -> {
                    return;
                }
            }

            float strength = weatherPresenceBlend * Mth.lerp(exposure, protectedStrength, exposedStrength);
            tintFog(event, red, green, blue, strength);
            return;
        }

        // The lethal sun remains visually warm even through the dome glass. Directly exposed players
        // receive the stronger desert heat haze and vignette, but the transition is eased.
        if (solarPresenceBlend > 0.01F) {
            float strength = solarPresenceBlend * Mth.lerp(solarExposureBlend, 0.20F, 0.34F);
            tintFog(event, 1.00F, 0.46F, 0.10F, strength);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE) {
            return;
        }

        float originalFar = event.getFarPlaneDistance();
        float targetFar = originalFar;
        float blend = 0.0F;

        if (weatherPresenceBlend > 0.01F && ClientSurfaceWeatherState.weatherActive()) {
            switch (ClientSurfaceWeatherState.weather()) {
                case ACID_RAIN -> {
                    // V3.4.4: clearly visible toxic haze. Keep the same fog distance inside and
                    // outside the dome so the glass does not visually cancel the weather.
                    targetFar = originalFar * 0.82F;
                    blend = weatherPresenceBlend;
                }
                case ACID_THUNDERSTORM -> {
                    // Thunder is deliberately heavier, but still leaves enough range to read the
                    // terrain and dome silhouette. Same distance on both sides of the glass.
                    targetFar = originalFar * 0.72F;
                    blend = weatherPresenceBlend;
                }
                case SANDSTORM -> {
                    float configuredFar = SurfaceHazardConfig.SANDSTORM_VISIBILITY.get().floatValue();
                    float exposedFar = Math.max(30.0F, configuredFar);
                    float protectedFar = Math.max(72.0F, exposedFar);
                    targetFar = Mth.lerp(weatherExposureBlend, protectedFar, exposedFar);
                    blend = weatherPresenceBlend;
                }
                case CLEAR -> {
                }
            }
        } else if (solarPresenceBlend > 0.01F) {
            // Existing worlds may still contain the old V3.3 value (112). Enforce a safer minimum
            // so the hot desert remains readable at long range while retaining a subtle heat haze.
            float configuredFar = SurfaceHazardConfig.SOLAR_VISIBILITY.get().floatValue();
            float exposedFar = Math.max(192.0F, configuredFar);
            float protectedFar = Math.max(224.0F, exposedFar);
            targetFar = Mth.lerp(solarExposureBlend, protectedFar, exposedFar);
            blend = solarPresenceBlend;
        }

        if (blend <= 0.01F) {
            return;
        }

        float clampedTarget = Math.min(originalFar, targetFar);
        float far = Mth.lerp(blend, originalFar, clampedTarget);
        if (far >= originalFar - 0.25F) {
            return;
        }

        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), Math.max(0.0F, far * 0.06F)));
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (weatherPresenceBlend > 0.01F && ClientSurfaceWeatherState.weatherActive()) {
            int protectedAlpha;
            int exposedAlpha;
            int red;
            int green;
            int blue;

            switch (ClientSurfaceWeatherState.weather()) {
                case ACID_RAIN -> {
                    protectedAlpha = 22;
                    exposedAlpha = 22;
                    red = 38;
                    green = 160;
                    blue = 26;
                }
                case ACID_THUNDERSTORM -> {
                    protectedAlpha = 32;
                    exposedAlpha = 32;
                    red = 24;
                    green = 126;
                    blue = 18;
                }
                case SANDSTORM -> {
                    protectedAlpha = 34;
                    exposedAlpha = 34;
                    red = 190;
                    green = 102;
                    blue = 25;
                }
                case CLEAR -> {
                    return;
                }
                default -> {
                    return;
                }
            }

            int alpha = Math.round(weatherPresenceBlend
                    * Mth.lerp(weatherExposureBlend, protectedAlpha, exposedAlpha));
            if (alpha > 0) {
                event.getGuiGraphics().fill(
                        0,
                        0,
                        event.getWindow().getGuiScaledWidth(),
                        event.getWindow().getGuiScaledHeight(),
                        argb(alpha, red, green, blue)
                );
            }
            return;
        }

        if (solarPresenceBlend > 0.01F) {
            // Keep a faint hot-air cast visible through the dome so a lethal clear day is readable
            // without exposing protected players to the full damage vignette.
            int heatAlpha = Math.round(solarPresenceBlend
                    * Mth.lerp(solarExposureBlend, 4.0F, 8.0F));
            if (heatAlpha > 0) {
                event.getGuiGraphics().fill(
                        0,
                        0,
                        event.getWindow().getGuiScaledWidth(),
                        event.getWindow().getGuiScaledHeight(),
                        argb(heatAlpha, 232, 116, 28)
                );
            }
            if (solarExposureBlend > 0.01F) {
                renderSolarVignette(event);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        clientTicks++;
        updateVisualBlends();

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            resetAudioState(minecraft);
            return;
        }

        updateWeatherAudio(minecraft, level, player);

        if (ClientSurfaceWeatherState.weatherActive()) {
            boolean insideDome = isInsideDome(player);
            boolean exposed = ClientSurfaceWeatherState.exposed();
            switch (ClientSurfaceWeatherState.weather()) {
                case ACID_RAIN -> spawnAcidRain(level, player, false, exposed, insideDome);
                case ACID_THUNDERSTORM -> spawnAcidRain(level, player, true, exposed, insideDome);
                case SANDSTORM -> spawnSand(level, player, exposed, insideDome);
                case CLEAR -> {
                }
            }
        } else if (ClientSurfaceWeatherState.solarActive()) {
            boolean insideDome = isInsideDome(player);
            if (ClientSurfaceWeatherState.solarExposed()) {
                spawnSolarHeatDust(level, player);
            } else if (insideDome) {
                // The player is protected, but the lethal clear-day surface must still be readable
                // through the glass. Render heat dust on the section of the outer shell currently
                // being viewed instead of putting the hazard effect inside the protected air.
                spawnViewedShellSolarHeat(level, player, 14);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        stopWeatherSounds(minecraft);
        ClientSurfaceWeatherState.clear();
        ambienceCooldown = 0;
        thunderCooldown = 0;
        lastAudioWeather = SurfaceWeatherType.CLEAR;
        lastAudioExposed = false;
        weatherPresenceBlend = 0.0F;
        weatherExposureBlend = 0.0F;
        solarPresenceBlend = 0.0F;
        solarExposureBlend = 0.0F;
    }

    private static void updateVisualBlends() {
        weatherPresenceBlend = approach(weatherPresenceBlend,
                ClientSurfaceWeatherState.weatherActive() ? 1.0F : 0.0F, 0.09F);
        weatherExposureBlend = approach(weatherExposureBlend,
                ClientSurfaceWeatherState.exposed() ? 1.0F : 0.0F, 0.075F);
        solarPresenceBlend = approach(solarPresenceBlend,
                ClientSurfaceWeatherState.solarActive() ? 1.0F : 0.0F, 0.07F);
        solarExposureBlend = approach(solarExposureBlend,
                ClientSurfaceWeatherState.solarExposed() ? 1.0F : 0.0F, 0.065F);
    }

    private static float approach(float current, float target, float rate) {
        if (Math.abs(target - current) < 0.002F) {
            return target;
        }
        return current + (target - current) * Mth.clamp(rate, 0.0F, 1.0F);
    }

    private static void tintFog(ViewportEvent.ComputeFogColor event,
                                float targetRed, float targetGreen, float targetBlue, float strength) {
        float blend = Mth.clamp(strength, 0.0F, 1.0F);
        event.setRed(Mth.lerp(blend, event.getRed(), targetRed));
        event.setGreen(Mth.lerp(blend, event.getGreen(), targetGreen));
        event.setBlue(Mth.lerp(blend, event.getBlue(), targetBlue));
    }

    /** Smooth texture-based heat vignette, closer to vanilla death/damage edge shading. */
    private static void renderSolarVignette(RenderGuiEvent.Pre event) {
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        float pulse = 0.5F + 0.5F * Mth.sin(clientTicks * 0.045F);
        float exposure = Mth.clamp(solarExposureBlend, 0.0F, 1.0F);
        float alpha = (0.34F + pulse * 0.12F) * exposure;
        if (alpha <= 0.005F) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        event.getGuiGraphics().blit(
                SOLAR_HEAT_VIGNETTE,
                0, 0, width, height,
                0.0F, 0.0F, 256, 256, 256, 256
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    private static void spawnAcidRain(ClientLevel level,
                                      LocalPlayer player,
                                      boolean thunder,
                                      boolean exposed,
                                      boolean insideDome) {
        if (!exposed && insideDome) {
            // Spawn a concentrated curtain on the exact section of the glass shell the player
            // is looking at. Randomly distributing a few particles around a 100-block dome made
            // them effectively invisible from the protected interior.
            spawnViewedShellAcidRain(level, player, thunder, thunder ? 44 : 32);
            return;
        }

        int count = thunder ? 34 : 24;
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (level.getRandom().nextDouble() - 0.5D) * 28.0D;
            double y = player.getY() + 7.0D + level.getRandom().nextDouble() * 14.0D;
            double z = player.getZ() + (level.getRandom().nextDouble() - 0.5D) * 28.0D;
            double driftX = (level.getRandom().nextDouble() - 0.5D) * (thunder ? 0.18D : 0.10D);
            double driftZ = (level.getRandom().nextDouble() - 0.5D) * (thunder ? 0.18D : 0.10D);
            double fall = -(1.25D + level.getRandom().nextDouble() * (thunder ? 0.90D : 0.62D));
            level.addAlwaysVisibleParticle(ModParticles.ACID_RAIN_STREAK.get(), x, y, z, driftX, fall, driftZ);
        }
    }

    private static void spawnViewedShellAcidRain(ClientLevel level,
                                                 LocalPlayer player,
                                                 boolean thunder,
                                                 int count) {
        ShellView shell = viewedShell(player);
        for (int i = 0; i < count; i++) {
            double lateral = (level.getRandom().nextDouble() - 0.5D) * 20.0D;
            double vertical = (level.getRandom().nextDouble() - 0.5D) * 14.0D;
            Vec3 surface = projectToShell(
                    shell.point().add(shell.tangentA().scale(lateral)).add(shell.tangentB().scale(vertical)),
                    shell.spherical()
            );
            Vec3 normal = shellNormal(surface, shell.spherical());
            Vec3 spawn = surface.add(normal.scale(1.0D)).add(0.0D, 3.0D + level.getRandom().nextDouble() * 9.0D, 0.0D);
            double fall = -(1.20D + level.getRandom().nextDouble() * (thunder ? 0.95D : 0.66D));
            level.addAlwaysVisibleParticle(
                    ModParticles.ACID_RAIN_STREAK.get(),
                    spawn.x, spawn.y, spawn.z,
                    (level.getRandom().nextDouble() - 0.5D) * 0.11D,
                    fall,
                    (level.getRandom().nextDouble() - 0.5D) * 0.11D
            );
        }
    }

    private static void spawnSand(ClientLevel level,
                                  LocalPlayer player,
                                  boolean exposed,
                                  boolean insideDome) {
        double windAngle = clientTicks * 0.010D + Math.sin(clientTicks * 0.0019D) * 0.62D;
        double windX = Math.cos(windAngle);
        double windZ = Math.sin(windAngle);
        double sideX = -windZ;
        double sideZ = windX;

        if (!exposed && insideDome) {
            spawnViewedShellSand(level, player, 38, windX, windZ, sideX, sideZ);
            return;
        }

        int count = exposed ? 42 : 30;
        for (int i = 0; i < count; i++) {
            double upstream = 6.0D + level.getRandom().nextDouble() * 22.0D;
            double side = (level.getRandom().nextDouble() - 0.5D) * 30.0D;
            double x = player.getX() - windX * upstream + sideX * side;
            double y = player.getY() - 2.0D + level.getRandom().nextDouble() * 12.0D;
            double z = player.getZ() - windZ * upstream + sideZ * side;
            double speed = 1.05D + level.getRandom().nextDouble() * 1.05D;
            double turbulence = (level.getRandom().nextDouble() - 0.5D) * 0.35D;
            level.addAlwaysVisibleParticle(
                    ModParticles.SANDSTORM_MOTE.get(),
                    x, y, z,
                    windX * speed + sideX * turbulence,
                    (level.getRandom().nextDouble() - 0.48D) * 0.18D,
                    windZ * speed + sideZ * turbulence
            );
        }
    }

    private static void spawnViewedShellSand(ClientLevel level,
                                             LocalPlayer player,
                                             int count,
                                             double windX,
                                             double windZ,
                                             double sideX,
                                             double sideZ) {
        ShellView shell = viewedShell(player);
        for (int i = 0; i < count; i++) {
            double lateral = (level.getRandom().nextDouble() - 0.5D) * 23.0D;
            double vertical = (level.getRandom().nextDouble() - 0.5D) * 17.0D;
            Vec3 surface = projectToShell(
                    shell.point().add(shell.tangentA().scale(lateral)).add(shell.tangentB().scale(vertical)),
                    shell.spherical()
            );
            Vec3 normal = shellNormal(surface, shell.spherical());
            Vec3 spawn = surface.add(normal.scale(1.15D));

            double speed = 1.05D + level.getRandom().nextDouble() * 1.15D;
            double turbulence = (level.getRandom().nextDouble() - 0.5D) * 0.42D;
            Vec3 velocity = new Vec3(
                    windX * speed + sideX * turbulence,
                    (level.getRandom().nextDouble() - 0.46D) * 0.17D,
                    windZ * speed + sideZ * turbulence
            );

            // Never launch the proxy particles through the glass into the protected dome.
            double inward = velocity.dot(normal);
            if (inward < 0.04D) {
                velocity = velocity.add(normal.scale(0.04D - inward));
            }

            level.addAlwaysVisibleParticle(
                    ModParticles.SANDSTORM_MOTE.get(),
                    spawn.x, spawn.y, spawn.z,
                    velocity.x, velocity.y, velocity.z
            );
        }
    }

    private static ShellView viewedShell(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 sphereCenter = new Vec3(DOME.centerX(), DOME.hemisphereCenterY(), DOME.centerZ());
        double radius = DOME.surfaceRadius();

        Vec3 m = eye.subtract(sphereCenter);
        double b = m.dot(look);
        double c = m.lengthSqr() - radius * radius;
        double discriminant = b * b - c;
        if (discriminant >= 0.0D) {
            double t = -b + Math.sqrt(discriminant);
            if (t > 0.0D) {
                Vec3 hit = eye.add(look.scale(t));
                if (hit.y >= DOME.hemisphereCenterY() - 0.35D) {
                    Vec3 normal = hit.subtract(sphereCenter).normalize();
                    return shellView(hit, normal, true);
                }
            }
        }

        double ox = eye.x - DOME.centerX();
        double oz = eye.z - DOME.centerZ();
        double dx = look.x;
        double dz = look.z;
        double a = dx * dx + dz * dz;
        if (a > 1.0E-6D) {
            double qb = ox * dx + oz * dz;
            double qc = ox * ox + oz * oz - radius * radius;
            double qd = qb * qb - a * qc;
            if (qd >= 0.0D) {
                double t = (-qb + Math.sqrt(qd)) / a;
                if (t > 0.0D) {
                    Vec3 hit = eye.add(look.scale(t));
                    if (hit.y >= DOME.baseY() - 0.5D && hit.y <= DOME.hemisphereCenterY() + 0.75D) {
                        Vec3 normal = new Vec3(hit.x - DOME.centerX(), 0.0D, hit.z - DOME.centerZ()).normalize();
                        return shellView(hit, normal, false);
                    }
                }
            }
        }

        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(eye.x - DOME.centerX(), 0.0D, eye.z - DOME.centerZ());
        }
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 normal = horizontal.normalize();
        Vec3 hit = new Vec3(
                DOME.centerX() + normal.x * radius,
                Mth.clamp(eye.y, DOME.baseY() + 0.25D, DOME.hemisphereCenterY() + 0.25D),
                DOME.centerZ() + normal.z * radius
        );
        return shellView(hit, normal, false);
    }

    private static ShellView shellView(Vec3 point, Vec3 normal, boolean spherical) {
        Vec3 reference = Math.abs(normal.y) > 0.90D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 tangentA = normal.cross(reference).normalize();
        Vec3 tangentB = normal.cross(tangentA).normalize();
        return new ShellView(point, normal, tangentA, tangentB, spherical);
    }

    private static Vec3 projectToShell(Vec3 candidate, boolean spherical) {
        if (spherical) {
            Vec3 center = new Vec3(DOME.centerX(), DOME.hemisphereCenterY(), DOME.centerZ());
            Vec3 relative = candidate.subtract(center);
            if (relative.lengthSqr() < 1.0E-6D) {
                relative = new Vec3(0.0D, 1.0D, 0.0D);
            }
            Vec3 projected = center.add(relative.normalize().scale(DOME.surfaceRadius()));
            if (projected.y >= DOME.hemisphereCenterY() - 0.25D) {
                return projected;
            }
        }

        double dx = candidate.x - DOME.centerX();
        double dz = candidate.z - DOME.centerZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-6D) {
            dx = 0.0D;
            dz = 1.0D;
            length = 1.0D;
        }
        double scale = DOME.surfaceRadius() / length;
        return new Vec3(
                DOME.centerX() + dx * scale,
                Mth.clamp(candidate.y, DOME.baseY() + 0.15D, DOME.hemisphereCenterY() + 0.35D),
                DOME.centerZ() + dz * scale
        );
    }

    private static Vec3 shellNormal(Vec3 surface, boolean spherical) {
        if (spherical && surface.y >= DOME.hemisphereCenterY() - 0.25D) {
            return surface.subtract(new Vec3(DOME.centerX(), DOME.hemisphereCenterY(), DOME.centerZ())).normalize();
        }
        return new Vec3(surface.x - DOME.centerX(), 0.0D, surface.z - DOME.centerZ()).normalize();
    }

    private record ShellView(
            Vec3 point,
            Vec3 normal,
            Vec3 tangentA,
            Vec3 tangentB,
            boolean spherical
    ) {
    }

    private static void spawnViewedShellSolarHeat(ClientLevel level, LocalPlayer player, int count) {
        // Run every other client tick: the effect should make the exterior look hostile through
        // the glass without turning the protected interior into a particle cloud.
        if ((clientTicks & 1) != 0) {
            return;
        }

        ShellView shell = viewedShell(player);
        for (int i = 0; i < count; i++) {
            double lateral = (level.getRandom().nextDouble() - 0.5D) * 24.0D;
            double vertical = (level.getRandom().nextDouble() - 0.5D) * 16.0D;
            Vec3 surface = projectToShell(
                    shell.point().add(shell.tangentA().scale(lateral)).add(shell.tangentB().scale(vertical)),
                    shell.spherical()
            );
            Vec3 normal = shellNormal(surface, shell.spherical());
            Vec3 spawn = surface.add(normal.scale(1.15D));

            level.addAlwaysVisibleParticle(
                    SOLAR_DUST,
                    spawn.x, spawn.y, spawn.z,
                    normal.x * 0.018D + (level.getRandom().nextDouble() - 0.5D) * 0.028D,
                    0.025D + level.getRandom().nextDouble() * 0.055D,
                    normal.z * 0.018D + (level.getRandom().nextDouble() - 0.5D) * 0.028D
            );
        }
    }

    private static void spawnSolarHeatDust(ClientLevel level, LocalPlayer player) {
        if ((clientTicks & 1) != 0) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            double x = player.getX() + (level.getRandom().nextDouble() - 0.5D) * 20.0D;
            double y = player.getY() - 0.8D + level.getRandom().nextDouble() * 5.2D;
            double z = player.getZ() + (level.getRandom().nextDouble() - 0.5D) * 20.0D;
            level.addParticle(
                    SOLAR_DUST,
                    x, y, z,
                    (level.getRandom().nextDouble() - 0.5D) * 0.035D,
                    0.035D + level.getRandom().nextDouble() * 0.055D,
                    (level.getRandom().nextDouble() - 0.5D) * 0.035D
            );
        }
    }

    private static void updateWeatherAudio(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
        SurfaceWeatherType weather = ClientSurfaceWeatherState.weather();
        boolean exposed = ClientSurfaceWeatherState.exposed();

        if (weather != lastAudioWeather || exposed != lastAudioExposed) {
            stopWeatherSounds(minecraft);
            ambienceCooldown = 0;
            thunderCooldown = 0;
            lastAudioWeather = weather;
            lastAudioExposed = exposed;
        }

        if (weather == SurfaceWeatherType.CLEAR) {
            return;
        }

        if (ambienceCooldown > 0) {
            ambienceCooldown--;
        }

        switch (weather) {
            case ACID_RAIN, ACID_THUNDERSTORM -> {
                if (ambienceCooldown <= 0) {
                    float volume = exposed
                            ? (weather == SurfaceWeatherType.ACID_THUNDERSTORM ? 0.66F : 0.52F)
                            : 0.22F;
                    level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            ModSounds.ACID_RAIN_AMBIENCE.get(),
                            SoundSource.WEATHER,
                            volume,
                            weather == SurfaceWeatherType.ACID_THUNDERSTORM ? 0.94F : 1.0F,
                            false
                    );
                    ambienceCooldown = RAIN_AMBIENCE_REPLAY_TICKS;
                }

                if (weather == SurfaceWeatherType.ACID_THUNDERSTORM) {
                    playThunderRumble(level, player, exposed);
                } else {
                    thunderCooldown = 0;
                }
            }
            case SANDSTORM -> {
                if (ambienceCooldown <= 0) {
                    level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            ModSounds.SANDSTORM_WIND.get(),
                            SoundSource.WEATHER,
                            exposed ? 0.82F : 0.27F,
                            0.94F + level.getRandom().nextFloat() * 0.08F,
                            false
                    );
                    ambienceCooldown = SAND_AMBIENCE_REPLAY_TICKS;
                }
                thunderCooldown = 0;
            }
            case CLEAR -> {
            }
        }
    }

    private static void playThunderRumble(ClientLevel level, LocalPlayer player, boolean exposed) {
        if (thunderCooldown > 0) {
            thunderCooldown--;
            return;
        }

        thunderCooldown = 120 + level.getRandom().nextInt(241);
        level.playLocalSound(
                player.getX(), player.getY() + 8.0D, player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                exposed ? 0.78F : 0.28F,
                0.72F + level.getRandom().nextFloat() * 0.18F,
                false
        );
    }

    private static void resetAudioState(Minecraft minecraft) {
        stopWeatherSounds(minecraft);
        ambienceCooldown = 0;
        thunderCooldown = 0;
        lastAudioWeather = SurfaceWeatherType.CLEAR;
        lastAudioExposed = false;
    }

    private static void stopWeatherSounds(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().stop(ModSounds.ACID_RAIN_AMBIENCE.get().getLocation(), SoundSource.WEATHER);
        minecraft.getSoundManager().stop(ModSounds.SANDSTORM_WIND.get().getLocation(), SoundSource.WEATHER);
    }

    private static boolean isInsideDome(LocalPlayer player) {
        return START_DOME.classify(player.getX(), player.getY(), player.getZ()) != DomeZone.OUTSIDE;
    }
}
