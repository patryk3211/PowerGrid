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
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public interface IRedstoneComponent {
    default boolean isEmitter() {
        return false;
    }

    default boolean isReceiver() {
        return false;
    }

    default void receiveRedstone(@NotNull PlacedComponent component, int level) {

    }

    default int getEmittedLevel(@NotNull PlacedComponent component) {
        return 0;
    }

    static void notifyNeighbours(@NotNull PlacedComponent component) {
        var state = component.getWorld().getBlockState(component.getPos());
        var circuit = (CircuitBoardBlock) state.getBlock();
        if(component.has(Orientation.PROPERTY)) {
            var updateDir = circuit.getDirection(state, component.get(Orientation.PROPERTY));
            var updatePos = component.getPos().offset(updateDir);
            component.getWorld().updateNeighbor(updatePos, circuit, component.getPos());
        } else {
            component.getWorld().updateNeighbors(component.getPos(), circuit);
        }
    }
}
