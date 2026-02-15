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

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.gauge.GaugeValueBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;
import org.patryk3211.powergrid.utility.ClientSideAccess;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class PlotterBlockEntity extends ElectricKineticBlockEntity {
    private static final float[] MAX_VALUES = new float[] { 2, 20, 200, 2000 };

    private GaugeValueBehaviour gaugeValue;
    private float maxValue = MAX_VALUES[0];

    private ElectricWire wire;
    protected float[] sampleBuffer = new float[40];
    protected int head;

    protected float headTarget = 0;
    protected float headPosition = 0;
    protected float prevHeadPosition = 0;
    @NotNull
    protected DyeColor color = DyeColor.BLACK;

    public PlotterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        gaugeValue = new GaugeValueBehaviour(Component.translatable("powergrid.devices.gauge.voltage"),
                Unit.VOLTAGE.get().component(), MAX_VALUES, this, new BoxTransform());
        gaugeValue.withCallback(i -> {
            maxValue = MAX_VALUES[i];
            sendData();
        });
        behaviours.add(gaugeValue);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        // 20 kilo-ohm "impedance".
        builder.setTerminalCount(2);
        wire = builder.connect(20e3f, builder.terminalNode(0), builder.terminalNode(1));
    }

    public float getAnimationSpeed() {
        if(!isSpeedRequirementFulfilled())
            return 0;
        return -Math.abs(getSpeed());
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if(!isSpeedRequirementFulfilled())
            return;
        var old = sampleBuffer;
        sampleBuffer = new float[(int) (128 / Math.abs(getSpeed()) * 40)];
        for(int i = 0; i < sampleBuffer.length; ++i) {
            float oldI = (float) i / sampleBuffer.length * old.length;
            int i0 = Mth.clamp(Mth.floor(oldI), 0, old.length - 1);
            int i1 = Mth.clamp(Mth.ceil(oldI), 0, old.length - 1);
            float s0 = old[i0];
            float s1 = old[i1];
            float weight = oldI - i0;
            sampleBuffer[i] = s0 * (1.0f - weight) + s1 * weight;
        }
        head = (int)((float) head / old.length * sampleBuffer.length);
        if(head < 0) head = 0;
        if(head >= sampleBuffer.length) head = sampleBuffer.length - 1;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        var samples = compound.getList("Samples", Tag.TAG_FLOAT);
        sampleBuffer = new float[samples.size()];
        for(int i = 0; i < sampleBuffer.length; ++i) {
            sampleBuffer[i] = samples.getFloat(i);
        }
        head = compound.getInt("Head");
        maxValue = MAX_VALUES[gaugeValue.getValue()];
        color = DyeColor.values()[compound.getInt("Color")];
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putInt("Color", color.ordinal());
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        var samples = new ListTag();
        for(int i = 0; i < sampleBuffer.length; ++i) {
            samples.add(FloatTag.valueOf(sampleBuffer[i]));
        }
        compound.put("Samples", samples);
        compound.putInt("Head", head);
        compound.putInt("Color", color.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if(!isSpeedRequirementFulfilled())
            return;
        headTarget = Mth.clamp(wire.potentialDifference() / maxValue, -1, 1);
        prevHeadPosition = headPosition;
        headPosition += (headTarget - headPosition) * 0.9f;

        sampleBuffer[head] = headPosition;
        head = (head + 1) % sampleBuffer.length;
        setUnsaved();
    }

    public void setColor(DyeColor dyeColor) {
        this.color = dyeColor;
        notifyUpdate();
    }

    private static final Object MAG_SLOT = new Object();
    private static final Object TIME_SLOT = new Object();
    private static final Object ZERO_SLOT = new Object();

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            var target = ClientSideAccess.getHitResult();
            if(target.getType() == HitResult.Type.BLOCK) {
                var hit = (BlockHitResult) target;
                if(hit.getDirection() != Direction.UP)
                    return;
                var loc = target.getLocation();
                var facing = getBlockState().getValue(PlotterBlock.HORIZONTAL_FACING);
                var coord = loc.get(facing.getAxis()) - worldPosition.get(facing.getAxis());
                if(facing.getAxisDirection() == Direction.AxisDirection.POSITIVE)
                    coord = 1 - coord;
                coord = (coord - 4 / 16f) / ((16 - 4) / 16f);
                if(coord < 0 || coord > 1)
                    return;
                coord = 1 - coord;
                int i = Math.round((float) coord * sampleBuffer.length);
                var x = sampleBuffer[(head + i) % sampleBuffer.length];

                var p1 = PlotterRenderer.graphPoint((float) i / sampleBuffer.length, 0, worldPosition, facing);
                var p2 = PlotterRenderer.graphPoint((float) i / sampleBuffer.length, x, worldPosition, facing);
                Outliner.getInstance().showLine(MAG_SLOT, p1, p2)
                        .lineWidth(1 / 80f)
                        .colored(new Color(200, 48, 48));
                Outliner.getInstance().showLine(TIME_SLOT,
                                PlotterRenderer.graphPoint((float) i / sampleBuffer.length, -1, worldPosition, facing),
                                PlotterRenderer.graphPoint((float) i / sampleBuffer.length, 1, worldPosition, facing))
                        .lineWidth(1 / 96f)
                        .colored(new Color(64, 64, 64));
                Outliner.getInstance().showLine(ZERO_SLOT,
                                PlotterRenderer.graphPoint(0, 0, worldPosition, facing),
                                PlotterRenderer.graphPoint(1, 0, worldPosition, facing))
                        .lineWidth(1 / 96f)
                        .colored(new Color(64, 64, 64));

                Lang.translate("gui.plotter.header")
                        .add(Lang.numberConstant((1 - coord) * sampleBuffer.length / 20f))
                        .add(Component.literal("s"))
                        .forGoggles(tooltip);
                Unit.VOLTAGE.formatWithPrefixes(x * maxValue)
                        .style(ChatFormatting.AQUA)
                        .forGoggles(tooltip, 1);
            }
        });
        return true;
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        return Math.abs(getSpeed()) >= 16;
    }

    public static class BoxTransform extends CenteredSideValueBoxTransform {
        public BoxTransform() {
            super((state, face) -> face.getAxis() == state.getValue(PlotterBlock.HORIZONTAL_FACING).getAxis());
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 7, 15.5);
        }
    }
}
