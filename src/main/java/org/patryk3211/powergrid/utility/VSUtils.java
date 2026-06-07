package org.patryk3211.powergrid.utility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class VSUtils {
    public static boolean sameShip(Level level, Vec3 pos1, Vec3 pos2) {
        return VSGameUtilsKt.getShipManagingPos(level, pos1) == VSGameUtilsKt.getShipManagingPos(level, pos2);
    }

    public static Vec3 projectToWorld(Level level, Vec3 pos) {
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship == null) {
            return pos;
        }
        return VectorConversionsMCKt.toMinecraft(ship.getShipToWorld().transformPosition(VectorConversionsMCKt.toJOML(pos)));
    }

    public static double projectedDistance(Level level, Vec3 pos1, Vec3 pos2) {
        var projPos1 = projectToWorld(level, pos1);
        var projPos2 = projectToWorld(level, pos2);
        return projPos1.distanceTo(projPos2);
    }

    public static boolean inShip(Level level, Vec3 pos) {
        return VSGameUtilsKt.isBlockInShipyard(level, pos);
    }

    public static boolean inShip(Entity entity) {
        return VSGameUtilsKt.getShipManaging(entity) != null;
    }

    public static Vec3 getVelocity(Level level, Vec3 pos) {
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            return VectorConversionsMCKt.toMinecraft(ship.getVelocity());
        } else {
            return Vec3.ZERO;
        }
    }
}
