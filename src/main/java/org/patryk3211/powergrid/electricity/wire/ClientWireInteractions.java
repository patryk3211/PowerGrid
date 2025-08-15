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

import net.createmod.catnip.math.VecHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.BlockWireAttachC2SPacket;
import org.patryk3211.powergrid.network.packets.BlockWireCutC2SPacket;
import org.patryk3211.powergrid.utility.Lang;

@Environment(EnvType.CLIENT)
public class ClientWireInteractions {
    private static BlockWireEntity currentEntity = null;
    private static int firstSegmentIndex;
    private static int firstSegmentPoint;

    private static final RandomSource r = RandomSource.create();

    public static void clientTick() {
        var mc = Minecraft.getInstance();
        var target = mc.hitResult;
        if(target.getType() != HitResult.Type.ENTITY)
            return;
        var entityHit = (EntityHitResult) target;
        if(currentEntity != entityHit.getEntity())
            return;

        var segment = getSegment(currentEntity, target.getLocation());
        if(segment != null) {
            int index1, index2, point1, point2;
            if(segment.getA() < firstSegmentIndex) {
                index1 = segment.getA();
                point1 = segment.getB();
                index2 = firstSegmentIndex;
                point2 = firstSegmentPoint;
            } else {
                index1 = firstSegmentIndex;
                index2 = segment.getA();
                if(index1 == index2 && segment.getB() < firstSegmentPoint) {
                    point1 = segment.getB();
                    point2 = firstSegmentPoint;
                } else {
                    point1 = firstSegmentPoint;
                    point2 = segment.getB();
                }
            }
            for(int i = index1; i <= index2; ++i) {
                var wireSegment = currentEntity.segments.get(i);
                Vec3 start = wireSegment.start, end = wireSegment.start.add(wireSegment.vector());
                if(i == index1) {
                    start = wireSegment.start.relative(wireSegment.direction, point1 / 16f);
                }
                if(i == index2) {
                    end = wireSegment.start.relative(wireSegment.direction, point2 / 16f);
                }
                var pos = VecHelper.lerp(r.nextFloat(), start, end).offsetRandom(r, 1 / 16f);
                mc.level.addAlwaysVisibleParticle(new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.5f), 0.5f),
                        pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    private static Tuple<Integer, Integer> getSegment(BlockWireEntity entity, Vec3 hitPos) {
        var localPos = hitPos.subtract(entity.position());
        var thickness = entity.getWireItem().getWireThickness();
        for(int i = 0; i < entity.boundingBoxes.size(); ++i) {
            var bb = entity.boundingBoxes.get(i);
            // Test with slightly larger bounding boxes.
            if(bb.inflate(thickness * 0.2f).contains(localPos)) {
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
                return new Tuple<>(i, segmentPoint);
            }
        }
        return null;
    }

    public static InteractionResult segmentCut(BlockWireEntity entity) {
        var mc = Minecraft.getInstance();
        var target = mc.hitResult;
        if(target.getType() != HitResult.Type.ENTITY)
            return InteractionResult.FAIL;
        if(mc.player.isShiftKeyDown()) {
            // Reset cutting sequence
            currentEntity = null;
            return InteractionResult.SUCCESS;
        }

        if(currentEntity != entity) {
            // First cut.
            var hitPos = target.getLocation();
            var segment = getSegment(entity, hitPos);
            if(segment != null) {
                firstSegmentIndex = segment.getA();
                firstSegmentPoint = segment.getB();
                currentEntity = entity;
            }
            return InteractionResult.CONSUME;
        } else {
            var secondSegment = getSegment(entity, target.getLocation());
            if(secondSegment != null) {
                ModdedPackets.sendToServer(new BlockWireCutC2SPacket(entity, firstSegmentIndex, firstSegmentPoint, secondSegment.getA(), secondSegment.getB()));
                currentEntity = null;
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.FAIL;
    }

    public static InteractionResult attachWire(BlockWireEntity entity) {
        var mc = Minecraft.getInstance();
        var target = mc.hitResult;
        if(target.getType() != HitResult.Type.ENTITY)
            return InteractionResult.FAIL;
        var stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if(entity.getWireItem() != stack.getItem()) {
            mc.player.displayClientMessage(Lang.translate("message.connection_incorrect_wire_type").style(ChatFormatting.RED).component(), true);
            return InteractionResult.FAIL;
        }

        var hitPos = target.getLocation();
        var segment = getSegment(entity, hitPos);
        if(segment == null)
            return InteractionResult.FAIL;

        ModdedPackets.sendToServer(new BlockWireAttachC2SPacket(entity, segment.getA(), segment.getB()));
        return InteractionResult.SUCCESS;
    }
}
