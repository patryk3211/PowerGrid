package org.patryk3211.powergrid.compat.vs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DummyVSProxy implements VSProxy {
    @Override
    public boolean sameShip(Level level, Vec3 pos1, Vec3 pos2) {
        return false;
    }

    @Override
    public Vec3 projectToWorld(Level level, Vec3 pos) {
        return pos;
    }

    @Override
    public double projectedDistance(Level level, Vec3 pos1, Vec3 pos2) {
        return pos1.distanceTo(pos2);
    }

    @Override
    public boolean inShip(Entity entity) {
        return false;
    }

    @Override
    public Vec3 getVelocity(Level level, Vec3 pos) {
        return Vec3.ZERO;
    }

    @Override
    public void init() {

    }
}
