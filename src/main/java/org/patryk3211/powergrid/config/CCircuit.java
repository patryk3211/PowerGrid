package org.patryk3211.powergrid.config;
import net.createmod.catnip.config.ConfigBase;
public class CCircuit extends ConfigBase{
    public final ConfigBool HighContrastTraces = b(false,"HighContrastTraces", Comments.HighContrastTraces);
    public final ConfigInt traceRedTop = i(255,0,255,"traceRedTop", Comments.traceRed);
    public final ConfigInt traceGreenTop = i(255,0,255,"traceGreenTop", Comments.traceGreen);
    public final ConfigInt traceBlueTop = i(255,0,255,"traceBlueTop", Comments.traceBlue);
    public final ConfigInt traceAlphaTop = i(255,0,255,"traceAlphaTop", Comments.traceAlpha);
    public final ConfigInt traceAlphaTopBehind = i(128,0,255,"traceAlphaTopBehind", Comments.traceAlpha);

    public final ConfigInt traceRedBottom = i(255,0,255,"traceRedBottom", Comments.traceRed);
    public final ConfigInt traceGreenBottom = i(255,0,255,"traceGreenBottom", Comments.traceGreen);
    public final ConfigInt traceBlueBottom= i(255,0,255,"traceBlueBottom", Comments.traceBlue);
    public final ConfigInt traceAlphaBottom = i(255,0,255,"traceAlphaBottom", Comments.traceAlphaBack);
    public final ConfigInt traceAlphaBottomBehind = i(128,0,255,"traceAlphaBottomBehind", Comments.traceAlphaBack);

    @Override
    public String getName() {
        return "Circuit Design Table";
    }

    private static class Comments {
        public static final String traceRed = "Amount of red in traces";
        public static final String traceGreen = "Amount of green in traces";
        public static final String traceBlue = "Amount of blue in traces";
        public static final String traceAlpha = "Amount of alpha in traces";
        public static final String traceAlphaBack = "Amount of alpha in traces on the back";
        public static final String HighContrastTraces = "Higher contrast between traces in the circuit editor";
    }
}
