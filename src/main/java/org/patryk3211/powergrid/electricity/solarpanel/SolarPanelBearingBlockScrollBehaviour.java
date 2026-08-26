package org.patryk3211.powergrid.electricity.solarpanel;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.List;

public class SolarPanelBearingBlockScrollBehaviour extends ScrollValueBehaviour {
    public static final BehaviourType<SolarPanelBearingBlockScrollBehaviour> TYPE = new BehaviourType<>();
    private List<Integer> divisors = new ArrayList<>(List.of(1));
    public int panelCount = 1;

    public SolarPanelBearingBlockScrollBehaviour(SmartBlockEntity be) {
        super(Lang.translateDirect("gui.solar_panel_bearing.slider"), be, new BearingFaceBox());
        between(0, 0);
        value = 0;
        withFormatter(this::formatIndex);
    }

    public int getPanelCount() {
        return panelCount;
    }

    public void refreshDivisors(int panelCount) {
        this.panelCount = panelCount;
        divisors = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (panelCount % i == 0 && (panelCount / i) <= 25)
                divisors.add(i);
        }
        int maxIdx = Math.max(0, divisors.size() - 1);
        between(0, maxIdx);
        if (value > maxIdx)
            value = maxIdx;
    }

    public int getDivisor() {
        if (divisors.isEmpty()) return 1;
        int idx = Math.max(0, Math.min(value, divisors.size() - 1));
        return divisors.get(idx);
    }

    public void setByDivisor(int savedDivisor) {
        for (int i = 0; i < divisors.size(); i++) {
            if (divisors.get(i) == savedDivisor) {
                value = i;
                return;
            }
        }
        value = 0;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        DivisorOption[] options = buildOptions();
        ValueSettingsFormatter fmt = new ValueSettingsFormatter.ScrollOptionSettingsFormatter(options);
        return new ValueSettingsBoard(label, Math.max(0, options.length - 1),
                1, ImmutableList.of(Component.empty()), fmt
        );
    }

    private DivisorOption[] buildOptions() {
        DivisorOption[] options = new DivisorOption[divisors.size()];
        for (int i = 0; i < divisors.size(); i++) {
            int divisor  = divisors.get(i);
            int perString = panelCount / divisor;
            options[i] = new DivisorOption(divisor + " × " + perString);
        }
        return options;
    }

    private static class DivisorOption implements INamedIconOptions {
        private final String label;

        DivisorOption(String label) {
            this.label = label;
        }

        @Override
        public AllIcons getIcon() {
            return AllIcons.I_NONE;
        }

        @Override
        public String getTranslationKey() {
            return label; // not a real translation just some jank to get text to show up where it normally shouldn't
        }
    }

    private String formatIndex(int idx) {
        if (divisors.isEmpty() || panelCount == 0) return "1 group";
        int divisor  = divisors.get(Math.max(0, Math.min(idx, divisors.size() - 1)));
        int perString = panelCount / divisor;
        return divisor + " × " + perString;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    static class BearingFaceBox extends ValueBoxTransform.Sided {

        @Override
        public float getScale() {
            return .4f;
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction side) {
            if (!state.hasProperty(SolarPanelBearingBlock.FACING))
                return false;
            var facing = state.getValue(SolarPanelBearingBlock.FACING);
            if (state.getValue(SolarPanelBearingBlock.FACING).getAxis() == Direction.Axis.Y) {
                if (getSide().equals(Direction.EAST)) return true;
                return getSide().equals(Direction.WEST);
            } else {
                if (getSide().equals(facing.getClockWise())) return true;
                return getSide().equals(facing.getCounterClockWise());
            }
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction side = getSide();
            Direction facing = state.getValue(SolarPanelBearingBlock.FACING);

            float roll = 0;
            for (Pointing p : Pointing.values())
                if (p.getCombinedDirection(facing) == side)
                    roll = p.getXRotation();
            if (facing == Direction.UP)
                roll += 180;

            float horizontalAngle = AngleHelper.horizontalAngle(facing);
            float verticalAngle = AngleHelper.verticalAngle(facing);
            Vec3 local = VecHelper.voxelSpace(8, 14.5, 9);

            local = VecHelper.rotateCentered(local, roll, Direction.Axis.Z);
            local = VecHelper.rotateCentered(local, horizontalAngle, Direction.Axis.Y);
            local = VecHelper.rotateCentered(local, verticalAngle, Direction.Axis.X);

            return local;
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(SolarPanelBearingBlock.FACING);

            if (facing.getAxis() == Direction.Axis.Y) {
                super.rotate(level, pos, state, ms);
                return;
            }

            float roll = 0;
            for (Pointing p : Pointing.values())
                if (p.getCombinedDirection(facing) == getSide())
                    roll = p.getXRotation();

            float yRot = AngleHelper.horizontalAngle(facing) + (facing == Direction.DOWN ? 180 : 0);
            TransformStack.of(ms)
                    .rotateYDegrees(yRot)
                    .rotateYDegrees(roll);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return Vec3.ZERO;
        }
    }
}