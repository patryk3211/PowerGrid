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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;

public class ThermalBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ThermalBehaviour> TYPE = new BehaviourType<>("thermal");
    public static final float BASE_TEMPERATURE = 22.0f;

    public static final int OVERHEAT_PARTICLES = 1;
    public static final int OVERHEAT_EXPLOSION = 2;
    public static final int IGNORE_EXTRA_COOLING = 4;

    private float temperature;

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

    public ThermalBehaviour(SmartBlockEntity be, float thermalMass, float dissipationFactor, float overheatTemperature) {
        super(be);
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.overheatTemperature = overheatTemperature;

        this.temperature = BASE_TEMPERATURE;
        this.coolingFactorMultiplier = 1.0f;
    }

    public ThermalBehaviour(SmartBlockEntity be, float thermalMass, float dissipationFactor) {
        this(be, thermalMass, dissipationFactor, 175.0f);
    }

    public static ThermalBehaviour forMaxPower(SmartBlockEntity be, float thermalMass, float power) {
        return forMaxPower(be, thermalMass, power, 175.0f);
    }

    public static ThermalBehaviour forMaxPower(SmartBlockEntity be, float thermalMass, float power, float overheatTemperature) {
        var targetTemperature = overheatTemperature - 25;
        var dissipation = power / (targetTemperature - BASE_TEMPERATURE);
        return new ThermalBehaviour(be, thermalMass, dissipation, overheatTemperature);
    }

    public ThermalBehaviour behaviourFlags(int flags) {
        behaviourFlags = flags;
        return this;
    }

    public ThermalBehaviour overheatCallback(Runnable callback) {
        this.overheatCallback = callback;
        return this;
    }

    public void track(@Nullable ThermalBehaviour other) {
        if(other == this || other == null) {
            this.trackedBehaviour = null;
            return;
        }
        this.trackedBehaviour = other.getPos();
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

        var tracked = trackedBehaviour != null ? get(getWorld(), trackedBehaviour, TYPE) : null;
        if(tracked != null) {
            this.temperature = tracked.temperature;
        }

        if(coolingAir != null && (coolingAir.source.isSourceRemoved() || coolingAir.source.getSpeed() == 0)) {
            noCooling();
        }

        if(tracked == null) {
            // Dissipate energy
            float dissipatedPower = dissipationFactor * coolingFactorMultiplier * (temperature - BASE_TEMPERATURE);
            temperature -= dissipatedPower / 20f / thermalMass;
            if(dissipatedPower != 0)
                blockEntity.markDirty();
        }

        var world = getWorld();
        var pos = getPos();
        if(world.isClient && ((behaviourFlags & OVERHEAT_PARTICLES) != 0)) {
            var random = getWorld().getRandom();
            float x = pos.getX() + random.nextFloat();
            float y = pos.getY() + random.nextFloat();
            float z = pos.getZ() + random.nextFloat();
            if (temperature >= overheatTemperature - 50) {
                float chance = (temperature - overheatTemperature + 100) / 100;
                if (random.nextFloat() < chance)
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
            }
        }

        if(isOverheated() && !world.isClient) {
            if((behaviourFlags & OVERHEAT_EXPLOSION) != 0) {
                explode(world, pos, blockEntity.getCachedState(), 1.0f);
            }
            if(overheatCallback != null)
                overheatCallback.run();
        }
    }

    public static void explode(World world, BlockPos pos, BlockState state, float power) {
        var registry = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
        var source = new MachineOverloadDamageSource(registry.getEntry(ModdedDamageTypes.OVERLOADED_MACHINE).get(), state.getBlock());
        // This block must be broken first to allow for damage to propagate.
        world.breakBlock(pos, false);
        world.createExplosion(null, source, null, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, power, false, World.ExplosionSourceType.BLOCK);
    }

    public boolean isOverheated() {
        return temperature >= overheatTemperature;
    }

    public void applyTickPower(float power) {
        var energy = power / 20f;
        temperature += energy / thermalMass;
        blockEntity.markDirty();
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        temperature = nbt.getFloat("Temperature");
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
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

    public static class MachineOverloadDamageSource extends DamageSource {
        private final Block machine;

        public MachineOverloadDamageSource(RegistryEntry<DamageType> type, Block machine) {
            super(type);
            this.machine = machine;
        }

        @Override
        public Text getDeathMessage(LivingEntity killed) {
            var translationId = "death.attack." + this.getType().msgId();
            var primeAdversary = killed.getPrimeAdversary();
            var machineName = Text.translatable(machine.getTranslationKey());
            if(primeAdversary != null) {
                return Text.translatable(translationId + ".player", killed.getDisplayName(), machineName, primeAdversary.getDisplayName());
            } else {
                return Text.translatable(translationId, killed.getDisplayName(), machineName);
            }
        }
    }
}
