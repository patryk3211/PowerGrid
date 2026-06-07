package org.patryk3211.powergrid.collections;


import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBearingContraption;

public class ModdedContraptions {
    public static Holder.Reference<ContraptionType> SOLAR_PANEL;

    public static void register() {
        SOLAR_PANEL = Registry.registerForHolder(
                CreateBuiltInRegistries.CONTRAPTION_TYPE,
                new ResourceLocation("powergrid", "solar_panel_bearing"),
                new ContraptionType(SolarPanelBearingContraption::new)
        );
    }
}
