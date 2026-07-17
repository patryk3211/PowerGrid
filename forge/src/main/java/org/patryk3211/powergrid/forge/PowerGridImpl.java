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
package org.patryk3211.powergrid.forge;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.forge.ComponentRegistryImpl;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.collections.forge.ModdedSoundEventsImpl;
import org.patryk3211.powergrid.commands.PerformanceCommand;
import org.patryk3211.powergrid.compat.cold_sweat.ColdSweatBridge;
import org.patryk3211.powergrid.compat.tfmg.TFMGBridge;
import org.patryk3211.powergrid.compat.tfmg.TFMGProxyImpl;
import org.patryk3211.powergrid.data.BlockTagProvider;
import org.patryk3211.powergrid.data.EntityHandlerProvider;
import org.patryk3211.powergrid.data.ItemTagProvider;
import org.patryk3211.powergrid.data.recipe.forge.MixingRecipes;
import org.patryk3211.powergrid.data.recipes.*;
import org.patryk3211.powergrid.electricity.febridge.FEInverterBlockEntity;
import org.patryk3211.powergrid.electricity.febridge.forge.FEInverterBlockEntityImpl;
import org.patryk3211.powergrid.electricity.wire.registry.WireItemEntry;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardMenu;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlockEntity;
import org.patryk3211.powergrid.kinetics.punchcard.forge.PunchCardMenuImpl;
import org.patryk3211.powergrid.kinetics.punchcard.forge.PunchCardReaderBlockEntityImpl;
import org.patryk3211.powergrid.ponder.PowerGridPonderPlugin;
import org.patryk3211.powergrid.utility.proxy.ProxyProvider;
import org.patryk3211.powergrid.utility.proxy.SubstituteBlockEntityProvider;
import org.patryk3211.powergrid.utility.proxy.TFMGProxy;

import java.util.List;
import java.util.function.BiConsumer;

@Mod(PowerGrid.MOD_ID)
public class PowerGridImpl {
    public static ModLoadingContext context;
    public static IEventBus bus;

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PowerGrid.MOD_ID);
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, PowerGrid.MOD_ID);

    public PowerGridImpl() {
        context = ModLoadingContext.get();
        bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.register(PowerGridImpl.class);
        EventBuses.registerModEventBus(PowerGrid.MOD_ID, bus);

        if(Platform.isModLoaded("tfmg")) {
            TFMGBridge.init();
            ProxyProvider.add(TFMGProxy.class, new TFMGProxyImpl());
        }

        if (Platform.isModLoaded("cold_sweat")) {
            MinecraftForge.EVENT_BUS.register(ColdSweatBridge.class);
        }

        SubstituteBlockEntityProvider.INSTANCE.register(PunchCardReaderBlockEntity.class, PunchCardReaderBlockEntityImpl::new);
        SubstituteBlockEntityProvider.INSTANCE.register(FEInverterBlockEntity.class, FEInverterBlockEntityImpl::new);
        PunchCardMenu.CONSTRUCTORS = PunchCardMenuImpl.constructors();
        PowerGrid.init();

        TABS.register("main", () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(ModdedItems.WIRE))
                .displayItems(new ItemDisplay.BaseItemDisplay(true))
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.powergrid.main"))
                .build());
        COMMAND_ARGUMENT_TYPES.register("performance_counter", () -> ArgumentTypeInfos
                .registerByClass(PerformanceCommand.PerformanceCounterArgument.class,
                        SingletonArgumentInfo.contextFree(PerformanceCommand.PerformanceCounterArgument::new)));

        PowerGrid.REGISTRATE.addLang("itemGroup", PowerGrid.asResource("main"), "Power Grid");
        TABS.register(bus);
        COMMAND_ARGUMENT_TYPES.register(bus);

        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);

        // Client init
        EnvExecutor.runInEnv(Env.CLIENT, () -> PowerGridClientImpl::init);
    }

    @SubscribeEvent
    public static void newRegistryEvent(NewRegistryEvent event) {
        event.<Component>create(
                RegistryBuilder.of(ComponentRegistry.REGISTRY_KEY.location()),
                registry -> ComponentRegistryImpl.REGISTRY = registry
        );
    }

    @SubscribeEvent
    public static void newDynamicRegistryEvent(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ComponentRegistry.ITEM_REGISTRY_KEY, ComponentRegistry.ITEM_CODEC, ComponentRegistry.ITEM_CODEC);
        event.dataPackRegistry(WireRegistry.KEY, WireItemEntry.CODEC, WireItemEntry.CODEC);
    }

    @SubscribeEvent
    public static void configLoad(ModConfigEvent.Loading event) {
        ModdedConfigs.onLoad(event.getConfig());
    }

    @SubscribeEvent
    public static void configReload(ModConfigEvent.Reloading event) {
        ModdedConfigs.onReload(event.getConfig());
    }

    @SubscribeEvent
    public static void soundEventRegister(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, ModdedSoundEventsImpl::register);
    }

    @SubscribeEvent
    public static void imcEnqueue(InterModEnqueueEvent event) {
        var forbiddenBlockEntities = List.of(
                ModdedBlockEntities.GENERATOR_CLUTCH,
//                ModdedBlockEntities.GENERATOR_ROTOR,
                ModdedBlockEntities.GENERATOR_INDUCTION_ROTOR,
                ModdedBlockEntities.GENERATOR_COMMUTATOR,
                ModdedBlockEntities.WINDING,
                ModdedBlockEntities.TRANSFORMER_MEDIUM,
                ModdedBlockEntities.HV_SWITCH,
                ModdedBlockEntities.DEVICE_CONNECTOR,
                ModdedBlockEntities.MULTIBLOCK_BATTERY,
                ModdedBlockEntities.CARBON_PILE,
                ModdedBlockEntities.CARBON_PILE_COIL
        );
        forbiddenBlockEntities.stream()
                .map(entry -> entry.getId().toString())
                .forEach(id -> InterModComms.sendTo("carryon", "blacklistBlock", () -> id));
        var forbiddenEntities = List.of(
                ModdedEntities.BLOCK_WIRE,
                ModdedEntities.HANGING_WIRE,
                ModdedEntities.CORD_ENTITY,
                ModdedEntities.STRING_LIGHT_CORD,
                ModdedEntities.ZAP_PROJECTILE
        );
        forbiddenEntities.stream()
                .map(entry -> entry.getId().toString())
                .forEach(id -> InterModComms.sendTo("carryon", "blacklistEntity", () -> id));
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();

        PowerGrid.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> langConsumer = provider::add;
            provideDefaultLang("interface", langConsumer);
            provideDefaultLang("messages", langConsumer);
            provideDefaultLang("tooltips", langConsumer);
            provideDefaultLang("components", langConsumer);
            provideDefaultLang("pads", langConsumer);

            providePonderLang(langConsumer);
            ModdedSoundEvents.provideLang(langConsumer);
        });

        generator.addProvider(true, new CookingRecipes(output));
        generator.addProvider(true, new CraftingRecipes(output));
        generator.addProvider(true, new CuttingRecipes(output));
        generator.addProvider(true, new ItemApplicationRecipes(output));
        generator.addProvider(true, new MagnetizingRecipes(output));
        generator.addProvider(true, new MechanicalCraftingRecipes(output));
        generator.addProvider(true, new MixingRecipes(output));
        generator.addProvider(true, new PressingRecipes(output));
        generator.addProvider(true, new SequencedAssemblyRecipes(output));
        generator.addProvider(true, new org.patryk3211.powergrid.data.recipe.forge.SequencedAssemblyRecipes(output));
        generator.addProvider(true, new DeployerApplicationRecipes(output));

        generator.addProvider(true, new BlockTagProvider(output, event.getLookupProvider()));
        generator.addProvider(true, new ItemTagProvider(output, event.getLookupProvider()));
        generator.addProvider(true, new EntityHandlerProvider(output));
        generator.addProvider(true, ModdedSoundEvents.provider(output));
    }

    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        var path = "assets/powergrid/lang/default/" + fileName + ".json";
        var jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null) {
            throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
        }
        var jsonObject = jsonElement.getAsJsonObject();
        for(var entry : jsonObject.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue().getAsString();
            consumer.accept(key, value);
        }
    }

    private static void providePonderLang(BiConsumer<String, String> consumer) {
        PonderIndex.addPlugin(new PowerGridPonderPlugin());
        PonderIndex.getLangAccess().provideLang(PowerGrid.MOD_ID, consumer);
    }

    public static AbstractPowerGridRegistrate createRegistrate() {
        AbstractPowerGridRegistrate.COMPONENT_ITEMS = ProviderType.register("component_items", ComponentItemEntryProviderImpl::new);
        AbstractPowerGridRegistrate.WIRE_ITEMS = ProviderType.register("wire_types", WireItemEntryProviderImpl::new);
        return ForgePowerGridRegistrate.create(PowerGrid.MOD_ID)
                .setTooltipModifierFactory(item ->
                        new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                                .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                                .andThen(TooltipModifier.mapNull(ElectricProperties.create(item)))
                );
    }

    public static void finalizeRegistrate() {
        ((ForgePowerGridRegistrate) PowerGrid.REGISTRATE).registerEventListeners(bus);
    }
}
