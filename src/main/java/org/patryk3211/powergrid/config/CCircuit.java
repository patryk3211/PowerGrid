package org.patryk3211.powergrid.config;
import net.createmod.catnip.config.ConfigBase;
public class CCircuit extends ConfigBase{
    public final ConfigInt traceRed = i(255,0,255,"traceRed", CCircuit.Comments.traceRed);
    public final ConfigInt traceGreen = i(255,0,255,"traceRed", CCircuit.Comments.traceGreen);
    public final ConfigInt traceBlue = i(255,0,255,"traceRed", CCircuit.Comments.traceBlue);
    public final ConfigInt traceAlpha = i(255,0,255,"traceRed", CCircuit.Comments.traceAlpha);
    @Override
    public String getName() {
        return "CCircuit";
    }

    private static class Comments {
        public static final String traceRed = "Amount of red in traces";
        public static final String traceGreen = "Amount of green in traces";
        public static final String traceBlue = "Amount of blue in traces";
        public static final String traceAlpha = "Amount of alpha in traces";
    }
}
