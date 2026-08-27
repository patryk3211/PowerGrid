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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;

@Environment(EnvType.CLIENT)
public class HangingWireRenderer extends EntityRenderer<HangingWireEntity> {
    public static final boolean RAINBOW_WIRES = false;

    public HangingWireRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(HangingWireEntity entity) {
        return entity.getWireEntry().texture();
    }

    @Override
    public void render(HangingWireEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if(entity.curveParams == null)
            return;

        if(entity.isOverheated())
            // Don't render since it's dead and only there to spawn particles.
            return;
        CurveParameters rp = entity.curveParams;

        // To introduce some subtle variety into the wires.
        var thicknessOffset = entity.getId() / 16f;

        int color = entity.getColor() | 0xFF000000;
        if(RAINBOW_WIRES) {
            var line = GlobalElectricNetworks.getLine(entity);
            if(line != null) {
                color = line.hashCode() | 0xFF000000;
            }
        }

        var pos = entity.position();
        float segmentSize = 0.5f;
        boolean simpleModel;
        if(ModdedConfigs.client().wireLOD.get()) {
            var playerPos = Minecraft.getInstance().player.position();
            if (playerPos.distanceToSqr(pos) > 64 * 64) {
                segmentSize = 3.0f;
                simpleModel = true;
            } else if (playerPos.distanceToSqr(pos) > 32 * 32) {
                segmentSize = 1.5f;
                simpleModel = !Minecraft.useFancyGraphics();
            } else {
                simpleModel = !Minecraft.useFancyGraphics();
            }
        } else {
            simpleModel = !Minecraft.useFancyGraphics();
        }
        VertexConsumer buffer;
        if(!simpleModel) {
            buffer = vertexConsumers.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
        } else {
            buffer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        }
        var world = entity.level();
        rp.runForSegments((x1, y1, z1, x2, y2, z2, offset, length) -> {
            var blockPos = BlockPos.containing((x1 + x2) * 0.5 + pos.x, (y1 + y2) * 0.5 + pos.y, (z1 + z2) * 0.5 + pos.z);
            var sky = world.getBrightness(LightLayer.SKY, blockPos);
            var block = world.getBrightness(LightLayer.BLOCK, blockPos);
            renderSegment(matrices, buffer,
                    x1, y1, z1,
                    x2, y2, z2,
                    rp.cross1, rp.cross2, LightTexture.pack(block, sky), color,
                    rp.thickness, thicknessOffset, (float) length, (float) offset, simpleModel);
        }, segmentSize);
    }

    public static void renderFromPositions(PoseStack matrices, VertexConsumer buffer, Vec3 t1, Vec3 t2, double horizontalCoefficient, double verticalCoefficient, double thickness, int light, int color) {
        double x = (t1.x + t2.x) * 0.5;
        double y = t1.y;
        double z = (t1.z + t2.z) * 0.5;
        var curve = new CurveParameters(t1, t2, horizontalCoefficient, verticalCoefficient, thickness);
        curve.runForSegments((x1, y1, z1, x2, y2, z2, offset, length) ->
                renderSegment(matrices, buffer,
                        x1 + x, y1 + y, z1 + z,
                        x2 + x, y2 + y, z2 + z,
                        curve.cross1, curve.cross2, light, color,
                        curve.thickness, 0, (float) length, (float) offset, !Minecraft.useFancyGraphics()), 0.5f);
    }

    public static void renderFromPositions(PoseStack matrices, VertexConsumer buffer, Vec3 t1, Vec3 t2, double horizontalCoefficient, double verticalCoefficient, double thickness, BlockAndTintGetter lightProvider, int color) {
        double x = (t1.x + t2.x) * 0.5;
        double y = t1.y;
        double z = (t1.z + t2.z) * 0.5;
        var curve = new CurveParameters(t1, t2, horizontalCoefficient, verticalCoefficient, thickness);
        curve.runForSegments((x1, y1, z1, x2, y2, z2, offset, length) -> {
                var blockPos = BlockPos.containing((x1 + x2) * 0.5 + x, (y1 + y2) * 0.5 + y, (z1 + z2) * 0.5 + z);
                var sky = lightProvider.getBrightness(LightLayer.SKY, blockPos);
                var block = lightProvider.getBrightness(LightLayer.BLOCK, blockPos);
                renderSegment(matrices, buffer,
                        x1 + x, y1 + y, z1 + z,
                        x2 + x, y2 + y, z2 + z,
                        curve.cross1, curve.cross2, LightTexture.pack(block, sky), color,
                        curve.thickness, 0, (float) length, (float) offset, !Minecraft.useFancyGraphics());
        }, 0.5f);
    }

    public static void renderSegment(PoseStack ms, VertexConsumer buffer,
                                     double x1, double y1, double z1, double x2, double y2, double z2,
                                     Vec3 cross1, Vec3 cross2, int light, int color,
                                     float thickness, float thicknessOffset, float uvLength, float lengthOffset, boolean simpleModel) {
        if(simpleModel) {
            quad(ms.last(), buffer, light, color,
                    x1 + cross1.x, y1 + cross1.y, z1 + cross1.z,
                    x1 - cross1.x, y1 - cross1.y, z1 - cross1.z,
                    x2 + cross1.x, y2 + cross1.y, z2 + cross1.z,
                    x2 - cross1.x, y2 - cross1.y, z2 - cross1.z,
                    thickness, thicknessOffset, uvLength, lengthOffset);
            quad(ms.last(), buffer, light, color,
                    x1 + cross2.x, y1 + cross2.y, z1 + cross2.z,
                    x1 - cross2.x, y1 - cross2.y, z1 - cross2.z,
                    x2 + cross2.x, y2 + cross2.y, z2 + cross2.z,
                    x2 - cross2.x, y2 - cross2.y, z2 - cross2.z,
                    thickness, thicknessOffset, uvLength, lengthOffset);
        } else {
            quad(ms.last(), buffer, light, color,
                    x1 + cross1.x, y1 + cross1.y, z1 + cross1.z,
                    x1 - cross2.x, y1 - cross2.y, z1 - cross2.z,
                    x2 + cross1.x, y2 + cross1.y, z2 + cross1.z,
                    x2 - cross2.x, y2 - cross2.y, z2 - cross2.z,
                    cross1.x - cross2.x, cross1.y - cross2.y, cross1.z - cross2.z,
                    thickness, thicknessOffset, uvLength, lengthOffset);
            quad(ms.last(), buffer, light, color,
                    x1 - cross1.x, y1 - cross1.y, z1 - cross1.z,
                    x1 + cross2.x, y1 + cross2.y, z1 + cross2.z,
                    x2 - cross1.x, y2 - cross1.y, z2 - cross1.z,
                    x2 + cross2.x, y2 + cross2.y, z2 + cross2.z,
                    cross2.x - cross1.x, cross2.y - cross1.y, cross2.z - cross1.z,
                    thickness, thicknessOffset, uvLength, lengthOffset);
            quad(ms.last(), buffer, light, color,
                    x1 + cross2.x, y1 + cross2.y, z1 + cross2.z,
                    x1 + cross1.x, y1 + cross1.y, z1 + cross1.z,
                    x2 + cross2.x, y2 + cross2.y, z2 + cross2.z,
                    x2 + cross1.x, y2 + cross1.y, z2 + cross1.z,
                    cross1.x + cross2.x, cross1.y + cross2.y, cross1.z + cross2.z,
                    thickness, thicknessOffset, uvLength, lengthOffset);
            quad(ms.last(), buffer, light, color,
                    x1 - cross2.x, y1 - cross2.y, z1 - cross2.z,
                    x1 - cross1.x, y1 - cross1.y, z1 - cross1.z,
                    x2 - cross2.x, y2 - cross2.y, z2 - cross2.z,
                    x2 - cross1.x, y2 - cross1.y, z2 - cross1.z,
                    -cross1.x - cross2.x, -cross1.y - cross2.y, -cross1.z - cross2.z,
                    thickness, thicknessOffset, uvLength, lengthOffset);
            quad(ms.last(), buffer, light, color,
                    x1 + cross1.x, y1 + cross1.y, z1 + cross1.z,
                    x1 + cross2.x, y1 + cross2.y, z1 + cross2.z,
                    x1 - cross2.x, y1 - cross2.y, z1 - cross2.z,
                    x1 - cross1.x, y1 - cross1.y, z1 - cross1.z,
                    x1 - x2, y1 - y2, z1 - z2,
                    thickness, thicknessOffset, thickness, thicknessOffset);
            quad(ms.last(), buffer, light, color,
                    x2 + cross1.x, y2 + cross1.y, z2 + cross1.z,
                    x2 - cross2.x, y2 - cross2.y, z2 - cross2.z,
                    x2 + cross2.x, y2 + cross2.y, z2 + cross2.z,
                    x2 - cross1.x, y2 - cross1.y, z2 - cross1.z,
                    x2 - x1, y2 - y1, z2 - z1,
                    thickness, thicknessOffset, thickness, thicknessOffset);
        }
    }

    public static void quad(PoseStack.Pose matrix, VertexConsumer buffer, int light, int color,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4,
                            float vLen, float v0, float uLen, float u0) {
        buffer.vertex(matrix.pose(), (float) x1, (float) y1, (float) z1)
                .color(color)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), 0, 1, 0)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x2, (float) y2, (float) z2)
                .color(color)
                .uv(u0, v0 + vLen)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), 0, 1, 0)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x4, (float) y4, (float) z4)
                .color(color)
                .uv(u0 + uLen, v0 + vLen)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), 0, 1, 0)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x3, (float) y3, (float) z3)
                .color(color)
                .uv(u0 + uLen, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), 0, 1, 0)
                .endVertex();
    }

    public static void quad(PoseStack.Pose matrix, VertexConsumer buffer, int light, int color,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4,
                            double nX, double nY, double nZ,
                            float vLen, float v0, float uLen, float u0) {
        var nLen = Math.sqrt(nX * nX + nY * nY + nZ * nZ);
        nX /= nLen; nY /= nLen; nZ /= nLen;
        buffer.vertex(matrix.pose(), (float) x1, (float) y1, (float) z1)
                .color(color)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), (float) nX, (float) nY, (float) nZ)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x2, (float) y2, (float) z2)
                .color(color)
                .uv(u0, v0 + vLen)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), (float) nX, (float) nY, (float) nZ)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x4, (float) y4, (float) z4)
                .color(color)
                .uv(u0 + uLen, v0 + vLen)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), (float) nX, (float) nY, (float) nZ)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x3, (float) y3, (float) z3)
                .color(color)
                .uv(u0 + uLen, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(matrix.normal(), (float) nX, (float) nY, (float) nZ)
                .endVertex();
    }

    public static void quad(PoseStack.Pose matrix, VertexConsumer buffer, int color,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4) {
        buffer.vertex(matrix.pose(), (float) x1, (float) y1, (float) z1)
                .color(color)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x2, (float) y2, (float) z2)
                .color(color)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x4, (float) y4, (float) z4)
                .color(color)
                .endVertex();
        buffer.vertex(matrix.pose(), (float) x3, (float) y3, (float) z3)
                .color(color)
                .endVertex();
    }
}
