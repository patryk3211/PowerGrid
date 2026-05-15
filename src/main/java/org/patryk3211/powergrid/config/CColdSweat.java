package org.patryk3211.powergrid.config;

import net.createmod.catnip.config.ConfigBase;

public class CColdSweat extends ConfigBase {
    public final ConfigFloat coldSweatTempScalar = f(0.04f, 0, "coldSweatTempScalar", Comments.coldSweatTempScalar);
    public final ConfigFloat coldSweatRangeScalar = f(0.5f, 0, "coldSweatRangeScalar", Comments.coldSweatRangeScalar);

    @Override
    public String getName() {
        return "cold_sweat";
    }

    private static class Comments {
        public static final String coldSweatTempScalar = "Factor used to calculate the heat that players receive, in ºMC per 100ºC";
        public static final String coldSweatRangeScalar = "Factor used to calculate the maximum range at which players receive heat, in blocks per 100ºC";
    }
}
