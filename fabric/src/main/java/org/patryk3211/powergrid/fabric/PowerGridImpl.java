/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.fabric;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.fabric.ComponentRegistryImpl;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.fabric.ModdedSoundEventsImpl;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

public class PowerGridImpl implements ModInitializer {
    public void onInitialize() {
        ComponentRegistryImpl.REGISTRY = FabricRegistryBuilder
                .createSimple(ComponentRegistry.REGISTRY_KEY)
                .buildAndRegister();

        PowerGrid.init();

        ModdedSoundEventsImpl.register();

        // Register platform events
        ServerEntityEvents.ENTITY_UNLOAD.register(WireEntity::entityUnload);
        ServerChunkEvents.CHUNK_LOAD.register(PowerGridImpl::chunkLoad);
    }

    private static void chunkLoad(ServerLevel level, LevelChunk chunk) {
        var global = GlobalElectricNetworks.getWorldNetworks(level);
        if(global == null)
            return;
        global.chunkLoaded(chunk.getPos());
    }

    public static AbstractPowerGridRegistrate createRegistrate() {
        return FabricPowerGridRegistrate.create(PowerGrid.MOD_ID)
                .setTooltipModifierFactory(item ->
                        new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                                .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                                .andThen(TooltipModifier.mapNull(ElectricProperties.create(item)))
                )
                .defaultCreativeTab("main", builder -> builder
                        .title(Component.translatable("itemGroup.powergrid.main"))
                        .icon(() -> new ItemStack(ModdedItems.WIRE)))
                .build();
    }

    public static void finalizeRegistrate() {
        PowerGrid.REGISTRATE.register();
    }
}
