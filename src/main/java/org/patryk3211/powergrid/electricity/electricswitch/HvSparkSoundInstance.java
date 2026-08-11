package org.patryk3211.powergrid.electricity.electricswitch;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

public class HvSparkSoundInstance extends AbstractTickableSoundInstance {
    private final HvSwitchBlockEntity be;

    protected HvSparkSoundInstance(HvSwitchBlockEntity be) {
        super(ModdedSoundEvents.SPARK.getMainEvent(), SoundSource.BLOCKS, be.getLevel().random);
        this.be = be;
        var pos = be.getBlockPos().getCenter();
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.attenuation = Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.3F;
    }

    @Override
    public void tick() {
        if(!be.isSparking()) {
            stop();
            return;
        }
        final float RANDOM_RANGE = 0.05f;
//        this.pitch += (random.nextFloat() * RANDOM_RANGE) - (RANDOM_RANGE * 0.5f);
//        if(this.pitch > 1.1f)
//            this.pitch = 1.1f;
//        if(this.pitch < 0.9f)
//            this.pitch = 0.9f;
        this.volume += (random.nextFloat() * RANDOM_RANGE) - (RANDOM_RANGE * 0.5f);
        if(this.volume > 0.4f)
            this.volume = 0.4f;
        if(this.volume < 0.2f)
            this.volume = 0.2f;
    }
}
