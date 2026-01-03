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
package org.patryk3211.powergrid.kinetics.plotter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class PlotterRenderer extends KineticBlockEntityRenderer<PlotterBlockEntity> {
    private static final float TIME_SPAN = 9.5f / 16;
    private static final float VOLTAGE_SPAN = 11.5f / 16;
    private static final Vec3 GRAPH_ORIGIN = new Vec3(8.0 / 16.0, 16.2 / 16.0, 13.0 / 16.0);

    private static final Quaternionf GRAPH_ROTATION = new Quaternionf()
            .rotateX(-112.5f / 180f * (float) Math.PI);

    public PlotterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static Vec3 graphPoint(float t, float v, BlockPos pos, Direction facing) {
        var dest = new Vector3d();
        GRAPH_ROTATION.transform(v * VOLTAGE_SPAN * 0.5f, t * TIME_SPAN, 0, dest);
        var out = new Vec3(
                dest.x + GRAPH_ORIGIN.x,
                dest.y + GRAPH_ORIGIN.y,
                dest.z + GRAPH_ORIGIN.z);
        out = switch(facing) {
            case NORTH -> out;
            case SOUTH -> new Vec3(1 - out.x, out.y, 1 - out.z);
            case EAST -> new Vec3(1 - out.z, out.y, out.x);
            case WEST -> new Vec3(out.z, out.y, 1 - out.x);
            default -> Vec3.ZERO;
        };
        return out.add(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected void renderSafe(PlotterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        var state = be.getBlockState();
        var facing = state.getValue(PlotterBlock.HORIZONTAL_FACING);
        var pointer = CachedBuffers.partial(ModdedPartialModels.PLOTTER_POINTER, state);
        var paper = CachedBuffers.partialFacing(ModdedPartialModels.PLOTTER_PAPER, state, facing.getOpposite());

        pointer
                .light(light)
                .center()
                .rotateToFace(facing)
                .uncenter()
                .translate(Mth.lerp(partialTicks, be.prevHeadPosition, be.headPosition) * VOLTAGE_SPAN * 0.5f, 0, 0)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
        var sprite = ModdedPartialModels.PAPER_SHIFT;
        float spriteWidth = sprite.getTarget().getU1() - sprite.getTarget().getU0();
        double uScroll = be.getAnimationSpeed() * AnimationTickHolder.getRenderTime(be.getLevel()) / (256 * 20) * TIME_SPAN;
        uScroll = uScroll - Math.floor(uScroll);
        uScroll = uScroll * spriteWidth / 2;
        paper
                .light(light)
                .translate(0, 1 / 64f, 0)
                .shiftUVScrolling(sprite, (float) uScroll, 0)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));

        // Render the graph from samples
        ms.pushPose();
        ms.rotateAround(new Quaternionf().rotateY(0.5f * (float) Math.PI * (2 - facing.get2DDataValue())), 0.5f, 0.5f, 0.5f);
        ms.rotateAround(GRAPH_ROTATION, (float) GRAPH_ORIGIN.x, (float) GRAPH_ORIGIN.y, (float) GRAPH_ORIGIN.z);
        ms.translate(GRAPH_ORIGIN.x, GRAPH_ORIGIN.y, GRAPH_ORIGIN.z);
        ms.scale(VOLTAGE_SPAN * 0.5f, TIME_SPAN, 1);
        if(be.getAnimationSpeed() != 0) {
            // This keeps the graph movement in sync with the paper scrolling.
            ms.translate(0, -1.0f / be.sampleBuffer.length * partialTicks, 0);
        }
        float yPrev = 0;
        var consumer = buffer.getBuffer(ModdedRenderLayers.getColor());

        var color = be.color.getTextureDiffuseColors();
        consumer.defaultColor((int) (color[0] * 160) + 16, (int) (color[1] * 160) + 16, (int) (color[2] * 160) + 16, 255);
        var m4 = ms.last().pose();
        for(int i = 0; i < be.sampleBuffer.length; ++i) {
            float y = Mth.clamp(be.sampleBuffer[(be.head + i) % be.sampleBuffer.length], -1, 1);
            if(i == 0) {
                yPrev = y;
                continue;
            }
            float x1 = (float) (i - 1) / be.sampleBuffer.length;
            float x2 = (float) i / be.sampleBuffer.length;

            float thickness = 1 / 32f;
            var diff = Math.abs(y - yPrev) * 0.5f;
            thickness += diff;

            float yMid = (yPrev + y) * 0.5f;
            y = yPrev = yMid;
            consumer.vertex(m4, yPrev - thickness, x1, 0).endVertex();
            consumer.vertex(m4, yPrev + thickness, x1, 0).endVertex();
            consumer.vertex(m4, y + thickness, x2, 0).endVertex();
            consumer.vertex(m4, y - thickness, x2, 0).endVertex();

            yPrev = y;
        }
        consumer.unsetDefaultColor();
        ms.popPose();
    }

    @Override
    protected BlockState getRenderedBlockState(PlotterBlockEntity be) {
        return shaft(be.getBlockState().getValue(PlotterBlock.HORIZONTAL_FACING).getClockWise().getAxis());
    }
}
