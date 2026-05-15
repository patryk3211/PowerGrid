package org.patryk3211.powergrid.compat.cold_sweat;

import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class ColdSweatBridge {
    @SubscribeEvent
    public static void onBlockTempsRegister(BlockTempRegisterEvent event) {
        event.register(new ElectricBlockTemp());
    }
}
