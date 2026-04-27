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

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class RepeatingSound {
    private SoundEvent event;
    private float sharedPitch;
    private int repeatDelay;
    private SoundScape scape;
    private float relativeVolume;

    public RepeatingSound(SoundEvent event, SoundScape scape, float sharedPitch, float relativeVolume,
                          int repeatDelay) {
        this.event = event;
        this.scape = scape;
        this.sharedPitch = sharedPitch;
        this.relativeVolume = relativeVolume;
        this.repeatDelay = Math.max(1, repeatDelay);
    }

    public void tick() {
        if (AnimationTickHolder.getTicks() % repeatDelay != 0)
            return;

        ClientLevel world = Minecraft.getInstance().level;
        Vec3 meanPos = scape.getMeanPos();

        world.playLocalSound(meanPos.x, meanPos.y, meanPos.z, event, SoundSource.AMBIENT,
                scape.getVolume() * relativeVolume, sharedPitch, true);
    }

}
