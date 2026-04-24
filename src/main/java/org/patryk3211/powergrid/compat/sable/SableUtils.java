package org.patryk3211.powergrid.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SableUtils {
    public static SableProxy PROXY = null;

    public static boolean sameSubLevel(Level level, Vec3 pos1, Vec3 pos2) {
        return SableCompanion.INSTANCE.getContaining(level, pos1) == SableCompanion.INSTANCE.getContaining(level, pos2);
    }

    public static boolean sameSubLevel(Level level, BlockPos pos1, BlockPos pos2) {
        return SableCompanion.INSTANCE.getContaining(level, pos1) == SableCompanion.INSTANCE.getContaining(level, pos2);
    }

    public static double projectedDistance(Level level, Vec3 pos1, Vec3 pos2) {
        var projPos1 = SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos1);
        var projPos2 = SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos2);
        return projPos1.distanceTo(projPos2);
    }

    public static void makeFullProxy() {
        PROXY = new SableProxyImpl();
        PROXY.init();
    }

    public static void makeDummyProxy() {
        PROXY = new DummySableProxy();
    }
}
