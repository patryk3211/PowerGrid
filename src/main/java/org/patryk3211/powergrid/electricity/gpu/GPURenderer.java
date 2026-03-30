package org.patryk3211.powergrid.electricity.gpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class GPURenderer extends SafeBlockEntityRenderer<GPUBlockEntity> {
    public GPURenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(GPUBlockEntity be, float pt, PoseStack ms, MultiBufferSource source, int light, int overlay) {
        var state = be.getBlockState();
        var model = CachedBuffers.partialFacing(ModdedPartialModels.GPU_FAN, state, state.getValue(GPUBlock.HORIZONTAL_FACING));

        float angle = (float) Math.PI;
        angle += Mth.lerp(pt, be.anglePrev, be.angle);
        model.light(light)
                .rotateYCentered(angle)
                .renderInto(ms, source.getBuffer(RenderType.solid()));
    }
}
