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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;

public class ThermalBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ThermalBehaviour> TYPE = new BehaviourType<>("thermal");
    public static final float BASE_TEMPERATURE = 22.0f;
    public static final int OVERHEAT_TICKS = 2;

    public static final int OVERHEAT_PARTICLES = 1;
    public static final int OVERHEAT_EXPLOSION = 2;
    public static final int IGNORE_EXTRA_COOLING = 4;

    private float temperature;
    private float prevTemperature;
    private int overheatTicks;

    // thermalMass = ΔE/ΔT
    private float thermalMass;
    // dissipation coefficient * area
    private float dissipationFactor;
    private final float overheatTemperature;

    private AirCurrent coolingAir;
    private float coolingFactorMultiplier;

    private BlockPos trackedBehaviour;

    private int behaviourFlags = OVERHEAT_PARTICLES | OVERHEAT_EXPLOSION;
    private Runnable overheatCallback;
    private boolean firstTick = true;

    private IParticleGenerator particleGenerator = null;

    protected ThermalBehaviour(SmartBlockEntity be, float thermalMass, float dissipationFactor, float overheatTemperature) {
        super(be);
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.overheatTemperature = overheatTemperature;

        this.temperature = BASE_TEMPERATURE;
        this.coolingFactorMultiplier = 1.0f;

        if(!shouldOverheat()) {
            // Overheating disabled but some devices still need the temperature in their behaviour.
            behaviourFlags = 0;
        }
    }

    @Nullable
    public static ThermalBehaviour simple(SmartBlockEntity be, float thermalMass, float dissipationFactor, float overheatTemperature) {
        if(!shouldOverheat())
            return null;
        return new ThermalBehaviour(be, thermalMass, dissipationFactor, overheatTemperature);
    }

    @NotNull
    public static ThermalBehaviour always(SmartBlockEntity be, float thermalMass, float dissipationFactor, float overheatTemperature) {
        return new ThermalBehaviour(be, thermalMass, dissipationFactor, overheatTemperature);
    }

    @Nullable
    public static ThermalBehaviour simple(SmartBlockEntity be, float thermalMass, float dissipationFactor) {
        return simple(be, thermalMass, dissipationFactor, 175.0f);
    }

    @Nullable
    public static ThermalBehaviour forMaxPower(SmartBlockEntity be, float thermalMass, float power) {
        return forMaxPower(be, thermalMass, power, 175.0f);
    }

    @Nullable
    public static ThermalBehaviour fromConfig(SmartBlockEntity be) {
        var block = be.getBlockState().getBlock();
        return forMaxPower(be, ThermalValues.getMass(block), ThermalValues.getPower(block));
    }

    @Nullable
    public static ThermalBehaviour fromConfig(SmartBlockEntity be, float overheatTemperature) {
        var block = be.getBlockState().getBlock();
        return forMaxPower(be, ThermalValues.getMass(block), ThermalValues.getPower(block), overheatTemperature);
    }

    public static float dissipationFactor(float power, float temperature) {
        return power / (temperature - BASE_TEMPERATURE);
    }

    @Nullable
    public static ThermalBehaviour forMaxPower(SmartBlockEntity be, float thermalMass, float power, float overheatTemperature) {
        var targetTemperature = overheatTemperature - 25;
        return simple(be, thermalMass, dissipationFactor(power, targetTemperature), overheatTemperature);
    }

    @Nullable
    public static <T extends SmartBlockEntity&IElectricEntity> ThermalBehaviour forVoltageAtResistance(T be, float voltage, float thermalMass) {
        return forVoltageAtResistance(be, voltage, be.resistance(), thermalMass, 175);
    }

    @Nullable
    public static <T extends SmartBlockEntity&IElectricEntity> ThermalBehaviour forVoltageAtResistance(T be, float voltage, float resistance, float thermalMass) {
        return forMaxPower(be, thermalMass, voltage * voltage / resistance, 175);
    }

    @Nullable
    public static <T extends SmartBlockEntity&IElectricEntity> ThermalBehaviour forVoltageAtResistance(T be, float voltage, float resistance, float thermalMass, float overheatTemperature) {
        return forMaxPower(be, thermalMass, voltage * voltage / resistance, overheatTemperature);
    }

    public static boolean shouldExplode() {
        return ModdedConfigs.server().electricity.explosiveDeconstruction.get();
    }

    public static boolean shouldOverheat() {
        return ModdedConfigs.server().electricity.overheating.get();
    }

    public ThermalBehaviour behaviourFlags(int flags) {
        behaviourFlags = flags;
        return this;
    }

    public ThermalBehaviour overheatCallback(Runnable callback) {
        this.overheatCallback = callback;
        return this;
    }

    public ThermalBehaviour particleGenerator(IParticleGenerator generator) {
        this.particleGenerator = generator;
        return this;
    }

    public void track(@Nullable ThermalBehaviour other) {
        if(other == this || other == null) {
            this.trackedBehaviour = null;
            return;
        }
        this.trackedBehaviour = other.getPos();
    }

    public float maxPower() {
        return (overheatTemperature - BASE_TEMPERATURE) * dissipationFactor;
    }

    public void resetTemperature() {
        this.temperature = BASE_TEMPERATURE;
    }

    public void setDissipationFactor(float dissipationFactor) {
        this.dissipationFactor = dissipationFactor;
    }

    public void setThermalMass(float mass) {
        this.thermalMass = mass;
    }

    public void noCooling() {
        coolingFactorMultiplier = 1;
        coolingAir = null;
    }

    public void setCoolingMultiplier(AirCurrent current, float value) {
        if((behaviourFlags & IGNORE_EXTRA_COOLING) != 0)
            return;
        coolingFactorMultiplier = value;
        coolingAir = current;
    }

    @Override
    public void tick() {
        super.tick();

        if(firstTick) {
            firstTick = false;
            return;
        }

        var world = getWorld();
        var pos = getPos();
        if(!world.isClientSide || blockEntity.isVirtual()) {
            var tracked = trackedBehaviour != null ? get(world, trackedBehaviour, TYPE) : null;
            if (tracked != null) {
                this.temperature = tracked.temperature;
            }

            if (coolingAir != null && (coolingAir.source.isSourceRemoved() || coolingAir.source.getSpeed() == 0)) {
                noCooling();
            }

            if (tracked == null) {
                // Dissipate energy
                float dissipatedPower = dissipationFactor * coolingFactorMultiplier * (temperature - BASE_TEMPERATURE);
                temperature -= dissipatedPower / 20f / thermalMass;
                if (dissipatedPower > 0 && temperature < BASE_TEMPERATURE)
                    temperature = BASE_TEMPERATURE;
                if (dissipatedPower != 0)
                    world.blockEntityChanged(getPos());
            }
            if (!Float.isFinite(temperature)) {
                // Reset if something went wrong.
                temperature = BASE_TEMPERATURE;
                prevTemperature = BASE_TEMPERATURE;
            }
            var temperatureDelta = temperature - prevTemperature;
            prevTemperature = temperature;

            if(isOverheated()) {
                if(temperatureDelta > 0 && overheatTicks++ >= OVERHEAT_TICKS) {
                    // Overheated for 3 ticks and temperature keeps rising,
                    // no more excuses, this device is exploding.
                    if (overheatCallback != null)
                        overheatCallback.run();
                    if ((behaviourFlags & OVERHEAT_EXPLOSION) != 0) {
                        explode(world, pos, blockEntity.getBlockState(), 1.0f);
                    }
                } else if(temperatureDelta <= 0) {
                    // Overheated but temperature is falling, the device is safe this time.
                    overheatTicks = 0;
                    if(temperature > overheatTemperature + 10) {
                        temperature = overheatTemperature + 10;
                        blockEntity.sendData();
                    }
                }
            }
        } else {
            if(((behaviourFlags & OVERHEAT_PARTICLES) != 0) && temperature >= overheatTemperature - 50) {
                var random = getWorld().getRandom();
                float chance = (temperature - overheatTemperature + 100) / 100;
                if (random.nextFloat() < chance) {
                    if (particleGenerator == null) {
                        float x = pos.getX() + random.nextFloat();
                        float y = pos.getY() + random.nextFloat();
                        float z = pos.getZ() + random.nextFloat();
                        world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
                    } else {
                        particleGenerator.generate((x, y, z) ->
                                        world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f),
                                random);
                    }
                }
            }
        }
    }

    public static void explode(Level world, BlockPos pos, BlockState state, float power) {
        if(shouldExplode()) {
            var registry = world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            var source = new MachineOverloadDamageSource(registry.getHolder(ModdedDamageTypes.OVERLOADED_MACHINE).get(), state.getBlock());
            // This block must be broken first to allow for damage to propagate.
            world.destroyBlock(pos, false);
            world.explode(null, source, null, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, power, false, Level.ExplosionInteraction.BLOCK);
        } else {
            // Break block without exploding.
            world.destroyBlock(pos, false);
        }
    }

    public boolean isOverheated() {
        return temperature >= overheatTemperature;
    }

    public void applyTickPower(float power) {
        if(Float.isFinite(power)) {
            var energy = power / 20f;
            temperature += energy / thermalMass;
        }
    }

    public void applyWirePower(@Nullable AbstractElectricWire wire) {
        if(wire == null)
            return;
        if(wire.isConverged())
            applyTickPower(wire.power());
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        temperature = nbt.getFloat("Temperature");
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        nbt.putFloat("Temperature", temperature);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public static class MachineOverloadDamageSource extends DamageSource {
        private final Block machine;

        public MachineOverloadDamageSource(Holder<DamageType> type, Block machine) {
            super(type);
            this.machine = machine;
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity killed) {
            var translationId = "death.attack." + this.type().msgId();
            var primeAdversary = killed.getKillCredit();
            var machineName = Component.translatable(machine.getDescriptionId());
            if(primeAdversary != null) {
                return Component.translatable(translationId + ".player", killed.getDisplayName(), machineName, primeAdversary.getDisplayName());
            } else {
                return Component.translatable(translationId, killed.getDisplayName(), machineName);
            }
        }
    }

    @FunctionalInterface
    public interface IParticleGenerator {
        void generate(IParticleConsumer consumer, RandomSource random);
    }

    @FunctionalInterface
    public interface IParticleConsumer {
        void accept(float x, float y, float z);
    }
}
