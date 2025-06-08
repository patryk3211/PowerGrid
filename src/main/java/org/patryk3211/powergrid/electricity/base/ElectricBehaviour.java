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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ElectricBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ElectricBehaviour> TYPE = new BehaviourType<>();

    private final IElectricEntity element;

    private final List<INode> internalNodes = new LinkedList<>();
    private final List<IElectricNode> externalNodes = new LinkedList<>();
    private final List<ElectricWire> internalWires = new LinkedList<>();

    private final List<List<WireEntity>> connections;
    private boolean destroying = false;

    public <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be) {
        super(be);
        this.element = be;

        var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
        element.buildCircuit(builder);

        connections = new ArrayList<>();
        for(int i = 0; i < externalNodes.size(); ++i)
            connections.add(new ArrayList<>());
    }

    public void joinNetwork(ElectricalNetwork network) {
        if(externalNodes.isEmpty())
            throw new IllegalStateException("Cannot join a network if no external nodes are defined");
        if(externalNodes.get(0).getNetwork() == null) {
            internalNodes.forEach(network::addNode);
            externalNodes.forEach(network::addNode);
            internalWires.forEach(network::addWire);
        }
    }

    public void rebuildCircuit() {
        var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
        if(!externalNodes.isEmpty()) {
            builder.with(externalNodes.get(0).getNetwork());
        }
        builder.alterExternal(false);
        builder.clear();
        element.buildCircuit(builder);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void unload() {
        var world = getWorld();
        for(var terminalConnections : connections) {
            for(var entity : terminalConnections) {
                entity.dropWire();
            }
        }
        internalWires.forEach(ElectricWire::remove);
        if(externalNodes.isEmpty())
            return;

        // Since every node has to have the same network we can
        // just take the network of the first external node and
        // assume that every other node belongs to it.
        var firstExternal = externalNodes.get(0);
        if(firstExternal != null) {
            var network = firstExternal.getNetwork();
            if (network != null) {
                externalNodes.forEach(network::removeNode);
                internalNodes.forEach(network::removeNode);
            }
        }
    }

    public void refreshConnectionEntities() {
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            for(var entity : connections.get(sourceTerminal)) {
                if(entity instanceof HangingWireEntity wire)
                    wire.refreshTerminalPositions();
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    public void addConnection(int sourceTerminal, WireEntity entity) {
        connections.get(sourceTerminal).add(entity);
        blockEntity.notifyUpdate();
    }

    public void removeConnection(int sourceTerminal, WireEntity entity) {
        var sourceConnections = connections.get(sourceTerminal);
        sourceConnections.remove(entity);
        blockEntity.notifyUpdate();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public IElectricNode getTerminal(int index) {
        if(index >= externalNodes.size())
            return null;
        return externalNodes.get(index);
    }

    public boolean hasConnection(int sourceTerminal, BlockWireEndpoint endpoint) {
        var sourceConnections = connections.get(sourceTerminal);
        for(var entity : sourceConnections) {
            if(entity.isConnectedTo(endpoint.getPos(), endpoint.getTerminal()))
                return true;
        }
        return false;
    }

    public void breakConnections() {
        if(destroying)
            return;
        destroying = true;
        var world = getWorld();
        if(!world.isClient) {
            for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
                var sourceConnections = connections.get(sourceTerminal);
                var endpoint = new BlockWireEndpoint(getPos(), sourceTerminal);
                for(var entity : sourceConnections) {
                    entity.endpointRemoved(endpoint);
                }
                sourceConnections.clear();
            }
        }
        blockEntity.notifyUpdate();
        destroying = false;
    }
}
