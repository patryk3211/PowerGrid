package org.patryk3211.powergrid.compat.sable;

import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.compat.sable.fan.ElectricFanPropellerBlockEntity;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlockEntity;
import org.patryk3211.powergrid.utility.proxy.SubstituteBlockEntityProvider;

public class SableProxyImpl implements SableProxy {
    @Override
    public void init() {
        SubstituteBlockEntityProvider.INSTANCE.register(ElectricFanBlockEntity.class, ElectricFanPropellerBlockEntity::new);
    }

    @Override
    public void setSubLevelTracking(Entity entity, SubLevelAccess subLevel) {
        if(entity instanceof EntityStickExtension sticky) {
            if(subLevel == null) {
                sticky.sable$setPlotPosition(null);
            } else {
                Vec3 plotPos = subLevel.logicalPose().transformPositionInverse(entity.position());
                sticky.sable$setPlotPosition(plotPos);
            }
        }
    }
}
