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

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class FEBridgeBlockEntity extends ElectricBlockEntity {
    private int charge;
    private SwitchedWire wire;

    public FEBridgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static final float VOLT_TO_FE = 2;
    private static final float AMP_TO_FE = 10f;

    @Override
    public void tick() {
        super.tick();

        if(wire.getState()) {
            charge += Math.round(wire.current() * AMP_TO_FE);
        }

        // TODO: FE bridge needs Volt to FE, as well as Amp to FE ratios.
        int maxCharge = (int) (wire.potentialDifference() * VOLT_TO_FE);
        int missingCharge = maxCharge - charge;
        if(missingCharge <= 0) {
            wire.setState(false);
            return;
        }

        float targetAmps = missingCharge / AMP_TO_FE;
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

