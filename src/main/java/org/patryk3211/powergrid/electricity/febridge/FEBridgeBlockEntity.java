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

import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class FEBridgeBlockEntity extends ElectricBlockEntity {
    private int charge;
    private SwitchedWire wire;

    public FEBridgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static float voltToFE() {
        return ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF();
    }

    public static float ampToFE() {
        return ModdedConfigs.server().electricity.forgeEnergyPerAmp.getF();
    }

    @Override
    public void tick() {
        super.tick();

        float ampToFe = ampToFE();
        if(wire.getState()) {
            charge += Math.round(wire.current() * ampToFe);
        }

        int maxCharge = (int) (wire.potentialDifference() * voltToFE());
        int missingCharge = maxCharge - charge;
        if(missingCharge <= 0) {
            wire.setState(false);
            return;
        }

        float targetAmps = missingCharge / ampToFe;
        float resistance = wire.potentialDifference() / targetAmps;
        wire.setResistance(resistance);
        wire.setState(true);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1));
    }
}

