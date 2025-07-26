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
package org.patryk3211.powergrid.electricity.deviceconnector;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Optional;

public class ProxyElectricBehaviour extends ElectricBehaviour {
    private final BlockPos behaviourPosition;
    private ElectricBehaviour mainBehaviour;

    public <T extends SmartBlockEntity & IElectricEntity> ProxyElectricBehaviour(T be, BlockPos behaviourPosition) {
        super(be);
        this.behaviourPosition = behaviourPosition;
    }

    public Optional<ElectricBehaviour> getMainBehaviour() {
        if(mainBehaviour != null)
            return Optional.of(mainBehaviour);
        var world = getWorld();
        mainBehaviour = get(world, behaviourPosition, TYPE);
        if(mainBehaviour == null)
            world.breakBlock(getPos(), true);
        return Optional.ofNullable(mainBehaviour);
    }

    @Override
    public void joinNetwork(ElectricalNetwork network) {
        getMainBehaviour().ifPresent(b -> b.joinNetwork(network));
    }

    @Override
    public @Nullable IElectricNode getTerminal(int index) {
        return getMainBehaviour().map(b -> b.getTerminal(index)).orElse(null);
    }

//    @Override
//    public boolean hasConnection(BlockWireEndpoint source, BlockWireEndpoint destination) {
//        return super.hasConnection(source, destination);
//    }
//
//    @Override
//    public Map<BlockWireEndpoint, List<WireEntity>> getConnections() {
//        return super.getConnections();
//    }

    @Override
    public boolean hasTerminal(int terminal) {
        return getMainBehaviour().map(b -> b.hasTerminal(terminal)).orElse(false);
    }

//    @Override
//    public void breakConnections() {
//        super.breakConnections();
//    }

//    @Override
//    public void addConnection(BlockWireEndpoint endpoint, WireEntity wire) {
//        super.addConnection(endpoint, wire);
//        mainBehaviour.addConnection(endpoint, wire);
//    }
//
//    @Override
//    public void removeConnection(BlockWireEndpoint endpoint, WireEntity wire) {
//        super.removeConnection(endpoint, wire);
//        mainBehaviour.removeConnection(endpoint, wire);
//    }

//    @Override
//    public void refreshConnectionEntities() {
//        super.refreshConnectionEntities();
//    }
}
