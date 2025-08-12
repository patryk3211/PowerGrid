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
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.forge.ComponentRegistryImpl;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.collections.forge.ModdedSoundEventsImpl;
import org.patryk3211.powergrid.data.BlockTagProvider;
import org.patryk3211.powergrid.data.ItemTagProvider;
import org.patryk3211.powergrid.data.recipe.forge.MixingRecipes;
import org.patryk3211.powergrid.data.recipes.*;
import org.patryk3211.powergrid.ponder.PowerGridPonderPlugin;

import java.util.function.BiConsumer;

@Mod(PowerGrid.MOD_ID)
public class PowerGridImpl {
    public static FMLJavaModLoadingContext context;
    public static IEventBus bus;

    public PowerGridImpl() {
        context = FMLJavaModLoadingContext.get();
        bus = context.getModEventBus();
        bus.register(PowerGridImpl.class);
        EventBuses.registerModEventBus(PowerGrid.MOD_ID, bus);

        PowerGrid.init();

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
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();

        PowerGrid.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> langConsumer = provider::add;
            provideDefaultLang("interface", langConsumer);
            provideDefaultLang("messages", langConsumer);
            provideDefaultLang("tooltips", langConsumer);

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

        generator.addProvider(true, new BlockTagProvider(output, event.getLookupProvider()));
        generator.addProvider(true, new ItemTagProvider(output, event.getLookupProvider()));
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
        return ForgePowerGridRegistrate.create(PowerGrid.MOD_ID)
                .setTooltipModifierFactory(item ->
                        new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                                .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                                .andThen(TooltipModifier.mapNull(ElectricProperties.create(item)))
                )
                .defaultCreativeTab("power_grid" , builder -> builder
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.powergrid.main"))
                        .icon(() -> new ItemStack(ModdedItems.WIRE)))
//                .lang(tab -> "itemGroup.powergrid.main", "Create: Power Grid")
                .build();
    }

    public static void finalizeRegistrate() {
        ((ForgePowerGridRegistrate) PowerGrid.REGISTRATE).registerEventListeners(bus);
    }
}
