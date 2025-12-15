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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.wire.CurveParameters;

import static org.patryk3211.powergrid.electricity.wire.HangingWireRenderer.renderSegment;

@Environment(EnvType.CLIENT)
public class CordRenderer<T extends CordEntity> extends EntityRenderer<T> {
    public CordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getWireItem().getWireTexture();
    }

    private static void renderPlug(PoseStack matrices, MultiBufferSource consumers, BlockState referenceState, Direction facing, float x, float y, float z, int light) {
        var model = CachedBuffers.partial(ModdedPartialModels.PLUG, referenceState);
        switch (facing) {
            case NORTH -> z -= 3 / 16f;
            case SOUTH -> z += 3 / 16f;
            case WEST -> x -= 3 / 16f;
            case EAST -> x += 3 / 16f;
            case DOWN -> y -= 3 / 16f;
            case UP -> y += 3 / 16f;
        }
        model
                .light(light)
                .translate(x, y, z)
                .rotateToFace(facing)
                .renderInto(matrices, consumers.getBuffer(RenderType.solid()));
    }

    protected void segmentRenderHook(T entity, PoseStack matrices, MultiBufferSource vertexConsumers,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     float offset, float length, boolean first, boolean last, int light) { }

    @Override
    public void render(T entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if(entity.renderParams == null)
            return;

        if(entity.isOverheated())
            // Don't render since it's dead and only there to spawn particles.
            return;

        assert entity.renderParams instanceof CurveParameters;
        CurveParameters rp = (CurveParameters) entity.renderParams;

        // To introduce some subtle variety into the wires.
        var thicknessOffset = entity.getId() / 16f;

        int color = entity.getColor() | 0xFF000000;

        var pos = entity.position();
        var world = entity.level();
        rp.runForSegments((x1, y1, z1, x2, y2, z2, offset, length, first, last) -> {
            var buffer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
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
                    renderPlug(matrices, vertexConsumers,
                            world.getBlockState(socket.getPosition()),
                            socket.getFacing(world), x1, y1, z1, currentLight);
                } else if(endpoint instanceof AutoCordEndpoint auto) {
                    var facing = auto.getPlugFacing();
                    if(facing != null) {
                        renderPlug(matrices, vertexConsumers,
                                world.getBlockState(auto.getPosition()),
                                facing.getOpposite(), x1, y1, z1, currentLight);
                    }
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
                    renderPlug(matrices, vertexConsumers,
                            world.getBlockState(socket.getPosition()),
                            socket.getFacing(world), x2, y2, z2, currentLight);
                } else if(endpoint instanceof AutoCordEndpoint auto) {
                    var facing = auto.getPlugFacing();
                    if(facing != null) {
                        renderPlug(matrices, vertexConsumers,
                                world.getBlockState(auto.getPosition()),
                                facing.getOpposite(), x2, y2, z2, currentLight);
                    }
                }
            }
            segmentRenderHook(entity, matrices, vertexConsumers, x1, y1, z1, x2, y2, z2, offset, length, first, last, currentLight);
            renderSegment(matrices, vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),
                    x1, y1, z1,
                    x2, y2, z2,
                    rp.cross1, rp.cross2, currentLight, color,
                    rp.thickness, thicknessOffset, length, offset);
        });
    }

    public static void renderPreview(ICordEndpoint start, Vec3 end, PoseStack matrices, MultiBufferSource vertexConsumers, Level level, CordItem item, int color) {
        var buffer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(item.getWireTexture()));
        var startPos = start.getExactPosition(level);
        CurveParameters rp = new CurveParameters(startPos, end, item.getHorizontalCoefficient(), item.getVerticalCoefficient(), item.getWireThickness());

        // To introduce some subtle variety into the wires.
        var thicknessOffset = 0;

        matrices.pushPose();
        var pos = new Vec3(
                (startPos.x + end.x) * 0.5f,
                startPos.y,
                (startPos.z + end.z) * 0.5f
        );
        matrices.translate(pos.x, pos.y, pos.z);
        rp.runForSegments((x1, y1, z1, x2, y2, z2, offset, length, first, last) -> {
            var currentLight = LightTexture.FULL_BRIGHT;
            if(first) {
                if(start instanceof SplitCordEndpoint split) {
                    var p1 = split.getEndpoint1().getExactPosition(level);
                    var p2 = split.getEndpoint2().getExactPosition(level);
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
                } else if(start instanceof SocketEndpoint socket) {
                    renderPlug(matrices, vertexConsumers,
                            level.getBlockState(socket.getPosition()),
                            socket.getFacing(level), x1, y1, z1, currentLight);
                } else if(start instanceof AutoCordEndpoint auto) {
                    var facing = auto.getPlugFacing();
                    if(facing != null) {
                        renderPlug(matrices, vertexConsumers,
                                level.getBlockState(auto.getPosition()),
                                facing.getOpposite(), x1, y1, z1, currentLight);
                    }
                }
            }
            renderSegment(matrices, vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(item.getWireTexture())),
                    x1, y1, z1,
                    x2, y2, z2,
                    rp.cross1, rp.cross2, currentLight, color,
                    rp.thickness, thicknessOffset, length, offset);
        });
        matrices.popPose();
    }
}
