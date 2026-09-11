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

import java.util.function.BooleanSupplier;

public class ContinuousSound extends AbstractTickableSoundInstance {
    private final float sharedPitch;
    private final SoundScape scape;
    private final float relativeVolume;
    private final BooleanSupplier stillPlaying;
    private final float targetVolume;
    private final float fadeSpeed;

    protected ContinuousSound(SoundEvent event, SoundScape scape, float sharedPitch, float relativeVolume) {
        super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.scape = scape;
        this.sharedPitch = sharedPitch;
        this.relativeVolume = relativeVolume;
        this.stillPlaying = null;
        this.targetVolume = 0;
        this.fadeSpeed = 0;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
    }

    public ContinuousSound(SoundEvent event, SoundSource source, double x, double y, double z,
                           float volume, float pitch, int fadeTicks, BooleanSupplier stillPlaying) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.scape = null;
        this.sharedPitch = pitch;
        this.relativeVolume = 1f;
        this.stillPlaying = stillPlaying;
        this.targetVolume = volume;
        this.fadeSpeed = fadeTicks <= 0 ? volume : volume / fadeTicks;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = 0;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
    }

    @Override
    public boolean canStartSilent() {
        return scape == null;
    }

    public void remove() {
        looping = false;
        volume = 0;
        stop();
    }

    @Override
    public float getVolume() {
        return scape != null ? scape.getVolume() * relativeVolume : volume;
    }

    @Override
    public float getPitch() {
        return sharedPitch;
    }

    @Override
    public double getX() {
        return scape != null ? scape.getMeanPos().x : x;
    }

    @Override
    public double getY() {
        return scape != null ? scape.getMeanPos().y : y;
    }

    @Override
    public double getZ() {
        return scape != null ? scape.getMeanPos().z : z;
    }

    @Override
    public void tick() {
        if (stillPlaying == null)
            return;
        boolean on = stillPlaying.getAsBoolean();
        float target = on ? targetVolume : 0;
        if (volume < target)
            volume = Math.min(target, volume + fadeSpeed);
        else if (volume > target)
            volume = Math.max(target, volume - fadeSpeed * 2f);
        if (!on && volume <= 0.001f)
            remove();
    }
}
