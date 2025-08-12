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
package org.patryk3211.powergrid.collections.fabric;

import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.util.entry.FluidEntry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedFluidsImpl {
    public static final FluidEntry<SimpleFlowableFluid.Flowing> ACID =
            REGISTRATE.fluid("acid", new ResourceLocation("block/water_still"), new ResourceLocation("block/water_flowing"))
                    .renderType(() -> RenderType::translucent)
                    .tag(FluidTags.WATER)
                    .source(SimpleFlowableFluid.Source::new)
                    .fluidAttributes(() -> new FluidVariantAttributeHandler() { })
                    .onRegisterAfter(Registries.FLUID, flowing -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> registerSimpleFluidRenderer(flowing, 0xFFFFEE80)))
                    .register();

    @Environment(EnvType.CLIENT)
    private static void registerSimpleFluidRenderer(SimpleFlowableFluid.Flowing fluid, int tint) {
        var handler = SimpleFluidRenderHandler.coloredWater(tint);
        FluidRenderHandlerRegistry.INSTANCE.register(fluid.getSource(), fluid.getFlowing(), handler);
    }

    public static Fluid acid() {
        return ACID.getSource().getSource();
    }

    public static Fluid acidFlowing() {
        return ACID.getSource().getFlowing();
    }

    public static void platformInit() {

    }
}
