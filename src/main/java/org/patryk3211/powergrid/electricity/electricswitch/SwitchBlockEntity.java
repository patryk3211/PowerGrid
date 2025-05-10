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
package org.patryk3211.powergrid.electricity.electricswitch;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class SwitchBlockEntity extends ElectricBlockEntity {
    private SwitchedWire wire;

    public SwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 0.5f, 0.1f);
    }

    @Override
    public void tick() {
        super.tick();
        var current = wire.current();
        applyLostPower(current * current * wire.getResistance());
    }

    public void setState(boolean state) {
        wire.setState(state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        var node1 = builder.addExternalNode();
        var node2 = builder.addExternalNode();
        wire = builder.connectSwitch(0.01f, node1, node2, !getCachedState().get(SwitchBlock.OPEN));
    }
}
