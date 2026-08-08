package com.wasted.domesurvival.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-authoritative tuning for surface hazards and custom weather scheduling. */
public final class SurfaceHazardConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue DAMAGE_INTERVAL_TICKS;

    public static final ForgeConfigSpec.BooleanValue SOLAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SOLAR_DAMAGE;
    public static final ForgeConfigSpec.IntValue SOLAR_VISIBILITY;

    public static final ForgeConfigSpec.BooleanValue ACID_RAIN_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ACID_RAIN_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ACID_THUNDER_DAMAGE;
    public static final ForgeConfigSpec.IntValue ACID_RAIN_VISIBILITY;
    public static final ForgeConfigSpec.IntValue ACID_THUNDER_VISIBILITY;

    public static final ForgeConfigSpec.BooleanValue SANDSTORM_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SANDSTORM_DAMAGE;
    public static final ForgeConfigSpec.IntValue SANDSTORM_MIN_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue SANDSTORM_MAX_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue SANDSTORM_MIN_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue SANDSTORM_MAX_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue SANDSTORM_VISIBILITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("surface_hazards");

        DAMAGE_INTERVAL_TICKS = builder
                .comment(
                        "How often exposed players receive surface-hazard damage, in ticks.",
                        "20 ticks = 1 second. Default 10 means two hazard hits per second.",
                        "Do not set below 10: vanilla LivingEntity hurt cooldown would make faster hurt() calls unreliable."
                )
                .defineInRange("damage_interval_ticks", 10, 10, 200);

        builder.push("solar");
        SOLAR_ENABLED = builder
                .comment("Whether direct daytime sky exposure outside the dome causes solar damage.")
                .define("enabled", true);
        SOLAR_DAMAGE = builder
                .comment("Solar damage per hit, in health points. 2.0 = one heart. Frequency is controlled separately.")
                .defineInRange("damage", 2.0D, 0.0D, 100.0D);
        SOLAR_VISIBILITY = builder
                .comment("Maximum client fog distance during lethal clear-sky solar exposure, in blocks.")
                .defineInRange("visibility", 192, 32, 512);
        builder.pop();

        builder.push("acid_rain");
        ACID_RAIN_ENABLED = builder
                .comment("Whether vanilla overworld rain is treated as hazardous acid rain.")
                .define("enabled", true);
        ACID_RAIN_DAMAGE = builder
                .comment("Acid rain damage per hit. 1.0 = half a heart. Frequency is controlled separately.")
                .defineInRange("damage", 1.0D, 0.0D, 100.0D);
        ACID_THUNDER_DAMAGE = builder
                .comment("Acid rain damage per hit during thunderstorms. Frequency is controlled separately.")
                .defineInRange("thunder_damage", 2.0D, 0.0D, 100.0D);
        ACID_RAIN_VISIBILITY = builder
                .comment("Legacy compatibility setting retained for old worlds. V3.4.3 uses a mild proportional fog instead of this fixed value.")
                .defineInRange("visibility", 144, 24, 512);
        ACID_THUNDER_VISIBILITY = builder
                .comment("Legacy compatibility setting retained for old worlds. V3.4.3 uses a mild proportional fog instead of this fixed value.")
                .defineInRange("thunder_visibility", 112, 16, 512);
        builder.pop();

        builder.push("sandstorm");
        SANDSTORM_ENABLED = builder
                .comment("Whether rare custom sandstorms can occur in the overworld.")
                .define("enabled", true);
        SANDSTORM_DAMAGE = builder
                .comment("Sandstorm damage per hit while directly exposed outdoors. Frequency is controlled separately.")
                .defineInRange("damage", 1.0D, 0.0D, 100.0D);
        SANDSTORM_MIN_DURATION_SECONDS = builder
                .comment("Minimum random sandstorm duration in real-time seconds.")
                .defineInRange("min_duration_seconds", 120, 10, 7200);
        SANDSTORM_MAX_DURATION_SECONDS = builder
                .comment("Maximum random sandstorm duration in real-time seconds.")
                .defineInRange("max_duration_seconds", 300, 10, 7200);
        SANDSTORM_MIN_INTERVAL_SECONDS = builder
                .comment("Minimum clear interval before the next random sandstorm can start.")
                .defineInRange("min_interval_seconds", 900, 30, 86400);
        SANDSTORM_MAX_INTERVAL_SECONDS = builder
                .comment("Maximum clear interval before the next random sandstorm can start.")
                .defineInRange("max_interval_seconds", 2400, 30, 86400);
        SANDSTORM_VISIBILITY = builder
                .comment("Maximum client fog distance during an exposed sandstorm, in blocks.")
                .defineInRange("visibility", 24, 6, 256);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private SurfaceHazardConfig() {
    }
}
