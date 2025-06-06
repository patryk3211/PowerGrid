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
package org.patryk3211.powergrid.electricity.wire;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.ArrayList;

public class WireItem extends Item implements IWire {
    protected float resistance;
    protected float maxLength;

    protected Identifier wireTexture;
    protected float horizontalCoefficient = 1.01f;
    protected float verticalCoefficient = 1.2f;
    protected float wireThickness = 1 / 16f;

    public WireItem(Settings settings) {
        super(settings);
        resistance = 0.1f;
        maxLength = 16f;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getPlayer() != null && context.getPlayer().isSneaking())
            return super.useOnBlock(context);
        if(context.getHand() != Hand.MAIN_HAND)
            return super.useOnBlock(context);

        var blockState = context.getWorld().getBlockState(context.getBlockPos());
        if(blockState.getBlock() instanceof IElectric electric) {
            var result = electric.onWire(blockState, context);
            if(result != ActionResult.PASS)
                return result;
        }
        var tag = context.getStack().getNbt();
        if(tag != null) {
            // This will result in the connection being a block wire (instead of a hanging wire)
            var world = context.getWorld();
            var stack = context.getStack();
            var endpoint = WireEndpointType.deserialize(tag);
            if(endpoint == null)
                return ActionResult.FAIL;

            var result = connect(world, stack, context.getPlayer(), endpoint, new ImaginaryWireEndpoint(context.getHitPos()));
            if(result.getResult().isAccepted()) {
                var entity = result.getValue();
                if(entity != null) {
                    stack.setNbt(new BlockWireEntityEndpoint(entity, true).serialize());
                    var player = context.getPlayer();
                    if(player != null)
                        player.setStackInHand(context.getHand(), stack);
                }
                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock(context);
    }

    public static TypedActionResult<BlockWireEntity> connect(World world, ItemStack stack, PlayerEntity player, IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        if(endpoint1.type() == WireEndpointType.BLOCK_WIRE && endpoint2.type() == WireEndpointType.BLOCK_WIRE)
            return mergeWires(world, stack, player, (BlockWireEntityEndpoint) endpoint1, (BlockWireEntityEndpoint) endpoint2);

        var lastPoint = endpoint1.getExactPosition(world);
        var targetPoint = endpoint2.getExactPosition(world);

        ITerminalPlacement terminal = null;
        if(endpoint2 instanceof BlockWireEndpoint wireEndpoint) {
            terminal = wireEndpoint.getTerminalPlacement(world);
        }

        var result = BlockTrace.findPath(world, lastPoint, targetPoint, terminal);
        if(result != null && result.reachedTarget()) {
            float addedLength = 0;
            for(var point : result.points())
                addedLength += point.length();

            if(endpoint1.type() != WireEndpointType.BLOCK_WIRE) {
                // New entity must be created.
                var newItems = (int) Math.ceil(addedLength);
                if(!PlayerUtilities.hasEnoughItems(player, stack, newItems)) {
                    if(player != null)
                        player.sendMessage(Lang.translate("message.connection_missing_items").style(Formatting.RED).component(), true);
                    return TypedActionResult.fail(null);
                }
                if(!world.isClient) {
                    var entity = BlockWireEntity.create(world, endpoint1, stack.copyWithCount(newItems), result.points());
                    if(endpoint2.type().isConnectable())
                        entity.setEndpoint2(endpoint2);
                    if(!((ServerWorld) world).spawnNewEntityAndPassengers(entity)) {
                        PowerGrid.LOGGER.error("Failed to spawn new block wire entity.");
                        if(player != null)
                            player.sendMessage(Lang.translate("message.connection_failed").style(Formatting.RED).component(), true);
                        return TypedActionResult.fail(null);
                    }
                    PlayerUtilities.removeItems(player, stack, newItems);
                    return TypedActionResult.success(entity);
                }
            } else {
                // Entity exists, we just need to extend it.
                var bwEndpoint = (BlockWireEntityEndpoint) endpoint1;
                var wire = bwEndpoint.getEntity(world);
                if(wire.getWireItem() != stack.getItem()) {
                    player.sendMessage(Lang.translate("message.connection_incorrect_wire_type").style(Formatting.RED).component(), true);
                    return TypedActionResult.fail(null);
                }

                var newItems = (int) Math.ceil(wire.getTotalLength() + addedLength - wire.getWireCount());
                if(!PlayerUtilities.hasEnoughItems(player, stack, newItems)) {
                    if(player != null)
                        player.sendMessage(Lang.translate("message.connection_missing_items").style(Formatting.RED).component(), true);
                    return TypedActionResult.fail(null);
                }

                if(!world.isClient) {
                    if(!bwEndpoint.getEnd()) {
                        PowerGrid.LOGGER.error("Cannot extend wire at start (must be flipped beforehand)");
                        return TypedActionResult.fail(null);
                    }
                    if(endpoint2.type().isConnectable())
                        wire.setEndpoint2(endpoint2);
                    wire.extend(result.points(), newItems);
                    PlayerUtilities.removeItems(player, stack, newItems);
                    return TypedActionResult.success(wire);
                }
            }

            return TypedActionResult.success(null);
        }

        return TypedActionResult.fail(null);
    }

    public static TypedActionResult<BlockWireEntity> mergeWires(World world, ItemStack stack, PlayerEntity player, BlockWireEntityEndpoint endpoint1, BlockWireEntityEndpoint endpoint2) {
        if(world.isClient)
            throw new IllegalStateException("Wire merging must occur on server");

        var lastPoint = endpoint1.getExactPosition(world);
        var targetPoint = endpoint2.getExactPosition(world);

        var entity1 = endpoint1.getEntity(world);
        var entity2 = endpoint2.getEntity(world);
        if(entity1.getWireItem() != entity2.getWireItem()) {
            if(player != null)
                player.sendMessage(Lang.translate("message.connection_two_wire_types").style(Formatting.RED).component(), true);
            return TypedActionResult.fail(null);
        }
        if(entity1.getWireItem() != stack.getItem()) {
            if(player != null)
                player.sendMessage(Lang.translate("message.connection_incorrect_wire_type").style(Formatting.RED).component(), true);
            return TypedActionResult.fail(null);
        }

        var result = BlockTrace.findPath(world, lastPoint, targetPoint, null);
        if(result == null || !result.reachedTarget())
            return TypedActionResult.fail(null);

        float addedLength = 0;
        for(var point : result.points())
            addedLength += point.length();

        var newItems = (int) Math.ceil(entity1.getTotalLength() + entity2.getTotalLength() + addedLength - entity1.getWireCount() - entity2.getWireCount());
        if(!PlayerUtilities.hasEnoughItems(player, stack, newItems)) {
            if(player != null)
                player.sendMessage(Lang.translate("message.connection_missing_items").style(Formatting.RED).component(), true);
            return TypedActionResult.fail(null);
        }

        BlockWireEntity targetEntity, sourceEntity;
        boolean flipped = false;
        if(endpoint1.getEnd() || !endpoint2.getEnd()) {
            // Merge endpoint2 into endpoint1
            targetEntity = entity1;
            if(!endpoint1.getEnd())
                // TODO: Check if spawn packet arrival doesn't break segments array or wire object.
                targetEntity = targetEntity.flip();
            sourceEntity = entity2;
            if(endpoint2.getEnd())
                flipped = true;
        } else {
            // Merge endpoint1 into endpoint2
            targetEntity = entity2;
            sourceEntity = entity1;
        }

        // Add connecting path
        targetEntity.extend(result.points(), newItems, false);

        if(flipped) {
            var segments = new ArrayList<BlockWireEntity.Point>();
            for(var segment : sourceEntity.segments) {
                segments.add(0, new BlockWireEntity.Point(segment.direction.getOpposite(), segment.gridLength));
            }
            targetEntity.setEndpoint2(sourceEntity.getEndpoint1());
            targetEntity.extend(segments, sourceEntity.getWireCount());
        } else {
            targetEntity.setEndpoint2(sourceEntity.getEndpoint2());
            targetEntity.extend(sourceEntity.segments, sourceEntity.getWireCount());
        }

        targetEntity.makeWire();
        sourceEntity.discard();
        return TypedActionResult.success(targetEntity);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return super.hasGlint(stack) || stack.hasNbt();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if(stack.hasNbt() && user.isSneaking()) {
            stack.setNbt(null);
            if(!world.isClient)
                user.sendMessage(Lang.translate("message.connection_reset").style(Formatting.GRAY).component(), true);
            return TypedActionResult.success(stack, true);
        }
        return super.use(world, user, hand);
    }

    @Override
    public float getResistance() {
        return resistance;
    }

    @Override
    public float getMaximumLength() {
        return maxLength;
    }

    @Environment(EnvType.CLIENT)
    public Identifier getWireTexture() {
        return wireTexture;
    }

    @Environment(EnvType.CLIENT)
    public float getHorizontalCoefficient() {
        return horizontalCoefficient;
    }

    @Environment(EnvType.CLIENT)
    public float getVerticalCoefficient() {
        return verticalCoefficient;
    }

    @Environment(EnvType.CLIENT)
    public float getWireThickness() {
        return wireThickness;
    }
}
