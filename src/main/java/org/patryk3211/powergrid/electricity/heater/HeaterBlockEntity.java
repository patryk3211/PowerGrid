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
package org.patryk3211.powergrid.electricity.heater;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Collection;
import java.util.List;

public class HeaterBlockEntity extends ElectricBlockEntity {
    public enum State {
        COLD,
        SMOKING,
        BLASTING
    }

    private IElectricNode node1;
    private IElectricNode node2;
    private State state;

    public HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = State.COLD;
    }

    @Override
    public void tick() {
        super.tick();
        float voltage = Math.abs(node1.getVoltage() - node2.getVoltage());
        float power = voltage * voltage / ModdedConfigs.server().electricity.heaterResistance.getF();
        if(power < ModdedConfigs.server().electricity.heaterSmokingPower.get()) {
            updateState(State.COLD);
        } else if(power < ModdedConfigs.server().electricity.heaterBlastingPower.get()) {
            updateState(State.SMOKING);
        } else {
            updateState(State.BLASTING);
        }
    }

    private void updateState(State state) {
        assert world != null;
        if(this.state != state) {
            this.state = state;
            world.updateNeighbors(pos, getCachedState().getBlock());
        }
    }

    public State getState() {
        return state;
    }

    @Override
    public void initializeNodes() {
        node1 = new FloatingNode();
        node2 = new FloatingNode();
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(node1);
        nodes.add(node2);
    }

    @Override
    public void addInternalWires(Collection<ElectricWire> wires) {
        wires.add(new ElectricWire(HeaterBlock.resistance(), node1, node2));
    }
}
