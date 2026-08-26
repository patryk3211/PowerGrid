package org.patryk3211.powergrid.electricity.solarpanel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class SolarPanelBearingRenderer<T extends KineticBlockEntity & IBearingBlockEntity> extends KineticBlockEntityRenderer<T> {
    public SolarPanelBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        final Direction facing = be.getBlockState()
                .getValue(SolarPanelBearingBlock.FACING);
        PartialModel top = ModdedPartialModels.SOLAR_PANEL_BEARING_ROTOR;
        SuperByteBuffer superBuffer = CachedBuffers.partial(top, be.getBlockState());

        float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1);
        kineticRotationTransform(superBuffer, be, facing.getAxis(), (float) (interpolatedAngle / 180 * Math.PI), light);

        if (facing.getAxis()
                .isHorizontal())
            superBuffer.rotateCentered(
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
        if (be.getBlockState().getValue(SolarPanelBearingBlock.FACING).getAxis().isVertical()){
            if (facing == Direction.UP){
                superBuffer.rotateCentered(AngleHelper.rad( -90 + AngleHelper.verticalAngle(facing)), Direction.EAST);
            } else {
                superBuffer.rotateCentered(AngleHelper.rad( 90 + AngleHelper.verticalAngle(facing)), Direction.EAST);
            }
        } else {
            superBuffer.rotateCentered(AngleHelper.rad(AngleHelper.verticalAngle(facing)), Direction.EAST);
        }
        superBuffer.rotateCentered(AngleHelper.rad(AngleHelper.verticalAngle(facing)), Direction.EAST);
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(KineticBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, state
                .getValue(SolarPanelBearingBlock.FACING)
                .getOpposite());
    }
}
