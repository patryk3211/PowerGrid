package org.patryk3211.powergrid.collections;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.Registry;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBearingContraption;

public class ModdedContraptions {
    public static ContraptionType SOLAR_PANEL;

    public static void register() {
        SOLAR_PANEL = Registry.register(
                CreateBuiltInRegistries.CONTRAPTION_TYPE,
                PowerGrid.asResource("solar_panel"),
                new ContraptionType(SolarPanelBearingContraption::new)
        );
    }
}
