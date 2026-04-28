package org.patryk3211.powergrid.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoostData(int durability) {
    public static final Codec<BoostData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("durability").forGetter(BoostData::durability)
    ).apply(instance, BoostData::new));

    public static BoostData of(int durability) {
        return new BoostData(durability);
    }
}
