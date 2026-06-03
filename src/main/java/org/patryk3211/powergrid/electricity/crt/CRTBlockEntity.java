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
package org.patryk3211.powergrid.electricity.crt;

import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.special.SamplingWire;

public class CRTBlockEntity extends ElectricBlockEntity {
    private SamplingWire xDeflect, yDeflect;
    private ElectricWire heater, gridCathode;
    private SamplingWire anodeCathode;

    // These are only needed on the client. CRT state is not persistent nor synchronized.
    protected float[] xPoints = new float[0];
    protected float[] yPoints = new float[0];
    protected float[] brightness = new float[0];
    protected DyeColor traceColor = DyeColor.RED;
    protected int head;

    public CRTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        electricBehaviour.setSyncAppender(ElectricBehaviour.CompoundAppender.of(xDeflect, yDeflect, anodeCathode));
    }

    public static int configSampleCount() {
        return EnvExecutor.getEnvSpecific(() -> () -> ModdedConfigs.client().crtPointCount.get(), () -> () -> 1);
    }

    public int ticks() {
        var ticksX = xDeflect.getNetwork() == null ? 1 : xDeflect.getNetwork().getMultiTick();
        var ticksY = yDeflect.getNetwork() == null ? 1 : yDeflect.getNetwork().getMultiTick();
        return Math.max(ticksX, ticksY);
    }

    public int sampleCount() {
        return configSampleCount() * ticks();
    }

    public DyeColor getColor() {
        return traceColor;
    }

    @Override
    public float resistance(String suffix) {
        return ResistanceValues.get(ModdedBlocks.CRT.get(), suffix);
    }

    @Override
    public boolean isNoisy() {
        return false;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("Color", traceColor.ordinal());
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        tag.putInt("Color", traceColor.ordinal());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("Color")) {
            traceColor = DyeColor.values()[tag.getInt("Color")];
        } else {
            traceColor = DyeColor.RED;
        }
    }

    private static float[] resample(int newSize, float[] old) {
        var sampleBuffer = new float[newSize];
        if(old.length == 0) {
            return sampleBuffer;
        }
        for(int i = 0; i < sampleBuffer.length; ++i) {
            float oldI = (float) i / sampleBuffer.length * old.length;
            int i0 = Mth.clamp(Mth.floor(oldI), 0, old.length - 1);
            int i1 = Mth.clamp(Mth.ceil(oldI), 0, old.length - 1);
            float s0 = old[i0];
            float s1 = old[i1];
            float weight = oldI - i0;
            sampleBuffer[i] = s0 * (1.0f - weight) + s1 * weight;
        }
        return sampleBuffer;
    }

    private void resample(int newSize) {
        int oldSize = xPoints.length;
        xPoints = resample(newSize, xPoints);
        yPoints = resample(newSize, yPoints);
        brightness = resample(newSize, brightness);
        if(oldSize == 0) {
            head = 0;
            return;
        }
        head = (int)((float) head / oldSize * newSize);
        if(head < 0) head = 0;
        if(head >= newSize) head = newSize - 1;
    }

    @Override
    public void electricalTick() {
        applyPower(xDeflect);
        applyPower(yDeflect);
        applyPower(heater);

        // Simplified electron tube model
        var Vgk = gridCathode.potentialDifference();
        var ival = Math.min(Vgk, 0) + anodeCathode.potentialDifference() / 100;

        // 1 amp is the target current for normal operation.
        // 0.8 amps is the minimum current.
        var heaterPower = Math.abs(heater.current());
        heaterPower = heaterPower < 0.8 ? 0 : Math.min(heaterPower * heaterPower, 1.2f);

        // Heater power affects the electron gun current.
        var R = (ival <= 0 || heaterPower <= 0) ? 0 : (anodeCathode.potentialDifference() / (ival * 0.01f * heaterPower));
        if(R <= 0) {
            anodeCathode.setResistance(1 / SwitchedWire.OFF_CONDUCTANCE);
        } else {
            anodeCathode.setResistance(R);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Simplified electron tube model
        var Vgk = gridCathode.potentialDifference();
        var ival = Math.min(Vgk, 0) + anodeCathode.potentialDifference() / 100;

        // 1 amp is the target current for normal operation.
        // 0.8 amps is the minimum current.
        var heaterPower = Math.abs(heater.current());
        heaterPower = heaterPower < 0.8 ? 0 : Math.min(heaterPower * heaterPower, 1.2f);

        if(level.isClientSide) {
            // 100 mA is needed for max brightness.
            // 50 mA is the minimum current.
            if((!xDeflect.isConverged() || !yDeflect.isConverged() || !anodeCathode.isConverged())
                    && !ModdedConfigs.server().electricity.plotterRecordNonconvergence.get())
                return;
            int newSize = sampleCount();
            if(newSize != xPoints.length)
                resample(newSize);
            int ticks = anodeCathode.samples == null ? 1 : anodeCathode.samples.length;
            for(int i = 0; i < ticks; ++i) {
                double bV = anodeCathode.samples == null ? anodeCathode.potentialDifference() : anodeCathode.samples[i];
                double xV = xDeflect.samples == null ? xDeflect.potentialDifference() : xDeflect.samples[i];
                double yV = yDeflect.samples == null ? yDeflect.potentialDifference() : yDeflect.samples[i];
                brightness[head] = (float) Mth.clamp((bV / anodeCathode.getResistance() - 0.05) / 0.05, 0, 1.5);
                xPoints[head] = (float) Mth.clamp(xV / xDeflect.getResistance() / 0.5, -1, 1);
                yPoints[head] = (float) Mth.clamp(yV / yDeflect.getResistance() / 0.5, -1, 1);
                head = (head + 1) % brightness.length;
            }
        }

        // Heater power affects the electron gun current.
        var R = (ival <= 0 || heaterPower <= 0) ? 0 : (anodeCathode.potentialDifference() / (ival * 0.01f * heaterPower));
        if(R <= 0) {
            anodeCathode.setResistance(1 / SwitchedWire.OFF_CONDUCTANCE);
        } else {
            anodeCathode.setResistance(R);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(7);
        float R = resistance("coils");
        xDeflect = new SamplingWire(R, builder.terminalNode(4), builder.terminalNode(6));
        yDeflect = new SamplingWire(R, builder.terminalNode(5), builder.terminalNode(6));
        builder.add(xDeflect);
        builder.add(yDeflect);

        gridCathode = builder.connect(1e+6f, builder.terminalNode(2), builder.terminalNode(0));
        heater = builder.connect(resistance("heater"), builder.terminalNode(1), builder.terminalNode(0));
        anodeCathode = new SamplingWire(1e+6f, builder.terminalNode(3), builder.terminalNode(0));
        builder.add(anodeCathode);
    }

    public InteractionResult setColor(DyeColor color) {
        traceColor = color;
        notifyUpdate();
        return InteractionResult.SUCCESS;
    }
}
