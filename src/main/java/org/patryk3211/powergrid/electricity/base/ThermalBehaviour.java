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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;

public class ThermalBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ThermalBehaviour> TYPE = new BehaviourType<>("thermal");
    public static final float BASE_TEMPERATURE = 22.0f;

    private float temperature;
    private int overheatingTicks;

    // thermalMass = ΔE/ΔT
    private final float thermalMass;
    // dissipation coefficient * area
    private final float dissipationFactor;
    private final float overheatTemperature;

    public ThermalBehaviour(SmartBlockEntity be, float thermalMass, float dissipationFactor, float overheatTemperature) {
        super(be);
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.overheatTemperature = overheatTemperature;

        this.temperature = BASE_TEMPERATURE;
        this.overheatingTicks = 0;
    }

    public ThermalBehaviour(SmartBlockEntity be, float thermalMass, float dissipationFactor) {
        this(be, thermalMass, dissipationFactor, 175.0f);
    }

    @Override
    public void tick() {
        super.tick();
        // Dissipate energy
        float dissipatedPower = dissipationFactor * (temperature - BASE_TEMPERATURE);
        temperature -= dissipatedPower / 20f / thermalMass;

        var world = getWorld();
        var pos = getPos();
        if(world.isClient) {
            var random = getWorld().getRandom();
            float x = pos.getX() + random.nextFloat();
            float y = pos.getY() + random.nextFloat();
            float z = pos.getZ() + random.nextFloat();
            if(temperature >= overheatTemperature - 50) {
                float chance = (temperature - overheatTemperature + 100) / 100;
                if(random.nextFloat() < chance)
                    world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
            }
        }

        if(temperature >= overheatTemperature && !world.isClient) {
            var registry = getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
            var source = new MachineOverloadDamageSource(registry.getEntry(ModdedDamageTypes.OVERLOADED_MACHINE).get(), blockEntity.getCachedState().getBlock());
            // This block must be broken first to allow for damage to propagate.
            world.breakBlock(getPos(), false);
            world.createExplosion(null, source, null, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, 1.0f, false, World.ExplosionSourceType.BLOCK);
        }

        if(dissipatedPower != 0)
            blockEntity.markDirty();
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
        if(nbt.contains("Overheating")) {
            overheatingTicks = nbt.getInt("Overheating");
        } else {
            overheatingTicks = 0;
        }
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        nbt.putFloat("Temperature", temperature);
        if(overheatingTicks > 0)
            nbt.putInt("Overheating", overheatingTicks);
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
