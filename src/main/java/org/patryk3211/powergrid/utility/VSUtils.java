package org.patryk3211.powergrid.utility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.compat.vs.DummyVSProxy;
import org.patryk3211.powergrid.compat.vs.VSProxy;
import org.patryk3211.powergrid.compat.vs.VSProxyImpl;

public class VSUtils {
    public static VSProxy PROXY = null;

    public static boolean sameShip(Level level, Vec3 pos1, Vec3 pos2) {
        return PROXY.sameShip(level, pos1, pos2);
    }

    public static Vec3 projectToWorld(Level level, Vec3 pos) {
        return PROXY.projectToWorld(level, pos);
    }

    public static double projectedDistance(Level level, Vec3 pos1, Vec3 pos2) {
        return PROXY.projectedDistance(level, pos1, pos2);
    }

    public static boolean inShip(Entity entity) {
        return PROXY.inShip(entity);
    }

    public static Vec3 getVelocity(Level level, Vec3 pos) {
        return PROXY.getVelocity(level, pos);
    }

    public static void makeFullProxy() {
        PROXY = new VSProxyImpl();
        PROXY.init();
    }

    public static void makeDummyProxy() {
        PROXY = new DummyVSProxy();
    }
}
