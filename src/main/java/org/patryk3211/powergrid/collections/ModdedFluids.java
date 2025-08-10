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
package org.patryk3211.powergrid.collections;

import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedFluids {
    public static final FluidEntry<SimpleFlowableFluid.Flowing> ACID =
            REGISTRATE.fluid("acid", new Identifier("block/water_still"), new Identifier("block/water_flowing"))
                    .renderType(() -> RenderLayer::getTranslucent)
                    .tag(FluidTags.WATER)
//                    .fluidAttributes(() -> new Create)
//                    .source(properties -> {
//
////                        new ArchitecturyFlowingFluid.Source(new SimpleArchitecturyFluidAttributes())
//                    })
//                    .source(SimpleFlowableFluid.Source::new)

//                    .fluidAttributes(() -> new FluidVariantAttributeHandler() { })
                    .onRegisterAfter(RegistryKeys.FLUID, flowing -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> registerSimpleFluidRenderer(flowing, 0xFFFFEE80)))
                    .register();

    @Environment(EnvType.CLIENT)
    private static void registerSimpleFluidRenderer(SimpleFlowableFluid.Flowing fluid, int tint) {
//        var handler = SimpleFluidRenderHandler.coloredWater(tint);
//        FluidRenderHandlerRegistry.INSTANCE.register(fluid.getStill(), fluid.getFlowing(), handler);
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
