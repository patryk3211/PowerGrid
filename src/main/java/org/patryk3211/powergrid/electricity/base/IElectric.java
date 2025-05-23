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
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.List;

public interface IElectric extends IWrenchable {
    /**
     * Get terminal index located at the given position.
     * @param pos Position inside the block.
     * @return Terminal index as defined in the block entity, -1 if there is no terminal at this position.
     */
    default int terminalIndexAt(BlockState state, Vec3d pos) {
        for(int i = 0; i < terminalCount(); ++i) {
            var terminal = terminal(state, i);
            if(terminal == null)
                continue;
            if(terminal.check(pos))
                return i;
        }
        return -1;
    }

    default ITerminalPlacement terminalAt(BlockState state, Vec3d pos) {
        for(int i = 0; i < terminalCount(); ++i) {
            var terminal = terminal(state, i);
            if(terminal == null)
                continue;
            if(terminal.check(pos))
                return terminal;
        }
        return null;
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
        var terminal = terminalIndexAt(state, context.getHitPos().subtract(pos.getX(), pos.getY(), pos.getZ()));
        if(terminal >= 0) {
            if(stack.hasNbt()) {
                // Continuing a connection.
                var tag = stack.getNbt();
                assert tag != null;
                var posArray = tag.getIntArray("Position");
                var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                var firstTerminal = tag.getInt("Terminal");
                var result = makeConnection(context.getWorld(), firstPosition, firstTerminal, context.getBlockPos(), terminal, context);
                if(result.isAccepted())
                    stack.setNbt(null);
                return result;
            } else {
                // Must be first connection.
                var tag = new NbtCompound();
                tag.putIntArray("Position", new int[] { pos.getX(), pos.getY(), pos.getZ() });
                tag.putInt("Terminal", terminal);
                stack.setNbt(tag);
                sendMessage(context, Lang.translate("message.connection_next").style(Formatting.GRAY).component());
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    default ElectricBehaviour getBehaviour(World world, BlockPos pos, BlockState state) {
        var blockEntity = world.getBlockEntity(pos);
        if(blockEntity instanceof SmartBlockEntity smartEntity)
            return smartEntity.getBehaviour(ElectricBehaviour.TYPE);
        return null;
    }

    static Vec3d getTerminalPos(BlockPos position, BlockState state, int terminalIndex) {
        if(state.getBlock() instanceof IElectric electric) {
            var terminal = electric.terminal(state, terminalIndex);
            if(terminal == null)
                return position.toCenterPos();
            var origin = terminal.getOrigin();
            return new Vec3d(position.getX() + origin.x, position.getY() + origin.y, position.getZ() + origin.z);
        } else {
            return position.toCenterPos();
        }
    }

    static void sendMessage(ItemUsageContext context, Text text) {
        if(context.getPlayer() != null) {
            context.getPlayer().sendMessage(text, true);
        }
    }

    static ActionResult makeConnection(World world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, ItemUsageContext context) {
        var state1 = world.getBlockState(pos1);
        var state2 = world.getBlockState(pos2);

        var behaviour1 = ((IElectric) state1.getBlock()).getBehaviour(world, pos1, state1);
        var behaviour2 = ((IElectric) state2.getBlock()).getBehaviour(world, pos2, state2);
        if(behaviour1 == null || behaviour2 == null) {
            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, at least one behaviour is null");
            return ActionResult.FAIL;
        }

        var node1 = behaviour1.getTerminal(terminal1);
        var node2 = behaviour2.getTerminal(terminal2);
        if(node1 == null || node2 == null || node1 == node2) {
            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, nodes: ({}, {})", node1, node2);
            return ActionResult.FAIL;
        }

        // Check if there is an existing connection between these nodes.
        if(behaviour1.hasConnection(terminal1, pos2, terminal2) || behaviour2.hasConnection(terminal2, pos1, terminal1)) {
            sendMessage(context, Lang.translate("message.connection_exists").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        var terminal1Pos = getTerminalPos(pos1, state1, terminal1);
        var terminal2Pos = getTerminalPos(pos2, state2, terminal2);

        var stack = context.getStack();
        assert stack.getItem() instanceof IWire;
        var item = (IWire) stack.getItem();
        var tag = stack.getNbt();
        assert tag != null;

        float distance = 0;
        if(tag.contains("Segments")) {
            for(var entry : tag.getList("Segments", NbtElement.COMPOUND_TYPE)) {
                distance += ((NbtCompound) entry).getFloat("Length");
            }
        } else {
            distance = (float) terminal1Pos.distanceTo(terminal2Pos);
            if(distance > item.getMaximumLength()) {
                sendMessage(context, Lang.translate("message.connection_too_long").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
        }

        // We round the exact distance between terminals for a more favourable item usage.
        int requiredItemCount = Math.max(Math.round(distance), 1);

        if(stack.getCount() < requiredItemCount && (context.getPlayer() == null || !context.getPlayer().isCreative())) {
            sendMessage(context, Lang.translate("message.connection_missing_items").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        if(world.isClient)
            return ActionResult.CONSUME;
        ServerWorld serverWorld = (ServerWorld) world;

        // The amount of used items dictates the resistance of a connection,
        // to make sure everything is fair.
        var R = item.getResistance() * requiredItemCount;

        WireEntity entity;
        if(tag.contains("Segments")) {
            List<BlockWireEntity.Point> points = new ArrayList<>();
            for(var entry : tag.getList("Segments", NbtElement.COMPOUND_TYPE)) {
                points.add(new BlockWireEntity.Point((NbtCompound) entry));
            }
            var lastPointList = tag.getList("LastPoint", NbtElement.FLOAT_TYPE);
            var lastPoint = new Vec3d(
                    lastPointList.getFloat(0),
                    lastPointList.getFloat(1),
                    lastPointList.getFloat(2)
            );
            var electric = (IElectric) state2.getBlock();
            var terminal = electric.terminal(state2, terminal2);
            var finalPoints = BlockTrace.findPath(world, lastPoint, terminal2Pos, terminal);
            if(finalPoints == null) {
                sendMessage(context, Lang.translate("message.connection_no_path").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
            points.addAll(finalPoints);
            entity = BlockWireEntity.create(serverWorld, pos1, terminal1, pos2, terminal2, new ItemStack(stack.getRegistryEntry(), requiredItemCount), R, points);
        } else {
            entity = HangingWireEntity.create(serverWorld, pos1, terminal1, pos2, terminal2, new ItemStack(stack.getRegistryEntry(), requiredItemCount), R);
        }

        if(!serverWorld.spawnNewEntityAndPassengers(entity)) {
            PowerGrid.LOGGER.error("Failed to spawn new connection wire entity.");
            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        if(context.getPlayer() == null || !context.getPlayer().isCreative())
            stack.decrement(requiredItemCount);

        behaviour1.addConnection(terminal1, new ElectricBehaviour.Connection(entity.getBlockPos(), entity.getUuid()));
        behaviour2.addConnection(terminal2, new ElectricBehaviour.Connection(entity.getBlockPos(), entity.getUuid()));
        return ActionResult.SUCCESS;
    }
}
