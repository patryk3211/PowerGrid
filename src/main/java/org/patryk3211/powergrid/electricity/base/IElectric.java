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

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

public interface IElectric extends IWrenchable {
    /**
     * Get terminal located at the given position.
     * @param pos Position inside the block.
     * @return Terminal index as defined in the block entity, -1 if there is no terminal at this position.
     */
    default int terminalAt(BlockState state, Vec3d pos) {
        for(int i = 0; i < terminalCount(); ++i) {
            var terminal = terminal(state, i);
            if(terminal.check(pos))
                return i;
        }
        return -1;
    }

    int terminalCount();

    /**
     * Get the terminal placement of a given terminal
     * @param state Block state
     * @param index Terminal index
     * @return Terminal placement
     */
    ITerminalPlacement terminal(BlockState state, int index);

    default ActionResult onWire(BlockState state, ItemUsageContext context) {
        var stack = context.getStack();
        var pos = context.getBlockPos();
        var terminal = terminalAt(state, context.getHitPos().subtract(pos.getX(), pos.getY(), pos.getZ()));
        if(terminal >= 0) {
            if(stack.hasNbt()) {
                // Continuing a connection.
                var tag = stack.getNbt();
                assert tag != null;
                var posArray = tag.getIntArray("position");
                var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                var firstTerminal = tag.getInt("terminal");
                stack.setNbt(null);
                var result = makeConnection(context.getWorld(), firstPosition, firstTerminal, context.getBlockPos(), terminal, context);
                return result ? ActionResult.SUCCESS : ActionResult.FAIL;
            } else {
                // Must be first connection.
                var tag = new NbtCompound();
                tag.putIntArray("position", new int[] { pos.getX(), pos.getY(), pos.getZ() });
                tag.putInt("terminal", terminal);
                stack.setNbt(tag);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Nullable
    static IElectricNode getTerminal(World world, BlockPos pos, int index) {
        var behaviour = getBehaviour(world, pos);
        return behaviour == null ? null : behaviour.getTerminal(index);
    }

    @Nullable
    static ElectricBehaviour getBehaviour(World world, BlockPos pos) {
        var blockEntity = world.getBlockEntity(pos);
        if(blockEntity instanceof SmartBlockEntity smartEntity)
            return smartEntity.getBehaviour(ElectricBehaviour.TYPE);
        return null;
    }

    static Vec3d getTerminalPos(BlockPos position, BlockState state, int terminalIndex) {
        if(state.getBlock() instanceof IElectric electric) {
            var terminal = electric.terminal(state, terminalIndex);
            var origin = terminal.getOrigin();
            return new Vec3d(position.getX() + origin.x, position.getY() + origin.y, position.getZ() + origin.z);
        } else {
            return position.toCenterPos();
        }
    }

    static boolean makeConnection(World world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, ItemUsageContext context) {
        if(world.isClient)
            return false;
        ServerWorld serverWorld = (ServerWorld) world;

        var behaviour1 = getBehaviour(world, pos1);
        var behaviour2 = getBehaviour(world, pos2);
        if(behaviour1 == null || behaviour2 == null)
            return false;

        // Check if there is an existing connection between these nodes.
        if(behaviour1.hasConnection(terminal1, pos2, terminal2) || behaviour2.hasConnection(terminal2, pos1, terminal1))
            return false;

        var node1 = behaviour1.getTerminal(terminal1);
        var node2 = behaviour2.getTerminal(terminal2);
        if(node1 == null || node2 == null)
            return false;

        // Put a wire between the nodes.
        var stack = context.getStack();
        assert stack.getItem() instanceof IWire;
        var item = (IWire) stack.getItem();
        // TODO: Calculate resistance using the distance between nodes.
        var R = item.getResistance();
        var wire = GlobalElectricNetworks.makeConnection(behaviour1, node1, behaviour2, node2, R);

        var entity = WireEntity.create(serverWorld, pos1, terminal1, pos2, terminal2);
        if (!serverWorld.spawnNewEntityAndPassengers(entity))
            PowerGrid.LOGGER.error("Failed to spawn new connection wire entity.");

        behaviour1.addConnection(terminal1, new ElectricBehaviour.Connection(pos2, terminal2, wire, stack.copy(), entity.getUuid()));
        behaviour2.addConnection(terminal2, new ElectricBehaviour.Connection(pos1, terminal1, wire, stack.copy(), entity.getUuid()));
        return true;
    }
}
