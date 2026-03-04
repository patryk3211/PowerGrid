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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.patryk3211.powergrid.electricity.wire.powercord.CordRenderer;
import org.patryk3211.powergrid.electricity.wire.powercord.ICordEndpoint;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlacementOverlay;

@Environment(EnvType.CLIENT)
public class WirePreview {
    private static final boolean DEBUG_BLOCK_TRACING = false;
    public static final Object outlineSlot = new Object();

    @Nullable
    public static ItemStack getUsedWireStack(Player player) {
        var stack1 = player.getMainHandItem();
        var stack2 = player.getOffhandItem();
        if(stack1 != null && stack1.getItem() instanceof IWire && stack1.hasTag()) {
            return stack1;
        } else if(stack2 != null && stack2.getItem() instanceof IWire && stack2.hasTag()) {
            return stack2;
        } else {
            return null;
        }
    }

    private static void renderCord(SuperRenderTypeBuffer buffer, PoseStack matrixStack, ClientLevel world, LocalPlayer player, HitResult target, ItemStack wireStack) {
        var endpoint = WireEndpointType.deserialize(wireStack.getTagElement("Connection"));
        if(!(endpoint instanceof ICordEndpoint cordEndpoint))
            return;
        CordRenderer.renderPreview(cordEndpoint, player.getRopeHoldPosition(AnimationTickHolder.getPartialTicks()),
                matrixStack, buffer, world, (CordItem) wireStack.getItem(), 0xFF413C31);
    }

    public static void render(SuperRenderTypeBuffer buffer, PoseStack matrixStack, ClientLevel world, LocalPlayer player, HitResult target) {
        ItemStack wireStack = getUsedWireStack(player);
        if(wireStack == null)
            return;
        if(!(wireStack.getItem() instanceof WireItem wireItem))
            return;
        if(wireStack.getItem() instanceof CordItem) {
            renderCord(buffer, matrixStack, world, player, target, wireStack);
            return;
        }
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
        var consumer = buffer.getBuffer(RenderType.entityTranslucent(wireItem.getWireTexture()));
        float thickness = wireItem.getWireThickness();

        var endpoint = WireEndpointType.deserialize(tag);
        if(endpoint == null)
            return;

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

        float length = (float) currentPos.distanceTo(hitPoint);
        // Stop rendering the preview above a thousand blocks to stop the game from freezing
        if(length > 1000)
            return;
        boolean isBlockWire = endpoint.type() != WireEndpointType.BLOCK;
        if(isBlockWire || hitTerminal == null) {
            length = 0;
            currentPos = BlockTrace.alignPosition(currentPos);
            var output = BlockTrace.findPathWithState(world, currentPos, hitPoint, hitTerminal, continueDir);
            if(output != null) {
                if(DEBUG_BLOCK_TRACING) {
                    var lineBuffer = buffer.getBuffer(ModdedRenderLayers.getDebugLines());
                    var state = output.getFirst();
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
                var points = output.getSecond();
                if(points != null) {
                    for(var p : points.points()) {
                        var nextPos = currentPos.add(p.vector());
                        int color = points.reachedTarget() ? 0x80AAFFAA : 0x80FFAAAA;
                        BlockWireRenderer.renderSegment(matrixStack, consumer, LightTexture.FULL_BRIGHT, color, currentPos, p.direction, thickness, p.length(), 0);
                        currentPos = nextPos;
                        length += p.length();
                    }
                }
            }
        } else {
            var color = length < wireItem.getMaximumLength() ? 0x80AAFFAA : 0x80FFAAAA;
            HangingWireRenderer.renderFromPositions(matrixStack, consumer, currentPos, hitPoint, 1.01, 1.2, thickness, LightTexture.FULL_BRIGHT, color);
        }

        if(!player.isCreative()) {
            int requiredItemCount = Math.max(Math.round(length * wireItem.getItemUseMultiplier()), 1);
            PlacementOverlay.setItemRequirement(wireStack.getItem(), requiredItemCount, wireStack.getCount() >= requiredItemCount);
        }
    }

    public static Component distanceOverlay(Player player) {
        ItemStack wireStack = getUsedWireStack(player);
        if(wireStack == null)
            return null;
        if(!(wireStack.getItem() instanceof WireItem wire))
            return null;

        var tag = wireStack.getTagElement("Connection");
        var endpoint = WireEndpointType.deserialize(tag);
        if(endpoint == null)
            return null;

        var currentPos = endpoint.getExactPosition(player.level());
        var target = Minecraft.getInstance().hitResult;
        if(target == null || target.getType() != HitResult.Type.BLOCK)
            return null;
        var hitPoint = target.getLocation();
        var distance = hitPoint.distanceTo(currentPos);
        return Lang.translate("gui.endpoint_distance")
                .add(Lang.numberConstant(distance).style(distance < wire.getMaximumLength() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .style(ChatFormatting.WHITE)
                .component();
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
