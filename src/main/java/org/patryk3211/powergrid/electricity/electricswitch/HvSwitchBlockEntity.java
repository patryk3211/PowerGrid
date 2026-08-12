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
package org.patryk3211.powergrid.electricity.electricswitch;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.createmod.catnip.animation.LerpedFloat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.ZapParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

import java.util.List;

public class HvSwitchBlockEntity extends ElectricKineticBlockEntity {
    protected LerpedFloat rod;
    @Nullable
    private SwitchedWire wire;

    // Only used for audio
    private int state = 1;
    private int splitCooldown;

    private boolean sparking;
    private int sparkTicks = 0;

    public HvSwitchBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        rod = LerpedFloat.linear().startWithValue(0).chase(0, 0, LerpedFloat.Chaser.LINEAR);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour.reducedSync();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        float speed = getSpeed();
        var facing = getBlockState().getValue(HvSwitchBlock.HORIZONTAL_FACING);
        if(facing == Direction.NORTH || facing == Direction.EAST)
            speed = -speed;
        rod.chase(speed > 0 ? 1 : 0, getChaseSpeed(), LerpedFloat.Chaser.LINEAR);
        sendData();
    }

    private float getChaseSpeed() {
        return Mth.clamp(Math.abs(getSpeed()) / 21 / 20, 0, 1);
    }

    private boolean getSwitchState() {
        return wire != null && wire.getState();
    }

    @Override
    public void initialize() {
        super.initialize();
        if(rod.getValue() == 1)
            state = 2;
        if(rod.getValue() == 0)
            state = 0;
        if(wire == null && isClosed())
            electricBehaviour.rebuildCircuit(false);
        if(wire != null) {
            wire.setState(false);
            wire.setResistance(getResistance());
            wire.setState(isClosed());
        }
    }

    private float extinguishRPM() {
        return Math.abs(wire == null ? 0 : (float) wire.current()) * ModdedConfigs.server().electricity.hvSwitchSparkExtinguishRPMFactor.getF();
    }

    private float sparkPotential() {
        float x = (1 - rod.getValue()) * 5;
        return ModdedConfigs.server().electricity.hvSwitchSparkPotentialFactor.getF() * x * x;
    }

    private static float sparkCurrent() {
        return ModdedConfigs.server().electricity.hvSwitchSparkMinimumCurrent.getF();
    }

    public boolean isSparking() {
        return sparking;
    }

    @Override
    public void tick() {
        applyPower(wire);

        super.tick();
        rod.tickChaser();

        if(sparking)
            ++sparkTicks;
        else
            sparkTicks = 0;
        if(!level.isClientSide) {
            if ((getSwitchState() != isClosed() && !isClosed()) || getResistance() > 5) {
                // Has disconnected.
                if (Math.abs(getSpeed()) < extinguishRPM()) {
                    sparking = true;
                }
            } else if(rod.getValue() > 0.1f && (!isClosed() || getResistance() > 5)) {
                if(wire == null) {
                    electricBehaviour.rebuildCircuit(false);
                } else {
                    // Switch open but voltage may be high enough to spark.
                    double voltage = Math.abs(wire.potentialDifference());
                    double sparkPotential = sparkPotential();
                    if (voltage > sparkPotential * 1.5f) {
                        sparking = true;
                    } else if (voltage > sparkPotential && level.random.nextFloat() < (voltage / sparkPotential - 1)) {
                        sparking = true;
                    }
                }
            }
        }
        if(sparking) {
            if(wire == null)
                electricBehaviour.rebuildCircuit(false);
            float openness = 1 - rod.getValue();
            // 0.5-5 Ohms for the arc resistance
            wire.setResistance(0.5 + level.random.nextFloat() * 4.5);
            wire.setState(true);
            if(sparkTicks >= 3 && !level.isClientSide) {
                if (isClosed() && wire.getResistance() > getResistance()) {
                    // The switch has closed with a better contact than the spark.
                    sparking = false;
                } else if (level.random.nextFloat() < openness * 0.9 - 0.1) {
                    // Random chance for the arc to go away.
                    sparking = false;
                } else if(Math.abs(wire.current()) < sparkCurrent()) {
                    // Not enough current for spark to continue to exist
                    sparking = false;
                }
                if(!sparking) {
                    wire.setResistance(getResistance());
                    wire.setState(isClosed());
                }
            }
        } else if(!rod.settled()) {
            if(getSwitchState() != isClosed() || (wire != null && wire.getResistance() != getResistance())) {
                if(isClosed()) {
                    if(wire == null)
                        electricBehaviour.rebuildCircuit(false);
                    wire.setResistance(getResistance());
                    wire.setState(isClosed());
                } else {
                    if(wire != null) {
                        wire.setState(false);
                    }
                }
                setUnsaved();
            }
        } else {
            if(wire != null && !wire.getState() && splitCooldown++ >= 100) {
                GlobalElectricNetworks.getWorldNetworks(level).scheduleIslandDiscovery(wire.getNetwork());
                electricBehaviour.rebuildCircuit(false);
            }
        }
        if(sparking && level.isClientSide && Math.abs(wire == null ? 0 : wire.current()) >= sparkCurrent()) {
            var facing = getBlockState().getValue(HvSwitchBlock.HORIZONTAL_FACING);
            double angle = rod.getValue() * Math.PI * 0.5;
            var origin = worldPosition.getCenter()
                    .add(facing.getStepX() * Math.sin(angle) * 1.5, Math.cos(angle) * 1.5, facing.getStepZ() * Math.sin(angle) * 1.5)
                    .offsetRandom(level.random, 0.1f);
            var end = worldPosition.getCenter()
                    .relative(facing, 1.2)
                    .subtract(0, 0.125, 0)
                    .offsetRandom(level.random, 0.1f);
            var sublevel = SableCompanion.INSTANCE.getContaining(level, worldPosition);
            level.addAlwaysVisibleParticle(new ZapParticleData(end, true, 2, 10, 0.1f),
                    true, origin.x, origin.y, origin.z, 0, 0, 0);
            if(sublevel != null) {
                origin = sublevel.lastPose().transformPosition(origin);
                end = sublevel.lastPose().transformPosition(end);
            }
            double dist = origin.distanceTo(end);
            int sparks = (int) (dist / 0.2f);
            for(int i = 0; i < sparks + 1; ++i) {
                double x = Mth.lerp((float) i / sparks, origin.x, end.x);
                double y = Mth.lerp((float) i / sparks, origin.y, end.y);
                double z = Mth.lerp((float) i / sparks, origin.z, end.z);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
            }
        }
        if((sparkTicks == 0) == sparking && !level.isClientSide)
            notifyUpdate();
    }

    @Override
    public void tickAudio() {
        assert level != null;
        super.tickAudio();
        var newState = 1;
        if(rod.getValue() == 1)
            newState = 2;
        if(rod.getValue() == 0)
            newState = 0;
        if(newState != state) {
            state = newState;
            if(state == 2) {
                level.playLocalSound(getBlockPos(), ModdedSoundEvents.HV_SWITCH_DISCONNECT.getMainEvent(),
                        SoundSource.BLOCKS, 1, 1, true);
            } else if(state == 0) {
                level.playLocalSound(getBlockPos(), ModdedSoundEvents.HV_SWITCH_CONNECT.getMainEvent(),
                        SoundSource.BLOCKS, 1, 1, true);
            }
        }
    }

    public boolean isClosed() {
        if(rod == null)
            return false;
        return rod.getValue() > 0.9f;
    }

    public float getResistance() {
        if(rod == null || !isClosed())
            return resistance();
        var x = rod.getValue();
        return -999 * x + 999 + resistance();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Rod", rod.writeNBT());
        compound.putBoolean("Sparking", sparking);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.put("Rod", rod.writeNBT());
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        // Always force the value to be set
        rod.readNBT(compound.getCompound("Rod"), false);
        if(wire == null && isClosed())
            electricBehaviour.rebuildCircuit(false);
        if(wire != null) {
            wire.setState(false);
            wire.setResistance(getResistance());
            wire.setState(isClosed());
        }
        boolean wasSparking = sparking;
        sparking = compound.getBoolean("Sparking");
        if(clientPacket && !wasSparking && sparking) {
            makeSparkSound();
        }
    }

    @Environment(EnvType.CLIENT)
    public void makeSparkSound() {
        Minecraft.getInstance().getSoundManager().play(new HvSparkSoundInstance(this));
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        if(isClosed() || sparking || (rod != null && rod.getValue() > 0.1f)) {
            splitCooldown = 0;
            wire = builder.connectSwitch(getResistance(), builder.terminalNode(0), builder.terminalNode(1), isClosed());
        } else {
            wire = null;
        }
    }
}
