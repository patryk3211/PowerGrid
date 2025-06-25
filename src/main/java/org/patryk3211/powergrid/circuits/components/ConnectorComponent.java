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

import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.List;

public class ConnectorComponent extends Component {
    private static final List<TerminalBoundingBox> TERMINALS = List.of(
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0.5f, 1.0f, 0.5f, 1.5f, 2.0f, 1.5f)
    );

    public ConnectorComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    public float getPadResistance(int padIndex) {
        return 0.01f;
    }

    @Override
    public boolean emitExternalTerminals() {
        return true;
    }

    @Override
    public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed) {
        return TERMINALS;
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder) {
    }
}
