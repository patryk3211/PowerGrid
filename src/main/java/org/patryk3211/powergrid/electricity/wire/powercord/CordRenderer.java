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
package org.patryk3211.powergrid.electricity.wire.powercord;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.wire.CurveParameters;

import static org.patryk3211.powergrid.electricity.wire.HangingWireRenderer.renderSegment;

@Environment(EnvType.CLIENT)
public class CordRenderer extends EntityRenderer<CordEntity> {
    public CordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(CordEntity entity) {
        return entity.getWireItem().getWireTexture();
    }

    @Override
    public void render(CordEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if(entity.renderParams == null)
            return;

        if(entity.isOverheated())
            // Don't render since it's dead and only there to spawn particles.
            return;

        var buffer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        assert entity.renderParams instanceof CurveParameters;
        CurveParameters rp = (CurveParameters) entity.renderParams;

        // To introduce some subtle variety into the wires.
        var thicknessOffset = entity.getId() / 16f;

        int color = entity.getColor() | 0xFF000000;

        var pos = entity.position();
        var world = entity.level();
        rp.runForSegments((x1, y1, z1, x2, y2, z2, offset, length, first, last) -> {
            var blockPos = BlockPos.containing((x1 + x2) * 0.5 + pos.x, (y1 + y2) * 0.5 + pos.y, (z1 + z2) * 0.5 + pos.z);
            var sky = world.getBrightness(LightLayer.SKY, blockPos);
            var block = world.getBrightness(LightLayer.BLOCK, blockPos);
            var currentLight = LightTexture.pack(block, sky);
            if(first) {
                var endpoint = entity.getEndpoint1();
                if(endpoint instanceof SplitCordEndpoint split) {
                    var p1 = split.getEndpoint1().getExactPosition(world);
                    var p2 = split.getEndpoint2().getExactPosition(world);
                    var normal = rp.getNormal();

                    var direction = new Vec3(x2 - p1.x + pos.x, y2 - p1.y + pos.y, z2 - p1.z + pos.z);
                    var v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    var smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    var smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            (float) (p1.x - pos.x), (float) (p1.y - pos.y), (float) (p1.z - pos.z),
                            (float) (x2 - (smallCross1.x + smallCross2.x) * 0.5f + normal.x / 32f),
                            (float) (y2 - (smallCross1.y + smallCross2.y) * 0.5f + normal.y / 32f),
                            (float) (z2 - (smallCross1.z + smallCross2.z) * 0.5f + normal.z / 32f),
                            smallCross1, smallCross2, currentLight, 0xFFB02E26,
                            rp.thickness * 0.5f, thicknessOffset, length * 2, offset);

                    direction = new Vec3(x2 - p2.x + pos.x, y2 - p2.y + pos.y, z2 - p2.z + pos.z);
                    v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            (float) (p2.x - pos.x), (float) (p2.y - pos.y), (float) (p2.z - pos.z),
                            (float) (x2 + (smallCross1.x + smallCross2.x) * 0.5f + normal.x / 32f),
                            (float) (y2 + (smallCross1.y + smallCross2.y) * 0.5f + normal.y / 32f),
                            (float) (z2 + (smallCross1.z + smallCross2.z) * 0.5f + normal.z / 32f),
                            smallCross1, smallCross2, currentLight, 0xFF3C44AA,
                            rp.thickness * 0.5f, thicknessOffset, length * 2, offset);
                    return;
                } else if(endpoint instanceof SocketEndpoint socket) {
                    var state = world.getBlockState(socket.getPosition());
                    var facing = socket.getFacing(world);
                    var model = CachedBuffers.partial(ModdedPartialModels.PLUG, state);
                    float x3 = x1, y3 = y1, z3 = z1;
                    switch(facing) {
                        case NORTH -> z3 -= 3 / 16f;
                        case SOUTH -> z3 += 3 / 16f;
                        case WEST -> x3 -= 3 / 16f;
                        case EAST -> x3 += 3 / 16f;
                        case DOWN -> y3 -= 3 / 16f;
                        case UP -> y3 += 3 / 16f;
                    }
                    model
                            .light(currentLight)
                            .translate(x3, y3, z3)
                            .rotateToFace(facing)
                            .renderInto(matrices, vertexConsumers.getBuffer(RenderType.solid()));
                }
            } else if(last) {
                var endpoint = entity.getEndpoint2();
                if(endpoint instanceof SplitCordEndpoint split) {
                    var p1 = split.getEndpoint1().getExactPosition(world);
                    var p2 = split.getEndpoint2().getExactPosition(world);
                    var normal = rp.getNormal();

                    var direction = new Vec3(x1 - p1.x + pos.x, y1 - p1.y + pos.y, z1 - p1.z + pos.z);
                    var v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    var smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    var smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            (float) (x1 - (smallCross1.x + smallCross2.x) * 0.5f - normal.x / 32f),
                            (float) (y1 - (smallCross1.y + smallCross2.y) * 0.5f - normal.y / 32f),
                            (float) (z1 - (smallCross1.z + smallCross2.z) * 0.5f - normal.z / 32f),
                            (float) (p1.x - pos.x), (float) (p1.y - pos.y), (float) (p1.z - pos.z),
                            smallCross1, smallCross2, currentLight, 0xFFB02E26,
                            rp.thickness * 0.5f, thicknessOffset, length * 2, offset);

                    direction = new Vec3(x1 - p2.x + pos.x, y1 - p2.y + pos.y, z1 - p2.z + pos.z);
                    v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            (float) (x1 + (smallCross1.x + smallCross2.x) * 0.5f - normal.x / 32f),
                            (float) (y1 + (smallCross1.y + smallCross2.y) * 0.5f - normal.y / 32f),
                            (float) (z1 + (smallCross1.z + smallCross2.z) * 0.5f - normal.z / 32f),
                            (float) (p2.x - pos.x), (float) (p2.y - pos.y), (float) (p2.z - pos.z),
                            smallCross1, smallCross2, currentLight, 0xFF3C44AA,
                            rp.thickness * 0.5f, thicknessOffset, length * 2, offset);
                    return;
                } else if(endpoint instanceof SocketEndpoint socket) {
                    var state = world.getBlockState(socket.getPosition());
                    var facing = socket.getFacing(world);
                    var model = CachedBuffers.partial(ModdedPartialModels.PLUG, state);
                    float x3 = x2, y3 = y2, z3 = z2;
                    switch(facing) {
                        case NORTH -> z3 -= 3 / 16f;
                        case SOUTH -> z3 += 3 / 16f;
                        case WEST -> x3 -= 3 / 16f;
                        case EAST -> x3 += 3 / 16f;
                        case DOWN -> y3 -= 3 / 16f;
                        case UP -> y3 += 3 / 16f;
                    }
                    model
                            .light(currentLight)
                            .translate(x3, y3, z3)
                            .rotateToFace(facing)
                            .renderInto(matrices, vertexConsumers.getBuffer(RenderType.solid()));
                }
            }
            renderSegment(matrices, vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),
                    x1, y1, z1,
                    x2, y2, z2,
                    rp.cross1, rp.cross2, currentLight, color,
                    rp.thickness, thicknessOffset, length, offset);
        });
    }
}
