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

import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.PlacementOverlay;

@Environment(EnvType.CLIENT)
public class WirePreview {
    private static final boolean DEBUG_BLOCK_TRACING = false;

    public static int wireItemCount;

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(WirePreview::render);
    }

    @Nullable
    public static ItemStack getUsedWireStack(PlayerEntity player) {
        var stack1 = player.getMainHandStack();
        var stack2 = player.getOffHandStack();
        if(stack1 != null && stack1.getItem() instanceof IWire && stack1.hasNbt()) {
            return stack1;
        } else if(stack2 != null && stack2.getItem() instanceof IWire && stack2.hasNbt()) {
            return stack2;
        } else {
            return null;
        }
    }

    private static void render(SuperRenderTypeBuffer buffer, MatrixStack matrixStack, ClientWorld world, ClientPlayerEntity player, HitResult target) {
        ItemStack wireStack = getUsedWireStack(player);
        if(wireStack == null)
            return;
        if(!(wireStack.getItem() instanceof WireItem wireItem))
            return;

        var tag = wireStack.getNbt();
        // TODO: Use correct texture for the used item.
        var consumer = buffer.getBuffer(RenderLayer.getEntityTranslucent(wireItem.getWireTexture()));
        float thickness = wireItem.getWireThickness();

        if(!tag.contains("Position") || !tag.contains("Terminal")) {
            if(tag.contains("Turns") && !player.isCreative()) {
                int requiredItemCount = tag.getInt("Turns");
                PlacementOverlay.setItemRequirement(wireStack.getItem(), requiredItemCount, wireStack.getCount() >= requiredItemCount);
            }
            return;
        }

        var posArray = tag.getIntArray("Position");
        var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
        var firstTerminal = tag.getInt("Terminal");

        var currentPos = IElectric.getTerminalPos(firstPosition, world.getBlockState(firstPosition), firstTerminal);
        boolean hasSegments = false;
        float length = 0;
        if(tag.contains("Segments")) {
            currentPos = BlockTrace.alignPosition(currentPos);
            for(var entry : tag.getList("Segments", NbtElement.COMPOUND_TYPE)) {
                var point = new BlockWireEntity.Point((NbtCompound) entry);
                var nextPos = currentPos.add(point.vector());
                BlockWireRenderer.renderSegment(matrixStack, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0x60AAFFFF, currentPos, point.direction, thickness, point.length, 0);
                currentPos = nextPos;
                length += point.length;
            }
            hasSegments = true;
        }

        var hitPoint = target.getPos();
        ITerminalPlacement passThrough = null;
        if(target.getType() == HitResult.Type.BLOCK) {
            var blockTarget = (BlockHitResult) target;
            var state = world.getBlockState(blockTarget.getBlockPos());
            if(state.getBlock() instanceof IElectric electric) {
                var pos = blockTarget.getBlockPos();
                var terminal = electric.terminalAt(state, hitPoint.subtract(pos.getX(), pos.getY(), pos.getZ()));
                if(terminal != null) {
                    hitPoint = terminal.getOrigin().add(pos.getX(), pos.getY(), pos.getZ());
                    passThrough = terminal;
                }
            }
        }

        if(hasSegments || passThrough == null) {
            currentPos = BlockTrace.alignPosition(currentPos);
            var output = BlockTrace.findPathWithState(world, currentPos, hitPoint, passThrough);
            if(output != null) {
                if(DEBUG_BLOCK_TRACING) {
                    var lineBuffer = buffer.getBuffer(ModdedRenderLayers.getDebugLines());
                    var state = output.getLeft();
                    for (var cell : state.states.values()) {
                        if (cell.backtrace == null)
                            continue;
                        BlockWireRenderer.debugLine(matrixStack, lineBuffer, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0xFFFF0000, state.transform(cell.position), state.transform(cell.backtrace.position));
                    }
                }
                var points = output.getRight();
                if(points != null) {
                    for(var p : points) {
                        var nextPos = currentPos.add(p.vector());
                        BlockWireRenderer.renderSegment(matrixStack, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0x80AAFFAA, currentPos, p.direction, thickness, p.length, 0);
                        currentPos = nextPos;
                        length += p.length;
                    }
                }
            }
        } else {
            HangingWireRenderer.renderFromPositions(matrixStack, consumer, currentPos, hitPoint, 1.01, 1.2, thickness, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0x80AAFFAA);
            length = (float) currentPos.distanceTo(hitPoint);
        }

        if(!player.isCreative()) {
            int requiredItemCount = Math.max(Math.round(length), 1);
            PlacementOverlay.setItemRequirement(wireStack.getItem(), requiredItemCount, wireStack.getCount() >= requiredItemCount);
        }
    }

    private static void render(WorldRenderContext context) {
        var matrixStack = context.matrixStack();
        matrixStack.push();

        var partialTicks = context.tickDelta();
        var cameraPos = context.camera().getPos();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        var buffer = SuperRenderTypeBuffer.getInstance();
        var player = MinecraftClient.getInstance().player;

        var world = context.world();
        var target = MinecraftClient.getInstance().crosshairTarget;
        if(player != null && target != null) {
            render(buffer, matrixStack, world, player, target);
        }

        buffer.draw();
        matrixStack.pop();
    }
}
