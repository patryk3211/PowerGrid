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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedFluidsImpl {
    public static final FluidEntry<BaseFlowingFluid.Flowing> ACID =
            REGISTRATE.fluid("acid",
                            ResourceLocation.tryBuild("powergrid", "block/acid_still"),
                            ResourceLocation.tryBuild("powergrid", "block/acid_flow"),
                            AcidFluidType::new)
                    .tag(FluidTags.create(PowerGrid.asResource("acid")))
                    .lang("Blazing Acid")
                    .source(BaseFlowingFluid.Source::new)
                        .bucket()
                        .lang("Blazing Acid Bucket")
                        .build()
                    .transform(translucent())
                    .register();

    private static <T extends BaseFlowingFluid, P> NonNullUnaryOperator<FluidBuilder<T, P>> translucent() {
        return b -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                b.renderType(() -> RenderType::translucent);
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
            return 0xFFFFFFFF;
        }

        @Override
        protected int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return 0xFFFFFFFF;
        }

        @Override
        public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
            if (entity.level().random.nextInt(15) >= 10) {
                entity.hurt(ModdedDamageTypes.ACID.simpleDamageSource(entity.level()), 2);
            }
            return false;
        }
    }
}
