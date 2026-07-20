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
import com.tterrag.registrate.providers.ProviderType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.createmod.catnip.lang.FontHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.fabric.ComponentRegistryImpl;
import org.patryk3211.powergrid.collections.ItemDisplay;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedCommands;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.fabric.ModdedSoundEventsImpl;
import org.patryk3211.powergrid.commands.PerformanceCommand;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.registry.WireItemEntry;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardMenu;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlockEntity;
import org.patryk3211.powergrid.kinetics.punchcard.fabric.PunchCardMenuImpl;
import org.patryk3211.powergrid.kinetics.punchcard.fabric.PunchCardReaderBlockEntityImpl;
import org.patryk3211.powergrid.utility.proxy.SubstituteBlockEntityProvider;

public class PowerGridImpl implements ModInitializer {
    public void onInitialize() {
        ComponentRegistryImpl.REGISTRY = FabricRegistryBuilder
                .createSimple(ComponentRegistry.REGISTRY_KEY)
                .buildAndRegister();
        DynamicRegistries.registerSynced(ComponentRegistry.ITEM_REGISTRY_KEY, ComponentRegistry.ITEM_CODEC,
                DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY);
        DynamicRegistries.registerSynced(WireRegistry.KEY, WireItemEntry.CODEC,
                DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY);

        var tab = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModdedBlocks.ELECTRIC_MOTOR))
                .displayItems(new ItemDisplay.BaseItemDisplay(true))
                .title(Component.translatable("itemGroup.powergrid.main"))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, PowerGrid.asResource("main"), tab);
        ArgumentTypeRegistry.registerArgumentType(PowerGrid.asResource("performance_counter"),
                PerformanceCommand.PerformanceCounterArgument.class,
                SingletonArgumentInfo.contextFree(PerformanceCommand.PerformanceCounterArgument::new));

        SubstituteBlockEntityProvider.INSTANCE.register(PunchCardReaderBlockEntity.class, PunchCardReaderBlockEntityImpl::new);
        PunchCardMenu.CONSTRUCTORS = PunchCardMenuImpl.constructors();
        PowerGrid.init();

        ModdedSoundEventsImpl.register();
        PowerGrid.REGISTRATE.addLang("itemGroup", PowerGrid.asResource("main"), "Power Grid");

        // Register platform events
        ServerEntityEvents.ENTITY_UNLOAD.register(BaseWireEntity::entityUnload);
        ServerChunkEvents.CHUNK_LOAD.register(PowerGridImpl::chunkLoad);
        CommandRegistrationEvent.EVENT.register(ModdedCommands::register);
    }

    private static void chunkLoad(ServerLevel level, LevelChunk chunk) {
        var global = GlobalElectricNetworks.getWorldNetworks((LevelAccessor) level);
        if(global == null)
            return;
        global.chunkLoaded(chunk.getPos());
    }

    public static AbstractPowerGridRegistrate createRegistrate() {
        AbstractPowerGridRegistrate.COMPONENT_ITEMS = ProviderType.register("component_items", ComponentItemEntryProviderImpl::new);
        AbstractPowerGridRegistrate.WIRE_ITEMS = ProviderType.register("wire_types", WireItemEntryProviderImpl::new);
        return FabricPowerGridRegistrate.create(PowerGrid.MOD_ID)
                .setTooltipModifierFactory(item ->
                        new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                                .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                                .andThen(TooltipModifier.mapNull(ElectricProperties.create(item)))
                );
    }

    public static void finalizeRegistrate() {
        PowerGrid.REGISTRATE.register();
    }
}
