package org.patryk3211.powergrid.circuits.components;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;

public class Via2Component extends Component {
    private static final ComponentFootprint NODED_FOOTPRINT = new ComponentFootprint.Builder(1, 1)
            .addPad(0, 0, 0).build();

    public Via2Component(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        if(placed != null) {
            return NODED_FOOTPRINT;
        }
        return super.footprint(placed);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {

    }
}