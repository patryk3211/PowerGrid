package org.patryk3211.powergrid.electricity.pump.forge;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class ElectricPumpBlockEntityImpl {
    public static boolean platformEndpointCheck(BlockEntity be, Direction face) {
        LazyOptional<IFluidHandler> capability = be.getCapability(ForgeCapabilities.FLUID_HANDLER, face.getOpposite());
        return capability.isPresent();
    }
}
