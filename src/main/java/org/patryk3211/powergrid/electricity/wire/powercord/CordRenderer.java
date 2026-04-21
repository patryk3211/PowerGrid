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
import dev.ryanhcode.sable.companion.SableCompanion;
import net.createmod.catnip.render.CachedBuffers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
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
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.wire.CurveParameters;
import org.patryk3211.powergrid.electricity.wire.registry.WireItemEntry;

import static org.patryk3211.powergrid.electricity.wire.HangingWireRenderer.lodLevel;
import static org.patryk3211.powergrid.electricity.wire.HangingWireRenderer.renderSegment;

@Environment(EnvType.CLIENT)
public class CordRenderer<T extends CordEntity> extends EntityRenderer<T> {
    public CordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getWireEntry().texture();
    }

    private static void renderPlug(PoseStack matrices, MultiBufferSource consumers, BlockState referenceState, Direction facing, double x, double y, double z, int light) {
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
                                     double x1, double y1, double z1, double x2, double y2, double z2,
                                     double offset, double length, boolean first, boolean last, int light) { }

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

        var world = entity.level();
        var rawPos = entity.position();
        var pos = SableCompanion.INSTANCE.projectOutOfSubLevel(world, rawPos);
        final var playerPos = ModdedConfigs.client().wireLOD.get() ? Minecraft.getInstance().player.position() : null;
        float segmentSize = 0.5f;
        final boolean simpleModel;
        switch(lodLevel(playerPos, rawPos.subtract(pos), rawPos, entity.terminalPos1, entity.terminalPos2)) {
            case 1 -> {
                segmentSize = 1.5f;
                simpleModel = !Minecraft.useFancyGraphics();
            }
            case 2 -> {
                segmentSize = 3.0f;
                simpleModel = true;
            }
            default -> simpleModel = !Minecraft.useFancyGraphics();
        }
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
                            p1.x - pos.x, p1.y - pos.y, p1.z - pos.z,
                            x2 - (smallCross1.x + smallCross2.x) * 0.5 + normal.x / 32,
                            y2 - (smallCross1.y + smallCross2.y) * 0.5 + normal.y / 32,
                            z2 - (smallCross1.z + smallCross2.z) * 0.5 + normal.z / 32,
                            smallCross1, smallCross2, currentLight, 0xFFB02E26,
                            rp.thickness * 0.5f, thicknessOffset, (float) (length * 2), (float) offset, simpleModel);

                    direction = new Vec3(x2 - p2.x + pos.x, y2 - p2.y + pos.y, z2 - p2.z + pos.z);
                    v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            p2.x - pos.x, p2.y - pos.y, p2.z - pos.z,
                            x2 + (smallCross1.x + smallCross2.x) * 0.5 + normal.x / 32,
                            y2 + (smallCross1.y + smallCross2.y) * 0.5 + normal.y / 32,
                            z2 + (smallCross1.z + smallCross2.z) * 0.5 + normal.z / 32,
                            smallCross1, smallCross2, currentLight, 0xFF3C44AA,
                            rp.thickness * 0.5f, thicknessOffset, (float) (length * 2), (float) offset, simpleModel);
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
                            x1 - (smallCross1.x + smallCross2.x) * 0.5f - normal.x / 32f,
                            y1 - (smallCross1.y + smallCross2.y) * 0.5f - normal.y / 32f,
                            z1 - (smallCross1.z + smallCross2.z) * 0.5f - normal.z / 32f,
                            p1.x - pos.x, p1.y - pos.y, p1.z - pos.z,
                            smallCross1, smallCross2, currentLight, 0xFFB02E26,
                            rp.thickness * 0.5f, thicknessOffset, (float) (length * 2), (float) offset, simpleModel);

                    direction = new Vec3(x1 - p2.x + pos.x, y1 - p2.y + pos.y, z1 - p2.z + pos.z);
                    v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            x1 + (smallCross1.x + smallCross2.x) * 0.5f - normal.x / 32f,
                            y1 + (smallCross1.y + smallCross2.y) * 0.5f - normal.y / 32f,
                            z1 + (smallCross1.z + smallCross2.z) * 0.5f - normal.z / 32f,
                            p2.x - pos.x, p2.y - pos.y, p2.z - pos.z,
                            smallCross1, smallCross2, currentLight, 0xFF3C44AA,
                            rp.thickness * 0.5f, thicknessOffset, (float) (length * 2), (float) offset, simpleModel);
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
                    rp.thickness, thicknessOffset, (float) length, (float) offset, simpleModel);
        }, segmentSize);
    }

    public static void renderPreview(ICordEndpoint start, Vec3 end, PoseStack matrices, MultiBufferSource vertexConsumers, Level level, WireItemEntry item, int color, Vec3 cameraPos) {
        var buffer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(item.texture()));
        var startPos = start.getExactPosition(level);
        CurveParameters rp = new CurveParameters(startPos, end, item.horizontalCoefficient(), item.verticalCoefficient(), item.wireThickness());

        // To introduce some subtle variety into the wires.
        var thicknessOffset = 0;
        matrices.translate(startPos.x - cameraPos.x, startPos.y - cameraPos.y, startPos.z - cameraPos.z);

        end = end.subtract(startPos);
        var pos = new Vec3(
                end.x * 0.5f,
                0.0f,
                end.z * 0.5f
        );
        matrices.translate(pos.x, pos.y, pos.z);
        rp.runForSegments((x1, y1, z1, x2, y2, z2, offset, length, first, last) -> {
            var currentLight = LightTexture.FULL_BRIGHT;
            if(first) {
                if(start instanceof SplitCordEndpoint split) {
                    var p1 = split.getEndpoint1().getExactPosition(level).subtract(startPos);
                    var p2 = split.getEndpoint2().getExactPosition(level).subtract(startPos);
                    var normal = rp.getNormal();

                    var direction = new Vec3(x2 - p1.x + pos.x, y2 - p1.y + pos.y, z2 - p1.z + pos.z);
                    var v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    var smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    var smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            p1.x - pos.x, p1.y - pos.y, p1.z - pos.z,
                            x2 - (smallCross1.x + smallCross2.x) * 0.5f + normal.x / 32f,
                            y2 - (smallCross1.y + smallCross2.y) * 0.5f + normal.y / 32f,
                            z2 - (smallCross1.z + smallCross2.z) * 0.5f + normal.z / 32f,
                            smallCross1, smallCross2, currentLight, 0xFFB02E26,
                            rp.thickness * 0.5f, thicknessOffset, (float) (length * 2), (float) offset, !Minecraft.useFancyGraphics());

                    direction = new Vec3(x2 - p2.x + pos.x, y2 - p2.y + pos.y, z2 - p2.z + pos.z);
                    v1 = new Vec3(1 - direction.x, 1 - direction.y, 1 - direction.z);
                    smallCross1 = v1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    smallCross2 = smallCross1.cross(direction).normalize().scale(rp.thickness * 0.25);
                    renderSegment(matrices, buffer,
                            p2.x - pos.x, p2.y - pos.y, p2.z - pos.z,
                            x2 + (smallCross1.x + smallCross2.x) * 0.5f + normal.x / 32f,
                            y2 + (smallCross1.y + smallCross2.y) * 0.5f + normal.y / 32f,
                            z2 + (smallCross1.z + smallCross2.z) * 0.5f + normal.z / 32f,
                            smallCross1, smallCross2, currentLight, 0xFF3C44AA,
                            rp.thickness * 0.5f, thicknessOffset, (float) (length * 2), (float) offset, !Minecraft.useFancyGraphics());
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
            renderSegment(matrices, vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(item.texture())),
                    x1, y1, z1,
                    x2, y2, z2,
                    rp.cross1, rp.cross2, currentLight, color,
                    rp.thickness, thicknessOffset, (float) length, (float) offset, !Minecraft.useFancyGraphics());
        }, 0.5f);
    }
}
