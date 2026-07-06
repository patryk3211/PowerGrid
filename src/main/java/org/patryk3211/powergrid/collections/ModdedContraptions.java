package org.patryk3211.powergrid.collections;


import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateRegistries;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBearingContraption;

public class ModdedContraptions {
    public static final DeferredRegister<ContraptionType> REGISTER = DeferredRegister.create(PowerGrid.MOD_ID, CreateRegistries.CONTRAPTION_TYPE);

    public static final RegistrySupplier<ContraptionType> SOLAR_PANEL = REGISTER.register("solar_panel", () -> new ContraptionType(SolarPanelBearingContraption::new));

    public static void register() {
        REGISTER.register();
    }
}
