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
package org.patryk3211.powergrid.collections.forge;

import com.simibubi.create.AllFluids;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.patryk3211.powergrid.PowerGrid;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedFluidsImpl {
    public static final FluidEntry<ForgeFlowingFluid.Flowing> ACID =
            REGISTRATE.fluid("acid",
                            ResourceLocation.tryBuild("minecraft", "block/water_still"),
                            ResourceLocation.tryBuild("minecraft", "block/water_flow"),
                            AcidFluidType::new)
                    .tag(FluidTags.create(PowerGrid.asResource("acid")))
                    .transform(translucent())
                    .register();

    private static <T extends ForgeFlowingFluid, P> NonNullUnaryOperator<FluidBuilder<T, P>> translucent() {
        return b -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                b.renderType(RenderType::translucent);
            });
            return b;
        };
    }

    public static Fluid acid() {
        return ACID.getSource().getSource();
    }

    public static Fluid acidFlowing() {
        return ACID.getSource().getFlowing();
    }

    public static void platformInit() {

    }

    public static class AcidFluidType extends AllFluids.TintedFluidType {
        public AcidFluidType(Properties properties, ResourceLocation still, ResourceLocation flowing) {
            super(properties, still, flowing);
        }

        @Override
        protected int getTintColor(FluidStack stack) {
            return 0xFFFFEE80;
        }

        @Override
        protected int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return 0xFFFFEE80;
        }
    }
}
