package org.patryk3211.powergrid.circuits.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class CircuitDesignTableRenderer extends SafeBlockEntityRenderer<CircuitDesignTableBlockEntity> {
    public CircuitDesignTableRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    protected void renderSafe(CircuitDesignTableBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        float power = Mth.clamp((be.power() - 15) / 15, 0, 1);

        var state = be.getBlockState();
        var glow = CachedBuffers.partial(ModdedPartialModels.CIRCUIT_TABLE_GLOW, state);

        int a = (int) (power * 192);
        glow.rotateYCenteredDegrees(state.getValue(CircuitDesignTableBlock.HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? 90 : 0)
                .disableDiffuse()
                .color(a, a, a, 255)
                .renderInto(ms, bufferSource.getBuffer(ModdedRenderLayers.getAdditive()));
    }
}
