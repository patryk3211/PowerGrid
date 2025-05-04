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
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.utility.BlockTrace;

public class BlockWirePreview {
    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(BlockWirePreview::render);
//        InteractEvents.USE.register(BlockWirePreview::onUse);
    }

//    private static ActionResult onUse(MinecraftClient client, HitResult hit, Hand hand) {
//
//        return ActionResult.PASS;
//    }

    private static void render(SuperRenderTypeBuffer buffer, MatrixStack matrixStack, ClientWorld world, ClientPlayerEntity player, HitResult target) {
        ItemStack wireStack;
        var stack1 = player.getMainHandStack();
        var stack2 = player.getOffHandStack();
        if(stack1 != null && stack1.getItem() instanceof IWire && stack1.hasNbt()) {
            wireStack = stack1;
        } else if(stack2 != null && stack2.getItem() instanceof IWire && stack2.hasNbt()) {
            wireStack = stack2;
        } else {
            return;
        }

        var tag = wireStack.getNbt();
        var consumer = buffer.getBuffer(ModdedRenderLayers.getDebugLines());

        var posArray = tag.getIntArray("Position");
        var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
        var firstTerminal = tag.getInt("Terminal");

        var currentPos = IElectric.getTerminalPos(firstPosition, world.getBlockState(firstPosition), firstTerminal);
        if(tag.contains("Segments")) {
            for(var entry : tag.getList("Segments", NbtElement.COMPOUND_TYPE)) {
                var point = new BlockWireEntity.Point((NbtCompound) entry);
                var nextPos = currentPos.add(point.vector());
                BlockWireRenderer.debugLine(matrixStack, consumer, 255, 0xFFFF00FF, currentPos, nextPos);
                currentPos = nextPos;
            }
        }

        var hitPoint = target.getPos();
        BlockState passThrough = null;
        if(target.getType() == HitResult.Type.BLOCK) {
            var blockTarget = (BlockHitResult) target;
            var state = world.getBlockState(blockTarget.getBlockPos());
            if(state.getBlock() instanceof IElectric electric) {
                var pos = blockTarget.getBlockPos();
                var terminal = electric.terminalAt(state, hitPoint.subtract(pos.getX(), pos.getY(), pos.getZ()));
                if(terminal != null) {
                    hitPoint = terminal.getOrigin().add(pos.getX(), pos.getY(), pos.getZ());
                    passThrough = state;
                }
            }
        }

        var points = BlockTrace.findPath(world, currentPos, hitPoint, passThrough);
        if(points != null) {
            for (var p : points) {
                var nextPos = currentPos.add(p.vector());
                BlockWireRenderer.debugLine(matrixStack, consumer, 255, 0xFF00FF00, currentPos, nextPos);
                currentPos = nextPos;
            }
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
