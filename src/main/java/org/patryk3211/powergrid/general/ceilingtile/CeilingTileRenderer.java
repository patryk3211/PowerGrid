package org.patryk3211.powergrid.general.ceilingtile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class CeilingTileRenderer extends SafeBlockEntityRenderer<CeilingTileBlockEntity> {
    public CeilingTileRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(CeilingTileBlockEntity be, float partialTicks, PoseStack matrices, MultiBufferSource consumer, int light, int overlay) {
        var bulbState = be.getBulbState();
        if(bulbState == null)
            return;

        var state = be.getBlockState();
        if(bulbState.isBurned())
            return;

        int a = (int) (bulbState.getAlpha() * 255);
        if(a > 0) {
            var vba = consumer.getBuffer(ModdedRenderLayers.getAdditive());
            var lightBuffer = CachedBuffers.partial(ModdedPartialModels.CEILING_LIGHT, state);
            lightBuffer
                    .light(light)
                    .color(a, a, a, 255)
                    .renderInto(matrices, vba);
        }
    }
}
