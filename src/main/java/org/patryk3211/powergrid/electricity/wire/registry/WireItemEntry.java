package org.patryk3211.powergrid.electricity.wire.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record WireItemEntry(
        float resistancePerItem,
        float itemsPerMeter,
        float maximumLength,
        float thermalMass,
        float maximumCurrent,
        ResourceLocation texture,
        float horizontalCoefficient,
        float verticalCoefficient,
        float wireThickness,
        boolean colorable,
        boolean cord,
        boolean insulated) {

    public static final Codec<WireItemEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("resistancePerItem").forGetter(WireItemEntry::resistancePerItem),
            Codec.FLOAT.fieldOf("itemsPerMeter").forGetter(WireItemEntry::itemsPerMeter),
            Codec.FLOAT.fieldOf("maximumLength").forGetter(WireItemEntry::maximumLength),
            Codec.FLOAT.fieldOf("thermalMass").forGetter(WireItemEntry::thermalMass),
            Codec.FLOAT.fieldOf("maximumCurrent").forGetter(WireItemEntry::maximumCurrent),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(WireItemEntry::texture),
            Codec.FLOAT.fieldOf("horizontalCoefficient").forGetter(WireItemEntry::horizontalCoefficient),
            Codec.FLOAT.fieldOf("verticalCoefficient").forGetter(WireItemEntry::verticalCoefficient),
            Codec.FLOAT.fieldOf("wireThickness").forGetter(WireItemEntry::wireThickness),
            Codec.BOOL.fieldOf("colorable").forGetter(WireItemEntry::colorable),
            Codec.BOOL.fieldOf("cord").forGetter(WireItemEntry::cord),
            Codec.BOOL.fieldOf("insulated").forGetter(WireItemEntry::insulated)
    ).apply(instance, WireItemEntry::new));

    public float dissipationFactor() {
        return maximumCurrent * maximumCurrent * resistancePerItem / 150;
    }
}
