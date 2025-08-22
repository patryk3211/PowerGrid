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
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.febridge.IFEBridgeHandler;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Optional;
import java.util.function.Supplier;

public class BridgeElectricBehaviour extends ElectricBehaviour {
    private final BlockPos behaviourPosition;
    private IFEBridgeHandler bridgeBehaviour;
    private long readEnergy;
    public long currentRate;
    private boolean fetched = false;
    private final Supplier<SwitchedWire> converterWire;

    public <T extends SmartBlockEntity & IElectricEntity> BridgeElectricBehaviour(T be, BlockPos behaviourPosition, Supplier<SwitchedWire> converterWire) {
        super(be);
        this.behaviourPosition = behaviourPosition;
        this.converterWire = converterWire;
    }

    private void constructBehaviours() {
        fetched = true;
        var world = getWorld();
        var mainBehaviour = get(world, behaviourPosition, TYPE);
        if(mainBehaviour != null)
            return;
        bridgeBehaviour = makeFEHandler(blockEntity);
        if(bridgeBehaviour == null) {
            world.destroyBlock(getPos(), true);
            return;
        }
        bridgeBehaviour.setAmount(readEnergy);
    }

    public Optional<ElectricBehaviour> getMainBehaviour() {
        if(!fetched)
            constructBehaviours();
        return Optional.ofNullable(get(getWorld(), behaviourPosition, TYPE));
    }

    @Nullable
    public IFEBridgeHandler getBridgeBehaviour() {
        if(!fetched)
            constructBehaviours();
        return bridgeBehaviour;
    }

    @Override
    public void joinNetwork(ElectricalNetwork network) {
        getMainBehaviour().ifPresentOrElse(
                b -> b.joinNetwork(network),
                () -> super.joinNetwork(network)
        );
    }

    @Override
    public @Nullable IElectricNode getTerminal(int index) {
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
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if(bridgeBehaviour != null) {
            bridgeBehaviour.setAmount(nbt.getLong("Energy"));
        } else {
            readEnergy = nbt.getLong("Energy");
        }
        if(clientPacket)
            currentRate = nbt.getLong("Rate");
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(bridgeBehaviour != null) {
            nbt.putLong("Energy", bridgeBehaviour.getAmount());
            if(clientPacket)
                nbt.putLong("Rate", currentRate);
        }
    }

    @Override
    public void tick() {
        super.tick();
        var energyStorage = getBridgeBehaviour();
        if(energyStorage == null)
            return;

        var wire = converterWire.get();
        var world = getWorld();

        energyStorage.charge(wire);
        var moved = energyStorage.moveEnergy();
        if(!world.isClientSide) {
            if(moved != currentRate) {
                currentRate = moved;
                blockEntity.sendData();
            }
        } else if(moved == 0) {
            // Since some things might not sync this to client, this is necessary
            // to provide a valid, client-side simulation parameters.
            var amount = energyStorage.getAmount();
            energyStorage.setAmount(Math.max(amount - currentRate, 0));
        }
        energyStorage.manageWire(wire);
    }

    @ExpectPlatform
    public static IFEBridgeHandler makeFEHandler(BlockEntity be) {
        throw new AssertionError();
    }

    public boolean isFE() {
        return bridgeBehaviour != null;
    }

    public long getBufferedAmount() {
        return bridgeBehaviour.getAmount();
    }
}
