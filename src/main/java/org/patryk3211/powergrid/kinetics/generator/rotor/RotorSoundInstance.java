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

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

public class RotorSoundInstance extends MovingSoundInstance {
    private final RotorBehaviour behaviour;

    protected RotorSoundInstance(RotorBehaviour behaviour) {
        super(ModdedSoundEvents.GENERATOR.getMainEvent(), SoundCategory.AMBIENT, behaviour.getWorld().random);
        this.behaviour = behaviour;
        var pos = behaviour.getPos().toCenterPos();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.attenuationType = AttenuationType.LINEAR;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0F;
    }

    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        if(behaviour.blockEntity.isRemoved() || !behaviour.isController()) {
            setDone();
        } else {
            var velocity = Math.abs(behaviour.getAngularVelocity());
            var pitch = velocity / 128f;
            if(velocity < 32) {
                this.volume = 0.0f;
                setDone();
            } else {
                var volume = (velocity / 128);
                this.volume = MathHelper.clamp(volume, 0, 1) * 0.3f;
            }
            this.pitch = MathHelper.clamp(pitch, 0.5f, 2.0f);
        }
    }
}
