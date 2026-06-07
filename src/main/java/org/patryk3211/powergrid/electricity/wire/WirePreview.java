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

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSpecialTextures;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.catnip.theme.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.wire.powercord.CordRenderer;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordEndpoint;
import org.patryk3211.powergrid.electricity.wire.registry.WireItemEntry;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlacementOverlay;
import org.patryk3211.powergrid.utility.VSUtils;

@Environment(EnvType.CLIENT)
public class WirePreview {
    private static final boolean DEBUG_BLOCK_TRACING = false;
    public static final Object outlineSlot = new Object();

    private static int renderPath = 0;
    private static ICordEndpoint renderedCordEndpoint;
    private static WireItemEntry renderedItem;
    private static Pair<BlockTrace.TraceState, BlockTrace.TraceResult> renderedTrace;
    private static Vec3 renderedPos1, renderedPos2;
    private static int renderedColor;

    @Nullable
    public static ItemStack getUsedWireStack(Player player) {
        var stack1 = player.getMainHandItem();
        var stack2 = player.getOffhandItem();
        if(stack1 != null && IWire.isWire(player.level(), stack1.getItem()) && stack1.hasTag()) {
            return stack1;
        } else if(stack2 != null && IWire.isWire(player.level(), stack2.getItem()) && stack2.hasTag()) {
            return stack2;
        } else {
            return null;
        }
    }

    public static void tick() {
        renderPath = 0;
        var player = Minecraft.getInstance().player;
        ItemStack wireStack = getUsedWireStack(player);
        if(wireStack == null)
            return;
        if(!IWire.isWire(player.level(), wireStack.getItem()))
            return;
        renderedItem = WireRegistry.forItem(player.level(), wireStack.getItem());
        if(IWire.isCord(player.level(), wireStack.getItem())) {
            var endpoint = WireEndpointType.deserialize(wireStack.getTagElement("Connection"));
            if(!(endpoint instanceof ICordEndpoint cordEndpoint))
                return;
            renderedCordEndpoint = cordEndpoint;
            renderPath = 3;
            return;
        }
        var target = Minecraft.getInstance().hitResult;
        if(target == null)
            return;
        if(target.getType() != HitResult.Type.BLOCK) {
            if(target.getType() == HitResult.Type.ENTITY) {
                var entityHit = (EntityHitResult) target;
                if(!(entityHit.getEntity() instanceof BlockWireEntity)) {
                    return;
                }
            } else {
                return;
            }
        }
        var tag = wireStack.getTagElement("Connection");
        var endpoint = WireEndpointType.deserialize(tag);
        if(endpoint == null)
            return;

        var world = Minecraft.getInstance().level;
        var currentPos = endpoint.getExactPosition(world);
        Direction continueDir = null;
        if(endpoint instanceof BlockWireEntityEndpoint bwe) {
            var entity = bwe.getEntity(world);
            if(entity != null) {
                var segments = entity.segments;
                if(segments.isEmpty())
                    return;
                if (bwe.getEnd()) {
                    var last = segments.get(segments.size() - 1);
                    continueDir = last.direction;
                } else {
                    var first = segments.get(0);
                    continueDir = first.direction.getOpposite();
                }
            }
        }

        var hitPoint = target.getLocation();
        ITerminalPlacement hitTerminal = null;
        if(target.getType() == HitResult.Type.BLOCK) {
            var blockTarget = (BlockHitResult) target;
            var state = world.getBlockState(blockTarget.getBlockPos());
            var electric = IElectric.getAt(world, blockTarget.getBlockPos());
            if(electric != null) {
                var pos = blockTarget.getBlockPos();
                var terminal = electric.terminalAt(state, hitPoint.subtract(pos.getX(), pos.getY(), pos.getZ()));
                if(terminal != null) {
                    hitPoint = terminal.getOrigin().add(pos.getX(), pos.getY(), pos.getZ());
                    hitTerminal = terminal;
                } else {
                    hitPoint = hitPoint.relative(blockTarget.getDirection(), 1/32f);
                }
            } else {
                hitPoint = hitPoint.relative(blockTarget.getDirection(), 1/32f);
            }
        }

        var projCurrentPos = VSUtils.projectToWorld(world, currentPos);
        var projHitPos = VSUtils.projectToWorld(world, hitPoint);
        float length = (float) VSUtils.projectedDistance(world, currentPos, hitPoint);
        // Stop rendering the preview above a thousand blocks to stop the game from freezing
        if(length > 1000)
            return;
        boolean isBlockWire = endpoint.type() != WireEndpointType.BLOCK;
        if(isBlockWire || hitTerminal == null) {
            if(!VSUtils.sameShip(world, currentPos, hitPoint))
                return;
            length = 0;
            currentPos = BlockTrace.alignPosition(currentPos);
            renderedPos1 = projCurrentPos;
            renderedTrace = BlockTrace.findPathWithState(world, currentPos, hitPoint, hitTerminal, continueDir);
            if(renderedTrace != null) {
                renderPath = 2;
                var points = renderedTrace.getSecond();
                if(points != null) {
                    for(var p : points.points()) {
                        length += p.length();
                    }
                }
            }
        } else {
            renderedColor = length < renderedItem.maximumLength() ? 0x80AAFFAA : 0x80FFAAAA;
            renderedPos1 = projCurrentPos;
            renderedPos2 = projHitPos;
            renderPath = 1;
        }

        if(!player.isCreative()) {
            int requiredItemCount = Math.max(Math.round(length * renderedItem.itemsPerMeter()), 1);
            PlacementOverlay.setItemRequirement(wireStack.getItem(), requiredItemCount, wireStack.getCount() >= requiredItemCount);
        }
    }

    public static void render(SuperRenderTypeBuffer buffer, PoseStack matrixStack, ClientLevel world, LocalPlayer player, Vec3 cameraPos) {
        matrixStack.pushPose();
        switch(renderPath) {
            case 1 -> {
                matrixStack.translate(renderedPos1.x - cameraPos.x, renderedPos1.y - cameraPos.y, renderedPos1.z - cameraPos.z);
                float thickness = renderedItem.wireThickness();
                var consumer = buffer.getBuffer(RenderType.entityTranslucent(renderedItem.texture()));
                HangingWireRenderer.renderFromPositions(matrixStack, consumer, Vec3.ZERO, renderedPos2.subtract(renderedPos1), 1.01, 1.2, thickness, LightTexture.FULL_BRIGHT, renderedColor);
            }
            case 2 -> {
                if(renderedTrace != null) {
                    if(DEBUG_BLOCK_TRACING) {
                        var lineBuffer = buffer.getBuffer(ModdedRenderLayers.getDebugLines());
                        var state = renderedTrace.getFirst();
                        for (var cell : state.states.values()) {
                            if (cell.backtrace == null)
                                continue;
                            int color = 0xFFFF0000;
                            if(!cell.isSupported())
                                color |= 0xFF00;
                            if(!cell.backtrace.isSupported())
                                color |= 0xFF;
                            BlockWireRenderer.debugLine(matrixStack, lineBuffer, LightTexture.FULL_BRIGHT, color, state.transform(cell.position), state.transform(cell.backtrace.position));
                        }
                    }
                    matrixStack.translate(renderedPos1.x - cameraPos.x, renderedPos1.y - cameraPos.y, renderedPos1.z - cameraPos.z);
                    var currentPos = Vec3.ZERO;
                    var points = renderedTrace.getSecond();
                    float thickness = renderedItem.wireThickness();
                    var consumer = buffer.getBuffer(RenderType.entityTranslucent(renderedItem.texture()));
                    if(points != null) {
                        for(var p : points.points()) {
                            var nextPos = currentPos.add(p.vector());
                            int color = points.reachedTarget() ? 0x80AAFFAA : 0x80FFAAAA;
                            BlockWireRenderer.renderSegment(matrixStack, consumer, LightTexture.FULL_BRIGHT, color, currentPos, p.direction, thickness, p.length(), 0);
                            currentPos = nextPos;
                        }
                    }
                }
            }
            case 3 -> {
                CordRenderer.renderPreview(renderedCordEndpoint, player.getRopeHoldPosition(AnimationTickHolder.getPartialTicks()),
                        matrixStack, buffer, world, renderedItem, 0xFF413C31, cameraPos);
            }
        }
        matrixStack.popPose();
    }

    public static Component distanceOverlay(Player player) {
        ItemStack wireStack = getUsedWireStack(player);
        if(wireStack == null)
            return null;
        if(!IWire.isWire(player.level(), wireStack.getItem()))
            return null;
        var wireEntry = WireRegistry.forItem(player.level(), wireStack.getItem());

        var tag = wireStack.getTagElement("Connection");
        var endpoint = WireEndpointType.deserialize(tag);
        if(endpoint == null)
            return null;

        var currentPos = endpoint.getExactPosition(player.level());
        var target = Minecraft.getInstance().hitResult;
        if(target == null || target.getType() != HitResult.Type.BLOCK)
            return null;
        var hitPoint = target.getLocation();
        var distance = VSUtils.projectedDistance(player.level(), currentPos, hitPoint);
        var msg = Lang.translate("gui.endpoint_distance")
                .add(Lang.numberConstant(distance).style(distance < wireEntry.maximumLength() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .style(ChatFormatting.WHITE);
        if(!endpoint.isValid(player.level())) {
            msg.add(Component.literal(" "))
                    .add(Lang.translate("message.no_original_connector")
                    .style(ChatFormatting.YELLOW)
                    .style(ChatFormatting.ITALIC));
        }

        return msg.component();
    }

    public static void notifyOfBlock(BlockPos pos) {
        Outliner.getInstance().showAABB(outlineSlot, new AABB(pos), 50)
                .colored(Color.RED.brighter())
                .withFaceTexture(AllSpecialTextures.CHECKERED)
                .lineWidth(0.05f);
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(AllSoundEvents.DENY.getMainEvent(), 1));
    }
}
