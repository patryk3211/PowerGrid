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
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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

    protected ResourceLocation wireTexture;
    protected float horizontalCoefficient = 1.01f;
    protected float verticalCoefficient = 1.2f;
    protected float wireThickness = 1 / 16f;

    public WireItem(Properties settings) {
        super(settings);
        resistance = 0.1f;
        maxLength = 16f;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            return super.useOn(context);
        if(context.getHand() != InteractionHand.MAIN_HAND)
            return super.useOn(context);

        var electric = IElectric.getAt(context.getLevel(), context.getClickedPos());
        var blockState = context.getLevel().getBlockState(context.getClickedPos());
        if(electric != null) {
            var result = electric.onWire(blockState, context);
            if(result != InteractionResult.PASS)
                return result;
        }
        var tag = context.getItemInHand().getTag();
        if(tag != null) {
            // This will result in the connection being a block wire (instead of a hanging wire)
            var world = context.getLevel();
            var stack = context.getItemInHand();
            var endpoint = WireEndpointType.deserialize(tag);
            if(endpoint == null)
                return InteractionResult.FAIL;

            var result = connect(world, stack, context.getPlayer(), endpoint, new ImaginaryWireEndpoint(context.getClickLocation()));
            if(result.getResult().consumesAction()) {
                var entity = result.getObject();
                if(entity != null) {
                    stack.setTag(new BlockWireEntityEndpoint(entity, true).serialize());
                    var player = context.getPlayer();
                    if(player != null)
                        player.setItemInHand(context.getHand(), stack);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    public static InteractionResultHolder<BlockWireEntity> connect(Level world, ItemStack stack, Player player, IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
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
                        player.displayClientMessage(Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component(), true);
                    return InteractionResultHolder.fail(null);
                }
                if(!world.isClientSide) {
                    var entity = BlockWireEntity.create(world, endpoint1, stack.copyWithCount(newItems), result.points());
                    if(endpoint2.type().isConnectable())
                        entity.setEndpoint2(endpoint2);
                    if(!((ServerLevel) world).tryAddFreshEntityWithPassengers(entity)) {
                        PowerGrid.LOGGER.error("Failed to spawn new block wire entity.");
                        if(player != null)
                            player.displayClientMessage(Lang.translate("message.connection_failed").style(ChatFormatting.RED).component(), true);
                        return InteractionResultHolder.fail(null);
                    }
                    PlayerUtilities.removeItems(player, stack, newItems);
                    return InteractionResultHolder.success(entity);
                }
            } else {
                // Entity exists, we just need to extend it.
                var bwEndpoint = (BlockWireEntityEndpoint) endpoint1;
                var wire = bwEndpoint.getEntity(world);
                if(wire.getWireItem() != stack.getItem()) {
                    player.displayClientMessage(Lang.translate("message.connection_incorrect_wire_type").style(ChatFormatting.RED).component(), true);
                    return InteractionResultHolder.fail(null);
                }

                var newItems = (int) Math.ceil(wire.getTotalLength() + addedLength - wire.getWireCount());
                if(!PlayerUtilities.hasEnoughItems(player, stack, newItems)) {
                    if(player != null)
                        player.displayClientMessage(Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component(), true);
                    return InteractionResultHolder.fail(null);
                }

                if(!world.isClientSide) {
                    if(!bwEndpoint.getEnd()) {
                        PowerGrid.LOGGER.error("Cannot extend wire at start (must be flipped beforehand)");
                        return InteractionResultHolder.fail(null);
                    }
                    if(endpoint2.type().isConnectable())
                        wire.setEndpoint2(endpoint2);
                    wire.extend(result.points(), newItems);
                    PlayerUtilities.removeItems(player, stack, newItems);
                    return InteractionResultHolder.success(wire);
                }
            }

            return InteractionResultHolder.success(null);
        }

        return InteractionResultHolder.fail(null);
    }

    public static InteractionResultHolder<BlockWireEntity> mergeWires(Level world, ItemStack stack, Player player, BlockWireEntityEndpoint endpoint1, BlockWireEntityEndpoint endpoint2) {
        if(world.isClientSide)
            throw new IllegalStateException("Wire merging must occur on server");

        var lastPoint = endpoint1.getExactPosition(world);
        var targetPoint = endpoint2.getExactPosition(world);

        var entity1 = endpoint1.getEntity(world);
        var entity2 = endpoint2.getEntity(world);
        if(entity1 == entity2)
            return InteractionResultHolder.fail(null);
        if(entity1.getWireItem() != entity2.getWireItem()) {
            if(player != null)
                player.displayClientMessage(Lang.translate("message.connection_two_wire_types").style(ChatFormatting.RED).component(), true);
            return InteractionResultHolder.fail(null);
        }
        if(entity1.getWireItem() != stack.getItem()) {
            if(player != null)
                player.displayClientMessage(Lang.translate("message.connection_incorrect_wire_type").style(ChatFormatting.RED).component(), true);
            return InteractionResultHolder.fail(null);
        }

        var result = BlockTrace.findPath(world, lastPoint, targetPoint, null);
        if(result == null || !result.reachedTarget())
            return InteractionResultHolder.fail(null);

        float addedLength = 0;
        for(var point : result.points())
            addedLength += point.length();

        var newItems = (int) Math.ceil(entity1.getTotalLength() + entity2.getTotalLength() + addedLength - entity1.getWireCount() - entity2.getWireCount());
        if(!PlayerUtilities.hasEnoughItems(player, stack, newItems)) {
            if(player != null)
                player.displayClientMessage(Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component(), true);
            return InteractionResultHolder.fail(null);
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

        sourceEntity.discard();
        return InteractionResultHolder.success(targetEntity);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.hasTag();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if(stack.hasTag() && user.isShiftKeyDown()) {
            stack.setTag(null);
            if(!world.isClientSide)
                user.displayClientMessage(Lang.translate("message.connection_reset").style(ChatFormatting.GRAY).component(), true);
            return InteractionResultHolder.sidedSuccess(stack, true);
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
    public ResourceLocation getWireTexture() {
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
