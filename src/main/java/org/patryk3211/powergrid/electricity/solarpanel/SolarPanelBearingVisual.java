package org.patryk3211.powergrid.electricity.solarpanel;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;

import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

import java.util.function.Consumer;

public class SolarPanelBearingVisual<T extends SolarPanelBearingBlockEntity> extends OrientedRotatingVisual<T> implements SimpleDynamicVisual {
    protected OrientedInstance assembly;
    final Axis rotationAxis;
    final Quaternionf blockOrientation;

    public SolarPanelBearingVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick, Direction.SOUTH,
                blockEntity.getBlockState().getValue(SolarPanelBearingBlock.FACING).getOpposite(),
                Models.partial(AllPartialModels.SHAFT_HALF));

        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        rotationAxis = Axis.of(Direction.get(Direction.AxisDirection.POSITIVE,
                blockState.getValue(BlockStateProperties.FACING).getAxis()).step());

        blockOrientation = getBlockStateOrientation(facing);

        PartialModel top = ModdedPartialModels.SOLAR_PANEL_BEARING_ROTOR;

        assembly = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(top))
                .createInstance();

        assembly.position(getVisualPosition())
                .rotation(blockOrientation)
                .setChanged();

    }

    static Quaternionf getBlockStateOrientation(Direction facing) {
        Quaternionf orientation;

        if (facing.getAxis().isHorizontal()) {
            orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()));
        } else {
            orientation = new Quaternionf();
        }

        if (facing.getAxis().isVertical()) {
            orientation.mul(Axis.XP.rotationDegrees(180 + AngleHelper.verticalAngle(facing)));
        } else {
            orientation.mul(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing)));
        }

        return orientation;
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(assembly);
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(assembly);
    }

    @Override
    protected void _delete() {
        super._delete();
        assembly.delete();
    }

    @Override
    public void beginFrame(Context context) {
        float interpolatedAngle = blockEntity.getInterpolatedAngle(context.partialTick() - 1);
        Quaternionf rot = rotationAxis.rotationDegrees(interpolatedAngle);

        rot.mul(blockOrientation);

        assembly.rotation(rot).setChanged();
    }

    public Direction.Axis getRotationAxis() {
        return blockState.getValue(SolarPanelBearingBlock.FACING).getAxis();
    }
}
