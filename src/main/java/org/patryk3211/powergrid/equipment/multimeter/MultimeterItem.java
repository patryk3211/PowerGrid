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

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class MultimeterItem extends Item implements IHaveElectricProperties {
    public static final float MAX_DISTANCE = 5;

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
                        return onTerminal(context.getLevel(), new CircuitBoardEndpoint(pos, x, y), context.getItemInHand());
                    }
                    return InteractionResult.PASS;
                }).orElse(InteractionResult.PASS);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        var data = getModeData(stack);
        switch(getMode(stack)) {
            case 0 -> {
                var pos = WireEndpointType.deserialize(data.getCompound("Pos"));
                var neg = WireEndpointType.deserialize(data.getCompound("Neg"));
                if(pos != null) {
                    var posPos = pos.getExactPosition(level);
                    if(posPos.distanceTo(entity.position()) > MAX_DISTANCE || !pos.isValid(level)) {
                        if(entity instanceof Player player)
                            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                                    .style(ChatFormatting.GRAY)
                                    .component(), true);
                        data.remove("Pos");
                    }
                }
                if(neg != null) {
                    var negPos = neg.getExactPosition(level);
                    if(negPos.distanceTo(entity.position()) > MAX_DISTANCE || !neg.isValid(level)) {
                        if(entity instanceof Player player)
                            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                                    .style(ChatFormatting.GRAY)
                                    .component(), true);
                        data.remove("Neg");
                    }
                }
            }
        }
    }

    // 0 = Voltage, 1 = Current
    public int getMode(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Mode");
    }

    private CompoundTag getModeData(ItemStack stack) {
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
            player.getItemInHand(usedHand).removeTagKey("ModeData");
            player.displayClientMessage(Lang.translate("message.multimeter_disconnected")
                    .style(ChatFormatting.GRAY)
                    .component(), true);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }
        return super.use(level, player, usedHand);
    }

    private InteractionResult onTerminal(Level level, IWireEndpoint endpoint, ItemStack stack) {
        if(getMode(stack) != 0)
            return InteractionResult.PASS;
        var data = getModeData(stack);
        if(data.contains("Pos") && data.contains("Neg"))
            return InteractionResult.PASS;
        if(data.contains("Pos")) {
            data.put("Neg", endpoint.serialize());
        } else {
            data.put("Pos", endpoint.serialize());
        }
        return InteractionResult.SUCCESS;
    }

    public void switchMode(ItemStack stack, Player user) {
        var tag = stack.getOrCreateTag();
        var current = tag.getInt("Mode");
        current = (current + 1) % 2;
        tag.putInt("Mode", current);
        user.displayClientMessage(Lang.translate("tooltip.multimeter.mode")
                .add(Lang.translate("tooltip.multimeter.mode." + current))
                .component(), true);
    }

    public Component getText(Level level, Player user, ItemStack stack) {
        return switch(getMode(stack)) {
            case 0 -> {
                var data = getModeData(stack);
                var pos = WireEndpointType.deserialize(data.getCompound("Pos"));
                var neg = WireEndpointType.deserialize(data.getCompound("Neg"));
                if(pos == null || neg == null)
                    yield null;
                if(!pos.isValid(level) || !neg.isValid(level))
                    yield null;
                var posNode = pos instanceof CircuitBoardEndpoint e ? e.getGenericNode(level) : pos.getNode(level);
                var negNode = neg instanceof CircuitBoardEndpoint e ? e.getGenericNode(level) : neg.getNode(level);
                var voltageNum = posNode.getVoltage() - negNode.getVoltage();
                var voltage = Unit.VOLTAGE.formatWithPrefixes(voltageNum);
                if(voltageNum > 500) {
                    voltage = Lang.text(">500 ").add(Unit.VOLTAGE.get());
                } else if(voltageNum < -500) {
                    voltage = Lang.text("<-500 ").add(Unit.VOLTAGE.get());
                }
                yield Lang.translate("tooltip.multimeter.voltage")
                        .add(voltage.style(ChatFormatting.BLUE))
                        .style(ChatFormatting.GRAY)
                        .component();
            }
            default -> null;
        };
    }

    @Environment(EnvType.CLIENT)
    public static Component multimeterOverlayText(Player player) {
        var stack = player.getMainHandItem();
        if(!(stack.getItem() instanceof MultimeterItem multimeter))
            return null;
        return multimeter.getText(player.level(), player, stack);
    }

    @Environment(EnvType.CLIENT)
    public static void keybindPressed() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if(player == null)
            return;
        var stack = player.getMainHandItem();
        if(!(stack.getItem() instanceof MultimeterItem multimeter))
            return;
        multimeter.switchMode(stack, player);
    }

    private static final ResourceLocation TEXTURE = PowerGrid.texture("special/copper_wire");
    @Environment(EnvType.CLIENT)
    private static void renderProbe(Vec3 point, SuperRenderTypeBuffer buffer, PoseStack matrixStack, ClientLevel world, LocalPlayer player) {
        HangingWireRenderer.renderFromPositions(matrixStack, buffer.getBuffer(RenderType.entitySolid(TEXTURE)),
                player.getRopeHoldPosition(AnimationTickHolder.getPartialTicks()),
                point, 1.01f, 1.01f, 1 / 16f, world, -1);
    }

    @Environment(EnvType.CLIENT)
    public static void render(SuperRenderTypeBuffer buffer, PoseStack matrixStack, ClientLevel world, LocalPlayer player) {
        var stack = player.getMainHandItem();
        if(!(stack.getItem() instanceof MultimeterItem multimeter))
            return;
        switch(multimeter.getMode(stack)) {
            case 0 -> {
                var data = multimeter.getModeData(stack);
                var pos = WireEndpointType.deserialize(data.getCompound("Pos"));
                if(pos != null && pos.isValid(world)) {
                    renderProbe(pos.getExactPosition(world), buffer, matrixStack, world, player);
                }
                var neg = WireEndpointType.deserialize(data.getCompound("Neg"));
                if(neg != null && neg.isValid(world)) {
                    renderProbe(neg.getExactPosition(world), buffer, matrixStack, world, player);
                }
            }
        }
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Voltage.max(500, player, tooltip);
    }
}
