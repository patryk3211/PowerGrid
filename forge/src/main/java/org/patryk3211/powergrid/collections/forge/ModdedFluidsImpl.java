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
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedFluidsImpl {
    public static final FluidEntry<ForgeFlowingFluid.Flowing> ACID =
            REGISTRATE.fluid("acid",
                            Identifier.of("minecraft", "block/water_still"),
                            Identifier.of("minecraft", "block/water_flowing"),
                            AcidFluidType::new)
//                    .renderType(() -> RenderLayer::getTranslucent)
                    .tag(FluidTags.WATER)
//                    .fluidAttributes(() -> new FluidVariantAttributeHandler() { })
//                    .onRegisterAfter(RegistryKeys.FLUID, flowing -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> registerSimpleFluidRenderer(flowing, 0xFFFFEE80)))
                    .register();

//    @Environment(EnvType.CLIENT)
//    private static void registerSimpleFluidRenderer(SimpleFlowableFluid.Flowing fluid, int tint) {
//        var handler = SimpleFluidRenderHandler.coloredWater(tint);
//        FluidRenderHandlerRegistry.INSTANCE.register(fluid.getStill(), fluid.getFlowing(), handler);
//    }

    public static Fluid acid() {
        return ACID.getSource().getStill();
    }

    public static Fluid acidFlowing() {
        return ACID.getSource().getFlowing();
    }

    public static void platformInit() {

    }

    public static class AcidFluidType extends AllFluids.TintedFluidType {
        public AcidFluidType(Properties properties, Identifier still, Identifier flowing) {
            super(properties, still, flowing);
        }

        @Override
        protected int getTintColor(FluidStack stack) {
            return 0xFFFFEE80;
        }

        @Override
        protected int getTintColor(FluidState state, BlockRenderView getter, BlockPos pos) {
            return 0xFFFFEE80;
        }
    }
}
