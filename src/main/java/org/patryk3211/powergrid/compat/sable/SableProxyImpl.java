package org.patryk3211.powergrid.compat.sable;

import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SableProxyImpl implements SableProxy {
    @Override
    public void setSubLevelTracking(Entity entity, SubLevelAccess subLevel) {
        if(entity instanceof EntityStickExtension sticky) {
            Vec3 plotPos = subLevel.logicalPose().transformPositionInverse(entity.position());
            sticky.sable$setPlotPosition(plotPos);
        }
    }
}
