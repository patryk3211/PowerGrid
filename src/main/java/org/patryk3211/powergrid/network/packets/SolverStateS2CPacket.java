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
package org.patryk3211.powergrid.network.packets;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.network.ClientBoundPackets;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

;

public class SolverStateS2CPacket implements SimplePacket {
    private final Map<BlockPos, double[]> solverValues = new HashMap<>();
    public final Set<ChunkPos> chunks = new HashSet<>();

    public SolverStateS2CPacket(Level world, ElectricalNetwork network) {
        // Read values straight from the solver guess vector
        var vector = network.getLastGuess();
        if(vector == null)
            return;

        for(var node : network.getNodes()) {
            if(!(node instanceof OwnedFloatingNode owned))
                continue;
            if(!(owned.endpoint instanceof BlockWireEndpoint endpoint))
                continue;
            var behaviour = endpoint.getElectricBehaviour(world);
            if(behaviour == null)
                continue;
            if(solverValues.containsKey(behaviour.getPos()))
                continue;
            chunks.add(new ChunkPos(behaviour.getPos()));
            // Ordering is important here, first serialize external, then internal nodes
            var external = behaviour.getExternalNodes();
            var internal = behaviour.getInternalNodes();
            if(external.isEmpty())
                continue;
            if(external.get(0).getNetwork() != network)
                continue;

            var doubles = new double[external.size() + internal.size()];
            int index = 0;
            for(var eNode : behaviour.getExternalNodes()) {
                if(vector.getNumRows() <= eNode.getIndex())
                    continue;
                doubles[index++] = vector.get(eNode.getIndex(), 0);
            }
            for(var iNode : behaviour.getInternalNodes()) {
                if(vector.getNumRows() <= iNode.getIndex())
                    continue;
                doubles[index++] = vector.get(iNode.getIndex(), 0);
            }
            solverValues.put(behaviour.getPos(), doubles);
        }
    }

    public SolverStateS2CPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        for(int i = 0; i < count; ++i) {
            var pos = buf.readBlockPos();
            var doubles = new double[buf.readInt()];
            for(int j = 0; j < doubles.length; ++j)
                doubles[j] = buf.readDouble();
            solverValues.put(pos, doubles);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(solverValues.size());
        for(var entry : solverValues.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            var doubles = entry.getValue();
            buf.writeInt(doubles.length);
            for(var value : doubles)
                buf.writeDouble(value);
        }
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var world = ClientBoundPackets.world();
            for(var entry : solverValues.entrySet()) {
                var pos = entry.getKey();
                if(!world.hasChunkAt(pos))
                    continue;
                var behaviour = BlockEntityBehaviour.get(world, pos, ElectricBehaviour.TYPE);
                if(behaviour == null)
                    continue;
                var external = behaviour.getExternalNodes();
                if(external.isEmpty())
                    continue;
                var network = external.get(0).getNetwork();
                if(network == null)
                    continue;
                var doubles = entry.getValue();
                int index = 0;
                // This assumes that the solver guess vector is not reallocated before every solve,
                // which is true for the current implementation.
                var vector = network.getLastGuess();
                if(vector == null)
                    continue;
                for(var eNode : external) {
                    if(vector.getNumRows() <= eNode.getIndex())
                        continue;
                    var diff = doubles[index] - vector.get(eNode.getIndex(), 0);
                    if(diff > 0.5)
                        PowerGrid.LOGGER.debug("Solver sync corrected difference of {} at {}", diff, pos);
                    vector.set(eNode.getIndex(), 0, doubles[index++]);
                }
                for(var iNode : behaviour.getInternalNodes()) {
                    if(vector.getNumRows() <= iNode.getIndex())
                        continue;
                    var diff = doubles[index] - vector.get(iNode.getIndex(), 0);
                    if(diff > 0.5)
                        PowerGrid.LOGGER.debug("Solver sync corrected difference of {} at {}", diff, pos);
                    vector.set(iNode.getIndex(), 0, doubles[index++]);
                }
            }
        });
    }
}
