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
package org.patryk3211.powergrid.electricity.transformer;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

public class TransformerSoundInstance extends MovingSoundInstance {
    protected final TransformerBlockEntity be;

    protected TransformerSoundInstance(TransformerBlockEntity be) {
        super(ModdedSoundEvents.TRANSFORMER_HUM.getMainEvent(), SoundCategory.AMBIENT, be.getWorld().random);

        this.be = be;
        var pos = be.getPos().toCenterPos();
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
        if(be.isRemoved()) {
            setDone();
        } else {
            var volume = (be.lastPower / 40) - 0.25f;
            this.volume = MathHelper.clamp(volume, 0, 1);
        }
    }
}
