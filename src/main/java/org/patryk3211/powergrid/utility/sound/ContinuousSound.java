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
package org.patryk3211.powergrid.utility.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ContinuousSound extends AbstractTickableSoundInstance {
    private float sharedPitch;
    private SoundScape scape;
    private float relativeVolume;

    protected ContinuousSound(SoundEvent event, SoundScape scape, float sharedPitch, float relativeVolume) {
        super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.scape = scape;
        this.sharedPitch = sharedPitch;
        this.relativeVolume = relativeVolume;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
    }

    public void remove() {
        stop();
    }

    @Override
    public float getVolume() {
        return scape.getVolume() * relativeVolume;
    }

    @Override
    public float getPitch() {
        return sharedPitch;
    }

    @Override
    public double getX() {
        return scape.getMeanPos().x;
    }

    @Override
    public double getY() {
        return scape.getMeanPos().y;
    }

    @Override
    public double getZ() {
        return scape.getMeanPos().z;
    }

    @Override
    public void tick() {}

}
