package com.wasted.domesurvival.forge.fluid;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

/**
 * DomeSurvival process fluids.
 *
 * <p>Purified water is a machine-process fluid. This revision supplies explicit client
 * textures so HUD mods such as Jade can query a valid sprite without crashing.</p>
 */
public final class ModFluids {
    private static final ResourceLocation WATER_STILL = new ResourceLocation("block/water_still");
    private static final ResourceLocation WATER_FLOW = new ResourceLocation("block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = new ResourceLocation("block/water_overlay");
    private static final int PURIFIED_TINT = 0xFF72D4DB;

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, DomeSurvival.MOD_ID);

    public static final RegistryObject<FluidType> PURIFIED_WATER_TYPE = FLUID_TYPES.register(
            "purified_water",
            () -> new PurifiedWaterFluidType(FluidType.Properties.create()
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300))
    );

    public static final RegistryObject<FlowingFluid> PURIFIED_WATER = FLUIDS.register(
            "purified_water",
            () -> new ForgeFlowingFluid.Source(purifiedWaterProperties())
    );

    public static final RegistryObject<FlowingFluid> FLOWING_PURIFIED_WATER = FLUIDS.register(
            "flowing_purified_water",
            () -> new ForgeFlowingFluid.Flowing(purifiedWaterProperties())
    );

    private static ForgeFlowingFluid.Properties purifiedWaterProperties() {
        return new ForgeFlowingFluid.Properties(
                PURIFIED_WATER_TYPE,
                PURIFIED_WATER,
                FLOWING_PURIFIED_WATER
        ).slopeFindDistance(4).levelDecreasePerBlock(1);
    }

    private ModFluids() {
    }

    private static final class PurifiedWaterFluidType extends FluidType {
        private PurifiedWaterFluidType(Properties properties) {
            super(properties);
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return WATER_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return WATER_FLOW;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return WATER_OVERLAY;
                }

                @Override
                public int getTintColor() {
                    return PURIFIED_TINT;
                }
            });
        }
    }
}
