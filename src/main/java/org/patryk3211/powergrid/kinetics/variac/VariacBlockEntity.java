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
package org.patryk3211.powergrid.kinetics.variac;

import net.createmod.catnip.animation.LerpedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.electricity.transformer.TransformerSoundInstance;
import org.patryk3211.powergrid.electricity.transformer.TransformerVolumeProvider;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

public class VariacBlockEntity extends ElectricKineticBlockEntity implements TransformerVolumeProvider {
    public static final float PRIMARY_TURNS = 25;
    public static final float CORE_AL = 1.5f;
    public static final float COUPLING_FACTOR = 0.9999f;
    public static final float PRIMARY_INDUCTANCE = PRIMARY_TURNS * PRIMARY_TURNS * CORE_AL;

//    protected ModelData assembly;
    protected LerpedFloat arm;

    protected ElectricWire primaryStray;
    protected ElectricWire secondaryStray;
    protected ElectricWire mutualInductance;
    protected TransformerCoupling coupling;

    public float lastCurrent;
    private boolean hasSoundSource;

    public VariacBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arm = LerpedFloat.linear().chase(0, 0, LerpedFloat.Chaser.LINEAR);
    }

    @Override
    public void initialize() {
        super.initialize();
        arm.forceNextSync();
        sendData();
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / 60.0f * 0.05f, 0, 1);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.simple(this, 2.0f, 0.5f);
    }

    @Override
    public float getVolume() {
        var volume = lastCurrent / 80;
        return Mth.clamp(volume * volume, 0, 0.5f);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        float speed = getSpeed();
        if(speed == 0) {
            arm.chase(arm.getValue(), 0, LerpedFloat.Chaser.LINEAR);
            arm.forceNextSync();
            return;
        }
        arm.chase(speed > 0 ? 1 : 0, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        sendData();
    }

    public float getRatio() {
        if(arm == null)
            return 0.01f;
        return arm.getValue() * 0.99f + 0.01f;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);

        var secondaryTurns = getRatio() * PRIMARY_TURNS;
        float secondaryInductance = secondaryTurns * secondaryTurns * CORE_AL;

        float mutualInductance = secondaryInductance * COUPLING_FACTOR;

        float ratio = getRatio();
        float primaryStray = PRIMARY_INDUCTANCE - mutualInductance;
        float secondaryStray = secondaryInductance - ratio * ratio * mutualInductance;

        var Tnode = builder.addInternalNode();
        var Pnode = builder.addInternalNode();

        this.primaryStray = builder.connect(primaryStray, builder.terminalNode(0), Tnode);
        this.mutualInductance = builder.connect(mutualInductance, Tnode, builder.terminalNode(1));
        // TODO: Find out if this can be replaced with the transformer coupling's resistance.
        this.secondaryStray = builder.connect(secondaryStray, Tnode, Pnode);
        this.coupling = builder.couple(ratio, Pnode, builder.terminalNode(1),
                builder.terminalNode(2), builder.terminalNode(1));
    }

    public void refreshParameters() {
        var secondaryTurns = getRatio() * PRIMARY_TURNS;
        float secondaryInductance = secondaryTurns * secondaryTurns * CORE_AL;
        float ratio = getRatio();

        float mutualInductance = secondaryInductance / (ratio * ratio) * COUPLING_FACTOR;
        float primaryStray = PRIMARY_INDUCTANCE - mutualInductance;
        float secondaryStray = secondaryInductance - ratio * ratio * mutualInductance;
        this.primaryStray.setResistance(primaryStray);
        this.mutualInductance.setResistance(mutualInductance);
        this.secondaryStray.setResistance(secondaryStray);
        this.coupling.setRatio(ratio);
    }

    @Override
    public void tick() {
        super.tick();
        arm.tickChaser();

        float power = 0;
        lastCurrent = 0;
        if(primaryStray != null) {
            var I1 = primaryStray.current();
            power += (float) (I1 * I1 * primaryStray.getResistance());
            lastCurrent += Math.abs(I1);
        }
        if(secondaryStray != null) {
            var I2 = secondaryStray.current();
            power += (float) (I2 * I2 * secondaryStray.getResistance());
            lastCurrent += Math.abs(I2);
        }
        if(mutualInductance != null) {
            var I3 = mutualInductance.current();
            power += (float) (I3 * I3 * mutualInductance.getResistance());
            lastCurrent += Math.abs(I3);
        }
        applyLostPower(power);

        if(!arm.settled()) {
            if(getSpeed() == 0) {
                arm.updateChaseTarget(arm.getValue());
            }
            refreshParameters();
            setChanged();
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        super.tickAudio();
        if(!hasSoundSource && getVolume() > 0) {
            Minecraft.getInstance().getSoundManager().play(new TransformerSoundInstance(this));
            hasSoundSource = true;
        } else if(hasSoundSource && getVolume() <= 0) {
            hasSoundSource = false;
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if(clientPacket)
            arm.forceNextSync();
        compound.put("Arm", arm.writeNBT());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        arm.readNBT(compound.getCompound("Arm"), clientPacket);
        refreshParameters();
    }
}
