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
package org.patryk3211.powergrid.equipment.multimeter;

import net.createmod.catnip.math.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.network.packets.MultimeterDataC2SPacket;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class MultimeterItem extends Item implements IHaveElectricProperties {
    public MultimeterItem(Properties properties) {
        super(properties.stacksTo(1));
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
            var pos = context.getClickedPos();
            var terminal = electric.terminalIndexAt(blockState, context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
            if(terminal >= 0) {
                return onTerminal(context.getLevel(), new BlockWireEndpoint(pos, terminal), context.getItemInHand());
            }
        }
        return context.getLevel().getBlockEntity(context.getClickedPos(), ModdedBlockEntities.CIRCUIT_BOARD.get())
                .map(be -> {
                    var pos = context.getClickedPos();
                    var state = context.getLevel().getBlockState(context.getClickedPos());
                    var hitLocalPos = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
                    hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleY(state), Direction.Axis.Y);
                    hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleX(state), Direction.Axis.X);
                    if(hitLocalPos.y >= 2 / 16f && hitLocalPos.y <= 3 / 16f) {
                        int x = (int) (hitLocalPos.x * 16);
                        int y = (int) (hitLocalPos.z * 16);
                        if(!be.getSchematic().getLayer(CircuitSchematic.Layer.FRONT, x, y))
                            return InteractionResult.FAIL;
                        return onTerminal(context.getLevel(), new CircuitBoardEndpoint(pos, x, y), context.getItemInHand());
                    }
                    return InteractionResult.PASS;
                }).orElse(InteractionResult.PASS);
    }

    @Environment(EnvType.CLIENT)
    private static Vec3 getAttachmentPoint() {
        var hit = Minecraft.getInstance().hitResult;
        if(hit == null || hit.getType() != HitResult.Type.ENTITY)
            return null;
        var entityHit = (EntityHitResult) hit;
        return entityHit.getLocation();
    }

    public InteractionResult useOnWire(Player player, ItemStack stack, InteractionHand hand, BaseWireEntity wireEntity) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        if(getMode(stack) != 1) {
            // Enable current mode.
            setMode(stack, 1);
        }
        if(player.level().isClientSide) {
            var point = getAttachmentPoint();
            var data = getModeData(stack);
            data.putFloat("X", (float) point.x);
            data.putFloat("Y", (float) point.y);
            data.putFloat("Z", (float) point.z);
            ModdedPackets.sendToServer(new MultimeterDataC2SPacket(point, wireEntity));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        var data = getModeData(stack);
        float maxDistance = ModdedConfigs.server().equipment.multimeterDistance.getF();
        switch(getMode(stack)) {
            case 0 -> {
                var pos = WireEndpointType.deserialize(data.getCompound("Pos"));
                var neg = WireEndpointType.deserialize(data.getCompound("Neg"));
                if(pos != null) {
                    var posPos = pos.getExactPosition(level);
                    if(posPos.distanceTo(entity.position()) > maxDistance || !pos.isValid(level)) {
                        if(entity instanceof Player player)
                            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                                    .style(ChatFormatting.GRAY)
                                    .component(), true);
                        data.remove("Pos");
                    }
                }
                if(neg != null) {
                    var negPos = neg.getExactPosition(level);
                    if(negPos.distanceTo(entity.position()) > maxDistance || !neg.isValid(level)) {
                        if(entity instanceof Player player)
                            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                                    .style(ChatFormatting.GRAY)
                                    .component(), true);
                        data.remove("Neg");
                    }
                }
            }
            case 1 -> {
                if(!level.isClientSide) {
                    if(data.contains("UUID")) {
                        var genericEntity = ((ServerLevel) level).getEntity(data.getUUID("UUID"));
                        if(genericEntity != null) {
                            data.putInt("EID", genericEntity.getId());
                        } else {
                            if(entity instanceof Player player)
                                player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                                        .style(ChatFormatting.GRAY)
                                        .component(), true);
                            // Wipe all data
                            stack.getOrCreateTag().remove("ModeData");
                        }
                    }
                }
                if(data.contains("X")) {
                    var point = new Vec3(data.getFloat("X"), data.getFloat("Y"), data.getFloat("Z"));
                    if(point.distanceTo(entity.position()) > maxDistance) {
                        if(entity instanceof Player player)
                            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                                    .style(ChatFormatting.GRAY)
                                    .component(), true);
                        // Wipe all data
                        stack.getOrCreateTag().remove("ModeData");
                    }
                }
            }
        }
    }

    // 0 = Voltage, 1 = Current
    public int getMode(ItemStack stack) {
        if(stack.getTag() == null || !stack.getTag().contains("Mode"))
            return -1;
        return stack.getOrCreateTag().getInt("Mode");
    }

    public void setMode(ItemStack stack, int mode) {
        var tag = stack.getOrCreateTag();
        tag.putInt("Mode", mode);
        tag.remove("ModeData");
    }

    public CompoundTag getModeData(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        if(tag.contains("ModeData")) {
            return tag.getCompound("ModeData");
        } else {
            var data = new CompoundTag();
            tag.put("ModeData", data);
            return data;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(player.isShiftKeyDown() && usedHand == InteractionHand.MAIN_HAND) {
            player.getItemInHand(usedHand).setTag(null);
            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                    .style(ChatFormatting.GRAY)
                    .component(), true);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }
        return super.use(level, player, usedHand);
    }

    private InteractionResult onTerminal(Level level, IWireEndpoint endpoint, ItemStack stack) {
        if(getMode(stack) != 0) {
            setMode(stack, 0);
        }
        var data = getModeData(stack);
        if(data.contains("Pos")) {
            var current = WireEndpointType.deserialize(data.getCompound("Pos"));
            if(endpoint.equals(current)) {
                data.remove("Pos");
                return InteractionResult.SUCCESS;
            }
        }
        if(data.contains("Neg")) {
            var current = WireEndpointType.deserialize(data.getCompound("Neg"));
            if(endpoint.equals(current)) {
                data.remove("Neg");
                return InteractionResult.SUCCESS;
            }
        }
        if(data.contains("Pos") && data.contains("Neg"))
            return InteractionResult.PASS;
        if(data.contains("Pos")) {
            data.put("Neg", endpoint.serialize());
        } else {
            data.put("Pos", endpoint.serialize());
        }
        return InteractionResult.CONSUME;
    }

    public float getMeasurement(Level level, ItemStack stack) {
        var data = getModeData(stack);
        return switch(getMode(stack)) {
            case 0 -> {
                var pos = WireEndpointType.deserialize(data.getCompound("Pos"));
                var neg = WireEndpointType.deserialize(data.getCompound("Neg"));
                if(pos == null || neg == null)
                    yield 0;
                if(!pos.isValid(level) || !neg.isValid(level))
                    yield 0;
                double posV = 0, negV = 0;
                if(data.contains("PosV")) {
                    posV = data.getFloat("PosV");
                } else {
                    var posNode = pos instanceof CircuitBoardEndpoint e ? e.getGenericNode(level) : pos.getNode(level);
                    if(posNode == null)
                        yield 0;
                    posV = posNode.getVoltage();
                }
                if(data.contains("NegV")) {
                    negV = data.getFloat("NegV");
                } else {
                    var negNode = neg instanceof CircuitBoardEndpoint e ? e.getGenericNode(level) : neg.getNode(level);
                    if(negNode == null)
                        yield 0;
                    negV = negNode.getVoltage();
                }
                yield (float) (posV - negV);
            }
            case 1 -> {
                var lineId = data.getInt("EID");
                var entity = level.getEntity(lineId);
                if(entity instanceof BaseWireEntity wire) {
                    yield wire.measuredCurrent();
                }
                yield 0;
            }
            default -> 0;
        };
    }

    public Component getText(Level level, Player user, ItemStack stack) {
        var measurement = getMeasurement(level, stack);
        return switch(getMode(stack)) {
            case 0 -> {
                var voltage = Unit.VOLTAGE.formatWithPrefixes(measurement);
                float maxVolt = ModdedConfigs.server().equipment.multimeterVoltage.getF();
                if(measurement > maxVolt) {
                    voltage = Lang.text(">" + maxVolt + " ").add(Unit.VOLTAGE.get());
                } else if(measurement < -maxVolt) {
                    voltage = Lang.text("<-" + maxVolt + " ").add(Unit.VOLTAGE.get());
                }
                yield Lang.translate("tooltip.multimeter.voltage")
                        .add(voltage.style(ChatFormatting.BLUE))
                        .style(ChatFormatting.GRAY)
                        .component();
            }
            case 1 -> {
                var current = Unit.CURRENT.formatWithPrefixes(measurement);
                float maxAmp = ModdedConfigs.server().equipment.multimeterCurrent.getF();
                if(measurement > maxAmp) {
                    current = Lang.text(">" + maxAmp + " ").add(Unit.CURRENT.get());
                } else if(measurement < -maxAmp) {
                    current = Lang.text("<-" + maxAmp + " ").add(Unit.CURRENT.get());
                }
                yield Lang.translate("tooltip.multimeter.current")
                        .add(current.style(ChatFormatting.YELLOW))
                        .style(ChatFormatting.GRAY)
                        .component();
            }
            default -> null;
        };
    }

    public float getDial(Level level, ItemStack stack) {
        float measurement = getMeasurement(level, stack);
        float value = Math.abs(switch(getMode(stack)) {
            case 0 -> measurement / ModdedConfigs.server().equipment.multimeterVoltage.getF();
            case 1 -> measurement / ModdedConfigs.server().equipment.multimeterCurrent.getF();
            default -> 0;
        });
        if(value > 1)
            return 1 + level.random.nextFloat() * 0.125f;
        return value;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Voltage.max(ModdedConfigs.server().equipment.multimeterVoltage.getF(), player, tooltip);
        Current.max(ModdedConfigs.server().equipment.multimeterCurrent.getF(), player, tooltip);
    }
}
