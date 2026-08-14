package org.patryk3211.powergrid.electricity.particles;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

public class HvSparkSoundInstance extends AbstractTickableSoundInstance {
    private final BlockEntity be;
    private final SparkSoundOwner owner;

    public <T extends BlockEntity&SparkSoundOwner> HvSparkSoundInstance(T be) {
        super(ModdedSoundEvents.SPARK.getMainEvent(), SoundSource.BLOCKS, be.getLevel().random);
        this.be = be;
        this.owner = be;
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
        if(be.isRemoved() || !owner.isSparking()) {
            stop();
            return;
        }
        final float RANDOM_RANGE = 0.05f;
        this.volume += (random.nextFloat() * RANDOM_RANGE) - (RANDOM_RANGE * 0.5f);
        if(this.volume > 0.4f)
            this.volume = 0.4f;
        if(this.volume < 0.2f)
            this.volume = 0.2f;
    }
}
