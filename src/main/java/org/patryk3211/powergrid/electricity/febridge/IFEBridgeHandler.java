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
package org.patryk3211.powergrid.electricity.febridge;

import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public interface IFEBridgeHandler {
    static float voltToFE() {
        return ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF();
    }

    static float wattToFE() {
        return ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF();
    }

    long getAmount();
    void setAmount(long amount);

    default void charge(SwitchedWire wire) {
        float wattsToFE = IFEBridgeHandler.wattToFE();
        if(wire.getState()) {
            var I = wire.current();
            setAmount(getAmount() + Math.round(I * I * wire.getResistance() * wattsToFE));
            setChanged();
        }
    }

    long moveEnergy();
    void setChanged();

    default void manageWire(SwitchedWire wire) {
        var V = Math.abs(wire.potentialDifference());
        long maxCharge = (long) (V * IFEBridgeHandler.voltToFE());
        long missingCharge = maxCharge - getAmount();
        if(missingCharge <= 0) {
            wire.setState(false);
            return;
        }

        float targetWatts = missingCharge / IFEBridgeHandler.wattToFE();
        float resistance = V * V / targetWatts;
        if(resistance > 0) {
            wire.setResistance(resistance);
            wire.setState(true);
        } else {
            wire.setState(false);
        }
    }
}
