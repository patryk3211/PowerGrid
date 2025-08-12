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
package org.patryk3211.powergrid.electricity.bell;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

@Environment(EnvType.CLIENT)
public class AlarmBellSoundInstance extends AbstractTickableSoundInstance {
    private final AlarmBellBlockEntity be;

    protected AlarmBellSoundInstance(AlarmBellBlockEntity be) {
        super(ModdedSoundEvents.ALARM_BELL.getMainEvent(), SoundSource.BLOCKS, be.getLevel().random);
        this.be = be;

        var pos = be.getBlockPos().getCenter();
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

    public void playEnd() {
        var manager = Minecraft.getInstance().getSoundManager();
        manager.play(new SimpleSoundInstance(ModdedSoundEvents.ALARM_BELL_END.getMainEvent(), SoundSource.BLOCKS, volume, pitch, random, x, y, z));
    }

    @Override
    public void tick() {
        if(be.isRemoved()) {
            stop();
        } else {
            var newVolume = be.getVolume();
            if(newVolume == 0) {
                stop();
                playEnd();
            }
            volume = be.getVolume();
            pitch = be.getPitch();
        }
    }
}
