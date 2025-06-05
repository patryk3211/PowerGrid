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
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

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

    default boolean accepts(ItemStack wireStack) {
        // By default, only light wires can go directly to devices.
        return wireStack.isIn(ModdedTags.Item.LIGHT_WIRES.tag);
    }

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
            if(!accepts(context.getStack())) {
                sendMessage(context, Lang.translate("message.connection_incorrect_wire_type").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
            if(stack.hasNbt()) {
                // Continuing a connection.
                var endpoint = WireEndpointType.deserialize(stack.getNbt());
                var result = makeConnection(context.getWorld(), endpoint, new BlockWireEndpoint(pos, terminal), context);
                if(result.isAccepted())
                    stack.setNbt(null);
                return result;
            } else {
                // Must be first connection.
                var endpoint = new BlockWireEndpoint(pos, terminal);
                var tag = endpoint.serialize();
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

    static ActionResult makeConnection(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, ItemUsageContext context) {
        if(endpoint1.type() == WireEndpointType.BLOCK && endpoint2.type() == WireEndpointType.BLOCK) {
            // Hanging wire connection.
            return makeHangingWireConnection(world, (BlockWireEndpoint) endpoint1, (BlockWireEndpoint) endpoint2, context);
        }

        var result = WireItem.connect(world, context.getStack(), context.getPlayer(), endpoint1, endpoint2);
        return result.getResult();
//        if(result.getResult() == ActionResult.SUCCESS) {
////            var entity = result.getValue();
////            if(entity != null) {
////                entity.setEndpoint2(endpoint2);
////                entity.makeWire();
////            }
//            return ActionResult.SUCCESS;
//        }

//        return ActionResult.FAIL;
    }

    static ActionResult makeHangingWireConnection(World world, BlockWireEndpoint endpoint1, BlockWireEndpoint endpoint2, ItemUsageContext context) {
        var behaviour1 = endpoint1.getElectricBehaviour(world);
        var behaviour2 = endpoint2.getElectricBehaviour(world);
        if(behaviour1 == null || behaviour2 == null) {
            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, at least one behaviour is null");
            return ActionResult.FAIL;
        }

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);
        if(node1 == null || node2 == null || node1 == node2) {
            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
            PowerGrid.LOGGER.error("Connection failed, nodes: ({}, {})", node1, node2);
            return ActionResult.FAIL;
        }

        // Check if there is an existing connection between these nodes.
        if(behaviour1.hasConnection(endpoint1.getTerminal(), endpoint2) || behaviour2.hasConnection(endpoint2.getTerminal(), endpoint1)) {
            sendMessage(context, Lang.translate("message.connection_exists").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        var terminal1Pos = endpoint1.getExactPosition(world);
        var terminal2Pos = endpoint2.getExactPosition(world);

        var stack = context.getStack();
        assert stack.getItem() instanceof IWire;
        var item = (IWire) stack.getItem();
        var tag = stack.getNbt();
        assert tag != null;

        float distance = (float) terminal1Pos.distanceTo(terminal2Pos);
        if(distance > item.getMaximumLength()) {
            sendMessage(context, Lang.translate("message.connection_too_long").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        // We round the exact distance between terminals for a more favourable item usage.
        int requiredItemCount = Math.max(Math.round(distance), 1);
        if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, requiredItemCount)) {
            sendMessage(context, Lang.translate("message.connection_missing_items").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        if(world.isClient)
            return ActionResult.SUCCESS;
        ServerWorld serverWorld = (ServerWorld) world;

        // The amount of used items dictates the resistance of a connection,
        // to make sure everything is fair.
        var R = item.getResistance() * requiredItemCount;
        var entity = HangingWireEntity.create(serverWorld, endpoint1, endpoint2, new ItemStack(stack.getRegistryEntry(), requiredItemCount), R);

        if(!serverWorld.spawnNewEntityAndPassengers(entity)) {
            PowerGrid.LOGGER.error("Failed to spawn new connection wire entity.");
            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
            return ActionResult.FAIL;
        }

        if(context.getPlayer() == null || !context.getPlayer().isCreative())
            stack.decrement(requiredItemCount);

        return ActionResult.SUCCESS;
    }
}
