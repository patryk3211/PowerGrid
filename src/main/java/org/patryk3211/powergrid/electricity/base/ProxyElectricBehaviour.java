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
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ProxyElectricBehaviour extends ElectricBehaviour {
    protected final Supplier<BlockPos> behaviourPosition;

    public <T extends SmartBlockEntity & IElectricEntity> ProxyElectricBehaviour(T be, Supplier<BlockPos> behaviourPosition) {
        super(be, false);
        this.behaviourPosition = behaviourPosition;
    }

    protected <T extends SmartBlockEntity & IElectricEntity> ProxyElectricBehaviour(T be, boolean buildCircuit, Supplier<BlockPos> behaviourPosition) {
        super(be, buildCircuit);
        this.behaviourPosition = behaviourPosition;
    }

    public Optional<ElectricBehaviour> getMainBehaviour() {
        var pos = behaviourPosition.get();
        if(getPos().equals(pos) || pos == null)
            return Optional.empty();
        if(!getWorld().isLoaded(pos))
            return Optional.empty();
        return Optional.ofNullable(get(getWorld(), pos, TYPE));
    }

    @Override
    public void joinNetwork(ElectricalNetwork network) {
        getMainBehaviour().ifPresentOrElse(
                b -> b.joinNetwork(network),
                () -> super.joinNetwork(network)
        );
    }

    @Override
    public @Nullable OwnedFloatingNode getTerminal(int index) {
        return getMainBehaviour()
                .map(b -> b.getTerminal(index))
                .orElseGet(() -> super.getTerminal(index));
    }

    @Override
    public boolean hasTerminal(int terminal) {
        return getMainBehaviour()
                .map(b -> b.hasTerminal(terminal))
                .orElseGet(() -> super.hasTerminal(terminal));
    }

    @Override
    public List<OwnedFloatingNode> getExternalNodes() {
        return getMainBehaviour()
                .map(ElectricBehaviour::getExternalNodes)
                .orElseGet(super::getExternalNodes);
    }

    @Override
    public void initialize() {
        for(var node : getExternalNodes()) {
            if(node.endpoint instanceof BlockWireEndpoint endpoint) {
                // Make an endpoint using the main's terminal index and proxy's position.
                var thisEndpoint = new BlockWireEndpoint(getPos(), endpoint.getTerminal());
                // Nodes from main behaviour are considered "new" and "up-to-date"
                GlobalElectricNetworks.getWorldNetworks(getWorld())
                        .addAndMigrateNode(thisEndpoint, node);
            }
        }
    }

    @Override
    public void unload() {
    }

    @Override
    public void remove() {
        // Node holder might not be removed.
        breakConnections();
    }

    public void baseInitialize() {
        super.initialize();
    }

    protected void baseUnload() {
        super.unload();
    }

    protected void baseRemove() {
        super.remove();
    }
}
