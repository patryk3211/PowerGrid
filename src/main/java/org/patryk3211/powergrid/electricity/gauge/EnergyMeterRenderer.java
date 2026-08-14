package org.patryk3211.powergrid.electricity.gauge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class EnergyMeterRenderer extends SafeBlockEntityRenderer<EnergyMeterBlockEntity> {

    public EnergyMeterRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    protected void renderSafe(EnergyMeterBlockEntity be, float pt, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        int index = 4;
        var state = be.getBlockState();
        var facing = state.getValue(EnergyMeterBlock.HORIZONTAL_FACING);
        var consumer = bufferSource.getBuffer(RenderType.solid());
        for(int i = 10000; i >= 1; i /= 10) {
            float angle = EnergyMeterScreen.getDialAngle(be.energy, be.lastEnergy, i, pt);
            var model = CachedBuffers.partial(ModdedPartialModels.ENERGY_METER_NEEDLE, state);

            double y = index % 2 == 0 ? 11.5 / 16 : 7.5 / 16;
            double x = (12.0 - index * 2) / 16;

            model
                    .rotateYCenteredDegrees(-facing.toYRot() - 180)
                    .translate(x, y, 0)
                    .rotateZ(-angle)
                    .light(light)
                    .renderInto(ms, consumer);
            --index;
        }
    }
}
