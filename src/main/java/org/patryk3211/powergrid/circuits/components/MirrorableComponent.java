package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public abstract class MirrorableComponent extends OrientableComponent {
    public static final BooleanProperty MIRRORED = new BooleanProperty(PowerGrid.MOD_ID, "mirrored").hidden().cast();

    protected final ComponentFootprint mirroredFootprint;

    public MirrorableComponent(ComponentFootprint footprint) {
        this(footprint, footprint.mirroredX());
    }

    public MirrorableComponent(ComponentFootprint footprint, ComponentFootprint mirrored) {
        super(footprint);
        mirroredFootprint = mirrored;
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(MIRRORED);
    }

    @Override
    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        if(placed != null && placed.get(MIRRORED)) {
            return mirroredFootprint.rotated(placed.get(ORIENTATION));
        }
        return super.footprint(placed);
    }

    @Override
    public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
        if(!counterClockwise) {
            if (!placed.get(MIRRORED)) {
                placed.set(MIRRORED, true);
                return true;
            }
            placed.set(MIRRORED, false);
        } else {
            if(placed.get(MIRRORED)) {
                placed.set(MIRRORED, false);
                return true;
            }
            placed.set(MIRRORED, true);
        }
        return super.rotate(placed, counterClockwise);
    }
}
