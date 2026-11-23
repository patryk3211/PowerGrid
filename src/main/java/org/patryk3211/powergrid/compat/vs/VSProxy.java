package org.patryk3211.powergrid.compat.vs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface VSProxy {
    boolean sameShip(Level level, Vec3 pos1, Vec3 pos2);

    Vec3 projectToWorld(Level level, Vec3 pos);

    double projectedDistance(Level level, Vec3 pos1, Vec3 pos2);

    boolean inShip(Entity entity);

    Vec3 getVelocity(Level level, Vec3 pos);

    void init();
}
