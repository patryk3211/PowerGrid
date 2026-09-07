package org.patryk3211.powergrid.electricity.solarpanel.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SolarBiomeEntry(
    String biome, // biome you wish to override
    boolean overrideTemp, // will stay at biome default unless overridden
    float biomeTemp, // self-explanatory
    boolean overrideSolarConstant, // will stay at default unless overridden
    float solarConstant, // Overworld default is 1361, higher is more power
    boolean enableFullRotation, // makes it so solar panels don't stop making power past the horizons (for space mods)
    boolean disableWeather, // disables weather effects on solar panel output
    boolean disableAtmosphere // disables atmospheric effects on solar panel output
) {
    public static final Codec<SolarBiomeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("biome").forGetter(SolarBiomeEntry::biome),
            Codec.BOOL.fieldOf("overrideTemp").forGetter(SolarBiomeEntry::overrideTemp),
            Codec.FLOAT.fieldOf("biomeTemp").forGetter(SolarBiomeEntry::biomeTemp),
            Codec.BOOL.fieldOf("overrideSolarConstant").forGetter(SolarBiomeEntry::overrideSolarConstant),
            Codec.FLOAT.fieldOf("solarConstant").forGetter(SolarBiomeEntry::solarConstant),
            Codec.BOOL.fieldOf("enableFullRotation").forGetter(SolarBiomeEntry::enableFullRotation),
            Codec.BOOL.fieldOf("disableWeather").forGetter(SolarBiomeEntry::disableWeather),
            Codec.BOOL.fieldOf("disableAtmosphere").forGetter(SolarBiomeEntry::disableAtmosphere)
            ).apply(instance, SolarBiomeEntry::new));
}
