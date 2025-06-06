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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.network.packets.BlockWireAttachC2SPacket;
import org.patryk3211.powergrid.network.packets.BlockWireCutC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

@Environment(EnvType.CLIENT)
public class ClientWireInteractions {
    private static BlockWireEntity currentEntity = null;
    private static int firstSegmentIndex;
    private static int firstSegmentPoint;

    private static Pair<Integer, Integer> getSegment(BlockWireEntity entity, Vec3d hitPos) {
        var localPos = hitPos.subtract(entity.getPos());
        var thickness = entity.getWireItem().getWireThickness();
        for(int i = 0; i < entity.boundingBoxes.size(); ++i) {
            var bb = entity.boundingBoxes.get(i);
            // Test with slightly larger bounding boxes.
            if(bb.expand(thickness * 0.2f).contains(localPos)) {
                // Found segment containing hit pos.
                var segment = entity.segments.get(i);
                int segmentPoint = switch(segment.direction.getAxis()) {
                    case X -> (int) Math.round(Math.abs(segment.start.x - hitPos.x) * 16);
                    case Y -> (int) Math.round(Math.abs(segment.start.y - hitPos.y) * 16);
                    case Z -> (int) Math.round(Math.abs(segment.start.z - hitPos.z) * 16);
                };
                if(segmentPoint < 0)
                    segmentPoint = 0;
                else if(segmentPoint > segment.gridLength)
                    segmentPoint = segment.gridLength;
                return new Pair<>(i, segmentPoint);
            }
        }
        return null;
    }

    public static ActionResult segmentCut(BlockWireEntity entity) {
        var mc = MinecraftClient.getInstance();
        var target = mc.crosshairTarget;
        if(target.getType() != HitResult.Type.ENTITY)
            return ActionResult.FAIL;

        if(currentEntity != entity) {
            // First cut.
            var hitPos = target.getPos();
            var segment = getSegment(entity, hitPos);
            if(segment != null) {
                firstSegmentIndex = segment.getLeft();
                firstSegmentPoint = segment.getRight();
                currentEntity = entity;
            }
            return ActionResult.CONSUME;
        } else {
            var secondSegment = getSegment(entity, target.getPos());
            if(secondSegment != null) {
                ClientPlayNetworking.send(new BlockWireCutC2SPacket(entity, firstSegmentIndex, firstSegmentPoint, secondSegment.getLeft(), secondSegment.getRight()));
                currentEntity = null;
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    public static ActionResult attachWire(BlockWireEntity entity) {
        var mc = MinecraftClient.getInstance();
        var target = mc.crosshairTarget;
        if(target.getType() != HitResult.Type.ENTITY)
            return ActionResult.FAIL;
        var stack = mc.player.getStackInHand(Hand.MAIN_HAND);
        if(entity.getWireItem() != stack.getItem()) {
            mc.player.sendMessage(Lang.translate("message.connection_incorrect_wire_type").style(Formatting.RED).component(), true);
            return ActionResult.FAIL;
        }

        var hitPos = target.getPos();
        var segment = getSegment(entity, hitPos);
        if(segment == null)
            return ActionResult.FAIL;

        ClientPlayNetworking.send(new BlockWireAttachC2SPacket(entity, segment.getLeft(), segment.getRight()));
        return ActionResult.SUCCESS;
    }
}
