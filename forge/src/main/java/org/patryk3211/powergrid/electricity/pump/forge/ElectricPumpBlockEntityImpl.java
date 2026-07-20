package org.patryk3211.powergrid.electricity.pump.forge;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ElectricPumpBlockEntityImpl {
    public static boolean platformEndpointCheck(BlockEntity be, Direction face) {
        var capability = be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), face.getOpposite());
        return capability != null;
    }
}
