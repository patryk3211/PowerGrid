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

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.info.*;
import org.patryk3211.powergrid.electricity.light.string.StringLightCordItem;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.patryk3211.powergrid.electricity.wire.registry.WireItemEntry;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.ArrayList;
import java.util.List;

public class WireItem extends Item implements IWire {
    public WireItem(Properties settings) {
        super(settings);
    }

    public static <I extends Item, R extends AbstractRegistrate<?>> NonNullUnaryOperator<ItemBuilder<I, R>> properties(
            float resistance, float maxLength, float itemUseMultiplier, float thermalMass, float maxCurrent,
            ResourceLocation texture, float horizontalCoeff, float verticalCoeff, float thickness,
            boolean colorable, boolean cord) {
        return b -> b.addMiscData(AbstractPowerGridRegistrate.WIRE_ITEMS, prov -> {
            prov.add(b.getName(), new WireItemEntry(
                    resistance, itemUseMultiplier, maxLength, thermalMass, maxCurrent,
                    texture, horizontalCoeff, verticalCoeff, thickness, colorable, cord
            ));
        });
    }

    public static InteractionResultHolder<BlockWireEntity> connect(Level world, ItemStack stack, Player player, IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        if(endpoint1.getSubLevel(world) != endpoint2.getSubLevel(world)) {
            // Abort, block wires must be in the same sublevel.
            if(player != null)
                player.displayClientMessage(Lang.translate("message.connection_failed").style(ChatFormatting.RED).component(), true);
            return InteractionResultHolder.fail(null);
        }
        var entry = WireRegistry.forItem(world, stack.getItem());
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
                var newItems = (int) Math.ceil(addedLength * entry.itemsPerMeter());
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
                        if(entry.colorable() && offItem.getItem() instanceof DyeItem dye) {
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
                if(wire.getItem() != stack.getItem()) {
                    player.displayClientMessage(Lang.translate("message.connection_incorrect_wire_type").style(ChatFormatting.RED).component(), true);
                    return InteractionResultHolder.fail(null);
                }

                var newItems = (int) Math.ceil((wire.getTotalLength() + addedLength) * entry.itemsPerMeter() - wire.getWireCount());
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
        if(entity1.getItem() != entity2.getItem()) {
            if(player != null)
                player.displayClientMessage(Lang.translate("message.connection_two_wire_types").style(ChatFormatting.RED).component(), true);
            return InteractionResultHolder.fail(null);
        }
        if(entity1.getItem() != stack.getItem()) {
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

    public static CompoundEventResult<ItemStack> use(Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if(!IWire.isWire(user.level(), stack.getItem()))
            return CompoundEventResult.pass();
        if(stack.has(ModdedDataComponents.CONNECTION_DATA.get()) && user.isShiftKeyDown()) {
            stack.remove(ModdedDataComponents.CONNECTION_DATA.get());
            if(!user.level().isClientSide)
                user.displayClientMessage(Lang.translate("message.connection_reset").style(ChatFormatting.GRAY).component(), true);
            return CompoundEventResult.interruptTrue(stack);
        }
        return CompoundEventResult.pass();
    }

    public static EventResult useOn(Player player, InteractionHand hand, BlockPos blockPos, Direction direction) {
        if(player != null && player.isShiftKeyDown())
            return EventResult.pass();
        if(hand != InteractionHand.MAIN_HAND)
            return EventResult.pass();
        var stack = player.getMainHandItem();
        if(!IWire.isWire(player.level(), stack.getItem()))
            return EventResult.pass();
        if(IWire.isCord(player.level(), stack.getItem()))
            return CordItem.useOn(player, hand, blockPos, direction);
        var hit = player.pick(PlayerUtilities.getReachDistance(player) + 1.0f, 1.0f, false);
        if(!(hit instanceof BlockHitResult blockHit))
            return EventResult.pass();

        var level = player.level();
        var electric = IElectric.getAt(level, blockPos);
        var blockState = level.getBlockState(blockPos);
        if(electric != null) {
            var context = new UseOnContext(player, hand, blockHit);
            var result = electric.onWire(blockState, context);
            if(result != InteractionResult.PASS)
                return EventResult.interrupt(result.consumesAction());
        }
        var connection = stack.get(ModdedDataComponents.CONNECTION_DATA.get());
        if(connection != null) {
            // This will result in the connection being a block wire (instead of a hanging wire)
            var endpoint = connection.endpoint();
            if(endpoint == null)
                return EventResult.interruptFalse();

            var result = connect(level, stack, player, endpoint, new ImaginaryWireEndpoint(
                    hit.getLocation().relative(direction, 1 / 32f)
            ));
            if(result.getResult().consumesAction()) {
                var entity = result.getObject();
                if(entity != null) {
                    stack.set(ModdedDataComponents.CONNECTION_DATA.get(), WireConnection.of(new BlockWireEntityEndpoint(entity, true)));
                    if(player != null)
                        player.setItemInHand(hand, stack);
                }
                return EventResult.interruptTrue();
            }
        }
        return EventResult.pass();
    }

    private static final IHaveElectricProperties WIRE_DISPLAY = (stack, player, tooltip) -> {
        var entry = WireRegistry.forItem(stack.getItem());
        if(entry == null)
            return;
        Resistance.series(entry.resistancePerItem(), player, tooltip);
        Current.max(entry.maximumCurrent(), player, tooltip);
        Range.max((int) entry.maximumLength(), tooltip);
        if(stack.getItem() instanceof StringLightCordItem cord)
            cord.appendProperties(stack, player, tooltip);
    };

    @Environment(EnvType.CLIENT)
    public static void tooltip(ItemStack stack, List<Component> components, TooltipContext tooltipContext, TooltipFlag tooltipFlag) {
        var entry = WireRegistry.forItem(stack.getItem());
        if(entry == null)
            return;
        ElectricPropertiesUtils.modify(WIRE_DISPLAY, stack, Minecraft.getInstance().player, tooltipFlag, components);
    }
}
