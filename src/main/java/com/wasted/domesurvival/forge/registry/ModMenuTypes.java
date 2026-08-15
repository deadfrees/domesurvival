package com.wasted.domesurvival.forge.registry;

import com.wasted.domesurvival.forge.machine.energy.CreativeEnergyBufferMenu;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferMenu;
import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferMenu;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferMenu;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorMenu;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierMenu;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerMenu;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<MenuType<CoalGeneratorMenu>> COAL_GENERATOR =
            MENU_TYPES.register("coal_generator", () -> IForgeMenuType.create(CoalGeneratorMenu::new));

    public static final RegistryObject<MenuType<WaterPurifierMenu>> WATER_PURIFIER =
            MENU_TYPES.register("water_purifier", () -> IForgeMenuType.create(WaterPurifierMenu::new));

    public static final RegistryObject<MenuType<OxygenElectrolyzerMenu>> OXYGEN_ELECTROLYZER =
            MENU_TYPES.register("oxygen_electrolyzer", () -> IForgeMenuType.create(OxygenElectrolyzerMenu::new));

    public static final RegistryObject<MenuType<OxygenFillerMenu>> OXYGEN_FILLER =
            MENU_TYPES.register("oxygen_filler", () -> IForgeMenuType.create(OxygenFillerMenu::new));

    public static final RegistryObject<MenuType<EnergyBufferMenu>> ENERGY_BUFFER =
            MENU_TYPES.register("energy_buffer", () -> IForgeMenuType.create(EnergyBufferMenu::new));

    public static final RegistryObject<MenuType<TitanEnergyBufferMenu>> ENERGY_BUFFER_TITAN =
            MENU_TYPES.register("energy_buffer_titan", () -> IForgeMenuType.create(TitanEnergyBufferMenu::new));

    public static final RegistryObject<MenuType<AdamantiumEnergyBufferMenu>> ENERGY_BUFFER_ADAMANTIUM =
            MENU_TYPES.register("energy_buffer_adamantium", () -> IForgeMenuType.create(AdamantiumEnergyBufferMenu::new));

    public static final RegistryObject<MenuType<CreativeEnergyBufferMenu>> ENERGY_BUFFER_CREATIVE =
            MENU_TYPES.register("energy_buffer_creative", () -> IForgeMenuType.create(CreativeEnergyBufferMenu::new));

    private ModMenuTypes() {
    }
}
