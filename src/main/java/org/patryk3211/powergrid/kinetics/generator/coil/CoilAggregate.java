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
package org.patryk3211.powergrid.kinetics.generator.coil;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.network.packets.AggregateCoilsS2CPacket;

import java.util.HashSet;
import java.util.Set;

public class CoilAggregate {
    private final Set<CoilBlockEntity> coils = new HashSet<>();
    private CoilBlockEntity.AggregateType type;
    @Nullable
    private CoilBlockEntity outputCoil;

    public CoilAggregate() {
        this.type = CoilBlockEntity.AggregateType.SERIES;
    }

    public boolean addCoil(CoilBlockEntity coil) {
        return coils.add(coil);
    }

    public void apply() {
        coils.forEach(coil -> coil.setAggregate(this));
        makeOutput(null);
    }

    public void makeOutput(@Nullable CoilBlockEntity outputCoil) {
        World world = outputCoil != null ? outputCoil.getWorld() : null;
        for(var coil : coils) {
            if(world == null)
                world = coil.getWorld();
            assert world != null;
            if(coil == outputCoil) {
                world.setBlockState(coil.getPos(), coil.getCachedState().with(CoilBlock.HAS_TERMINALS, true), Block.NOTIFY_LISTENERS);
                coil.addElectricBehaviour();
                continue;
            }

            if(coil.getCachedState().get(CoilBlock.HAS_TERMINALS) && outputCoil == null) {
                outputCoil = coil;
            } else {
                // Only one coil can be the output coil.
                world.setBlockState(coil.getPos(), coil.getCachedState().with(CoilBlock.HAS_TERMINALS, false), Block.NOTIFY_LISTENERS);
                coil.removeElectricBehaviour();
            }
        }
        this.outputCoil = outputCoil;
        if(outputCoil != null) {
            setType(outputCoil.getAggregateType());
        }
        if(world != null && !world.isClient && !coils.isEmpty()) {
            var coil = coils.iterator().next();
            var packet = new AggregateCoilsS2CPacket(coils);
            for(var player : PlayerLookup.tracking(coil)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    public void removeOutput(CoilBlockEntity outputCoil) {
        var world = outputCoil.getWorld();
        assert world != null;
        world.setBlockState(outputCoil.getPos(), outputCoil.getCachedState().with(CoilBlock.HAS_TERMINALS, false), Block.NOTIFY_LISTENERS);

        if(this.outputCoil != outputCoil)
            return;
        outputCoil.removeElectricBehaviour();
        this.outputCoil = null;
    }

    public float totalVoltage() {
        float voltage = 0;
        if(type == CoilBlockEntity.AggregateType.SERIES) {
            for(var coil : coils) {
                voltage += coil.getCoilBehaviour().emfVoltage();
            }
        } else {
            boolean first = true;
            float sign = 0;
            for(var coil : coils) {
                var coilVoltage = coil.getCoilBehaviour().emfVoltage();
                if(first) {
                    sign = Math.signum(coilVoltage);
                    voltage = Math.abs(coilVoltage);
                    first = false;
                } else {
                    if(sign != Math.signum(coilVoltage)) {
                        voltage = 0;
                        break;
                    }
                    voltage = Math.min(voltage, Math.abs(coilVoltage));
                }
            }
            voltage *= sign;
        }
        return voltage;
    }

    public float totalResistance() {
        float resistance = 0;
        // For now assume that every coil has the same resistance.
        if(type == CoilBlockEntity.AggregateType.SERIES) {
            resistance = CoilBlock.resistance() * coils.size();
        } else {
            resistance = CoilBlock.resistance() / coils.size();
        }
        return resistance;
    }

    public float coilCurrent() {
        var nodeCurrent = outputCoil != null ? outputCoil.sourceNodeCurrent() : 0;
        if(type == CoilBlockEntity.AggregateType.PARALLEL)
            // Current is spread out over all the nodes.
            return nodeCurrent / coils.size();
        return nodeCurrent;
    }

    public void setType(CoilBlockEntity.AggregateType type) {
        this.type = type;
        coils.forEach(coil -> coil.propagateType(type));
    }

    public CoilBlockEntity.AggregateType getType() {
        return type;
    }
}
