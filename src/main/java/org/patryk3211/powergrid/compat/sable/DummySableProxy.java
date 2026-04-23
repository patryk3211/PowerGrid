package org.patryk3211.powergrid.compat.sable;

import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.world.entity.Entity;

public class DummySableProxy implements SableProxy {
    @Override
    public void setSubLevelTracking(Entity entity, SubLevelAccess subLevel) { }
}
