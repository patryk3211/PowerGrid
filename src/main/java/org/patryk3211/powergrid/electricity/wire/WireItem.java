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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.config.WireValues;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.ArrayList;

public class WireItem extends Item implements IWire {
    protected ResourceLocation wireTexture;
    protected float horizontalCoefficient = 1.01f;
    protected float verticalCoefficient = 1.2f;
    protected float wireThickness = 1 / 16f;
    protected boolean colored = false;

    public WireItem(Properties settings) {
        super(settings);
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
        var stack = context.getItemInHand();
        var connection = stack.get(ModdedDataComponents.CONNECTION_DATA.get());
        if(connection != null) {
            var world = context.getLevel();
            var endpoint = connection.endpoint();
            if(endpoint == null)
                return InteractionResult.FAIL;

            var result = connect(world, stack, context.getPlayer(), endpoint, new ImaginaryWireEndpoint(
                    context.getClickLocation().relative(context.getClickedFace(), 1 / 32f)
            ));
            if(result.getResult().consumesAction()) {
                var entity = result.getObject();
                if(entity != null) {
                    stack.set(ModdedDataComponents.CONNECTION_DATA.get(), WireConnection.of(new BlockWireEntityEndpoint(entity, true)));
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

        if(endpoint1.type() == WireEndpointType.BLOCK && endpoint2.type() == WireEndpointType.BLOCK_WIRE) {
            var e = endpoint1;
            endpoint1 = endpoint2;
            endpoint2 = e;
        }

        var lastPoint = endpoint1.getExactPosition(world);
        if(endpoint1.type() != WireEndpointType.BLOCK_WIRE)
            lastPoint = BlockTrace.alignPosition(lastPoint);
        var targetPoint = endpoint2.getExactPosition(world);

        Direction continueDir = null;
        if(endpoint1 instanceof BlockWireEntityEndpoint bwe) {
            var entity = bwe.getEntity(world);
            if(entity == null)
                return InteractionResultHolder.fail(null);
            var segments = entity.segments;
            if(bwe.getEnd()) {
                if(segments.size() == 0)
                    return InteractionResultHolder.fail(null);
                var last = segments.get(segments.size() - 1);
                continueDir = last.direction;
            } else {
                var first = segments.get(0);
                continueDir = first.direction.getOpposite();
            }
        }

        ITerminalPlacement terminal = null;
        if(endpoint2 instanceof BlockWireEndpoint wireEndpoint) {
            terminal = wireEndpoint.getTerminalPlacement(world);
        }

        var result = BlockTrace.findPath(world, lastPoint, targetPoint, terminal, continueDir);
        if(result != null && result.reachedTarget()) {
            float addedLength = 0;
            for(var point : result.points())
                addedLength += point.length();

            if(endpoint1.type() != WireEndpointType.BLOCK_WIRE) {
                // New entity must be created.
                var newItems = (int) Math.ceil(addedLength * ((IWire) stack.getItem()).getItemUseMultiplier());
                if(!PlayerUtilities.hasEnoughItems(player, stack, newItems)) {
                    if(player != null)
                        player.displayClientMessage(Lang.translate("message.connection_missing_items").style(ChatFormatting.RED).component(), true);
                    return InteractionResultHolder.fail(null);
                }
                if(!world.isClientSide) {
                    var entity = BlockWireEntity.create(world, endpoint1, stack.copyWithCount(newItems), result.points());
                    if(endpoint2.type().isConnectable())
                        entity.setEndpoint2(endpoint2);
                    if(player != null) {
                        var offItem = player.getOffhandItem();
                        if(((IWire) stack.getItem()).canBeColored() && offItem.getItem() instanceof DyeItem dye) {
                            entity.setColor(dye.getDyeColor());
                        }
                    }
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

                var newItems = (int) Math.ceil((wire.getTotalLength() + addedLength) * ((IWire) stack.getItem()).getItemUseMultiplier() - wire.getWireCount());
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

        Direction continueDir;
        var currentSegments = endpoint1.getEntity(world).segments;
        if(endpoint1.getEnd()) {
            var last = currentSegments.get(currentSegments.size() - 1);
            continueDir = last.direction;
        } else {
            var first = currentSegments.get(0);
            continueDir = first.direction.getOpposite();
        }

        var result = BlockTrace.findPath(world, lastPoint, targetPoint, null, continueDir);
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
        return super.isFoil(stack) || stack.has(ModdedDataComponents.CONNECTION_DATA.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if(stack.has(ModdedDataComponents.CONNECTION_DATA.get()) && user.isShiftKeyDown()) {
            stack.remove(ModdedDataComponents.CONNECTION_DATA.get());
            if(!world.isClientSide)
                user.displayClientMessage(Lang.translate("message.connection_reset").style(ChatFormatting.GRAY).component(), true);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        return super.use(world, user, hand);
    }

    @Override
    public float getResistance() {
        return WireValues.resistance(this);
    }

    @Override
    public float getMaximumLength() {
        return WireValues.maxLength(this);
    }

    @Override
    public float getItemUseMultiplier() {
        return WireValues.itemUseMultiplier(this);
    }

    @Override
    public float getDissipationFactor() {
        return WireValues.dissipationFactor(this);
    }

    @Override
    public float getThermalMass() {
        return WireValues.thermalMass(this);
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

    @Override
    public boolean canBeColored() {
        return colored;
    }
}
