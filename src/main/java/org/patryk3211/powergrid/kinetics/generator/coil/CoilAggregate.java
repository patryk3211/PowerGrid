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

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.network.packets.AggregateCoilsS2CPacket;

import java.util.HashSet;
import java.util.Set;

public class CoilAggregate {
    private final World world;
    private final Set<CoilBlockEntity> coils = new HashSet<>();
    private CoilBlockEntity.AggregateType type;

    @Nullable
    private CoilBlockEntity outputCoil;

    public CoilAggregate(World world) {
        this.type = CoilBlockEntity.AggregateType.SERIES;
        this.world = world;
    }

    public boolean addCoil(CoilBlockEntity coil) {
        return coils.add(coil);
    }

    public int size() {
        return coils.size();
    }

    public void apply() {
        coils.forEach(coil -> coil.setAggregate(this));
        if(!world.isClient) {
            // Send aggregate to clients
            var packet = new AggregateCoilsS2CPacket(coils);
            sendToTrackers(packet);
        }
        makeOutput(null);
    }

    private <T extends FabricPacket> void sendToTrackers(T packet) {
        assert !world.isClient;
        Set<ServerPlayerEntity> trackers = new HashSet<>();
        coils.forEach(coil -> trackers.addAll(PlayerLookup.tracking(coil)));
        for(var player : trackers) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    public void makeOutput(@Nullable CoilBlockEntity outputCoil) {
        if(coils.isEmpty())
            throw new IllegalStateException("Cannot set output coil of an empty aggregate");

        int parallel = 0, series = 0;
        for(var coil : coils) {
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

            if(coil.getAggregateType() == CoilBlockEntity.AggregateType.SERIES) {
                ++series;
            } else {
                ++parallel;
            }
        }
        this.outputCoil = outputCoil;
        if(outputCoil != null) {
            this.type = outputCoil.getAggregateType();
        } else {
            if(series >= parallel) {
                type = CoilBlockEntity.AggregateType.SERIES;
            } else {
                type = CoilBlockEntity.AggregateType.PARALLEL;
            }
        }
        // Force type propagation.
        coils.forEach(coil -> coil.propagateType(type));
    }

    public void removeOutput(@NotNull CoilBlockEntity outputCoil) {
        world.setBlockState(outputCoil.getPos(), outputCoil.getCachedState().with(CoilBlock.HAS_TERMINALS, false), Block.NOTIFY_LISTENERS);

        if(this.outputCoil != outputCoil)
            return;
        this.outputCoil.removeElectricBehaviour();
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
        // Current is always spread out over all the nodes.
        // The only difference is how it affects the load.
        return nodeCurrent / coils.size();
    }

    public void setType(CoilBlockEntity.AggregateType type) {
        if(this.type != type) {
            this.type = type;
            coils.forEach(coil -> coil.propagateType(type));
        }
    }

    public CoilBlockEntity.AggregateType getType() {
        return type;
    }
}
