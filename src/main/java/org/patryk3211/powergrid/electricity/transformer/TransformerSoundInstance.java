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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

@Environment(EnvType.CLIENT)
public class TransformerSoundInstance extends AbstractTickableSoundInstance {
    protected final BlockEntity be;
    protected final TransformerVolumeProvider provider;

    public  <T extends BlockEntity& TransformerVolumeProvider> TransformerSoundInstance(T be) {
        super(ModdedSoundEvents.TRANSFORMER_HUM.getMainEvent(), SoundSource.AMBIENT, be.getLevel().random);

        this.be = be;
        this.provider = be;

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

    @Override
    public void tick() {
        if(be.isRemoved()) {
            stop();
        } else {
            this.volume = provider.getVolume();
            if(this.volume == 0)
                stop();
        }
    }
}
