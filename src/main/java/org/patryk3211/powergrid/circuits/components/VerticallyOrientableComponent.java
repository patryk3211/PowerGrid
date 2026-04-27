/*
 * Copyright 2026 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.Collection;
import java.util.List;

public abstract class VerticallyOrientableComponent extends OrientableComponent {
    public static final BooleanProperty VERTICAL = new BooleanProperty(PowerGrid.MOD_ID, "vertical").hidden().cast();

    private final ComponentFootprint verticalFootprint;

    public VerticallyOrientableComponent(ComponentFootprint footprint, ComponentFootprint verticalFootprint) {
        super(footprint);
        this.verticalFootprint = verticalFootprint;
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(VERTICAL);
    }

    @Override
    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        if(placed != null && placed.get(VERTICAL)) {
            return verticalFootprint.rotated(placed.get(ORIENTATION));
        }
        return super.footprint(placed);
    }

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        var id = ComponentRegistry.getId(this);
        return component.get(VERTICAL) ? id.withSuffix("_vertical") : id;
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        var id = ComponentRegistry.getId(this);
        return List.of(id, id.withSuffix("_vertical"));
    }

    @Override
    public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
        if(!counterClockwise) {
            if (!placed.get(VERTICAL)) {
                placed.set(VERTICAL, true);
                return true;
            }
            placed.set(VERTICAL, false);
        } else {
            if(placed.get(VERTICAL)) {
                placed.set(VERTICAL, false);
                return true;
            }
            placed.set(VERTICAL, true);
        }
        return super.rotate(placed, counterClockwise);
    }
}
