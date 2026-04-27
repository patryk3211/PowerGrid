/*
 * Copyright 2025 patryk3211
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
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.List;

public class ConnectorComponent extends Component {
    private static final List<TerminalBoundingBox> TERMINALS = List.of(
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 1f, 1.0f, 1f, 2f, 2.0f, 2f)
    );

    public ConnectorComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LABEL);
    }

    @Override
    public boolean emitExternalTerminals() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed) {
        if(placed.customData instanceof List) {
            return (List<TerminalBoundingBox>) placed.customData;
        } else {
            var label = placed.get(LABEL);
            if(label.isEmpty()) {
                placed.customData = TERMINALS;
                return TERMINALS;
            } else {
                var list = List.of(new TerminalBoundingBox(
                        net.minecraft.network.chat.Component.literal(label),
                        1, 1, 1, 2, 2, 2
                ));
                placed.customData = list;
                return list;
            }
        }
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
    }
}
