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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableObject;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

@Environment(EnvType.CLIENT)
public class RotorSoundInstance extends AbstractTickableSoundInstance {
    private final RotorBehaviour behaviour;

    protected RotorSoundInstance(RotorBehaviour behaviour) {
        super(ModdedSoundEvents.GENERATOR.getMainEvent(), SoundSource.AMBIENT, behaviour.getWorld().random);
        this.behaviour = behaviour;
        var pos = behaviour.getPos().getCenter();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
    }

    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if(behaviour.blockEntity.isRemoved() || !behaviour.isController()) {
            stop();
        } else {
            var closest = new MutableObject<BlockPos>();
            var playerPos = Minecraft.getInstance().player.blockPosition();
            behaviour.forEachSegment(segment -> {
                if(closest.getValue() == null) {
                    closest.setValue(segment.getPos());
                } else {
                    var dCurrent = closest.getValue().distSqr(playerPos);
                    var dNew = segment.getPos().distSqr(playerPos);
                    if(dNew < dCurrent) {
                        closest.setValue(segment.getPos());
                    }
                }
            });
            if(closest.getValue() != null) {
                var pos = closest.getValue().getCenter();
                this.x = pos.x;
                this.y = pos.y;
                this.z = pos.z;
            }

            var velocity = Math.abs(behaviour.getAngularVelocity());
            var pitch = velocity / (behaviour.getMaxRotationSpeed() / 2f);
            if(velocity < 32) {
                this.volume = 0.0f;
                stop();
            } else {
                var volume = (velocity / 128) * ModdedConfigs.client().generatorSoundMultiplier.getF();
                this.volume = Mth.clamp(volume, 0, 1);
            }
            this.pitch = Mth.clamp(pitch, 0.5f, 2f);
        }
    }
}
