package com.wasted.domesurvival.forge.machine.module;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Built-in logical module definitions used by all DomeSurvival modular machines.
 *
 * <p>This is deliberately not a Forge registry. Physical module items will resolve
 * to these stable logical IDs, while machines only depend on the logical API.</p>
 */
public final class MachineModuleCatalog {
    public static final MachineModule EFFICIENCY = new MachineModule(
            id("efficiency"),
            MachineModuleType.EFFICIENCY,
            Set.of(MachineModuleType.OVERDRIVE),
            new MachineModuleModifiers(90, 80, 100, false, false, false)
    );

    public static final MachineModule OVERDRIVE = new MachineModule(
            id("overdrive"),
            MachineModuleType.OVERDRIVE,
            Set.of(MachineModuleType.EFFICIENCY),
            new MachineModuleModifiers(135, 160, 100, false, false, false)
    );

    public static final MachineModule BUFFER = new MachineModule(
            id("buffer"),
            MachineModuleType.BUFFER,
            Set.of(),
            new MachineModuleModifiers(100, 100, 175, false, false, false)
    );

    public static final MachineModule AUTOMATION = new MachineModule(
            id("automation"),
            MachineModuleType.AUTOMATION,
            Set.of(),
            new MachineModuleModifiers(100, 100, 100, true, false, false)
    );

    public static final MachineModule EMERGENCY_PROTECTION = new MachineModule(
            id("emergency_protection"),
            MachineModuleType.EMERGENCY_PROTECTION,
            Set.of(),
            new MachineModuleModifiers(100, 100, 100, false, true, false)
    );

    public static final MachineModule COMMUNICATION = new MachineModule(
            id("communication"),
            MachineModuleType.COMMUNICATION,
            Set.of(),
            new MachineModuleModifiers(100, 100, 100, false, false, true)
    );

    private static final Map<ResourceLocation, MachineModule> BY_ID;

    static {
        Map<ResourceLocation, MachineModule> modules = new LinkedHashMap<>();
        register(modules, EFFICIENCY);
        register(modules, OVERDRIVE);
        register(modules, BUFFER);
        register(modules, AUTOMATION);
        register(modules, EMERGENCY_PROTECTION);
        register(modules, COMMUNICATION);
        BY_ID = Collections.unmodifiableMap(modules);
    }

    private MachineModuleCatalog() {
    }

    public static Collection<MachineModule> all() {
        return BY_ID.values();
    }

    @Nullable
    public static MachineModule byId(ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }

    private static void register(Map<ResourceLocation, MachineModule> modules, MachineModule module) {
        MachineModule previous = modules.put(module.id(), module);
        if (previous != null) {
            throw new IllegalStateException("Duplicate machine module id: " + module.id());
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, path);
    }
}
