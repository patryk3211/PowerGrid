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
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ProxyElectricBehaviour extends ElectricBehaviour {
    private final Supplier<BlockPos> behaviourPosition;

    public <T extends SmartBlockEntity & IElectricEntity> ProxyElectricBehaviour(T be, Supplier<BlockPos> behaviourPosition) {
        super(be, false);
        this.behaviourPosition = behaviourPosition;
    }

    public Optional<ElectricBehaviour> getMainBehaviour() {
        var pos = behaviourPosition.get();
        if(getPos().equals(pos))
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
}
