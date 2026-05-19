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
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;

public class LargeInductionRotorBlock extends InductionRotorBlock {
    public LargeInductionRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getInertia() {
        return ModdedConfigs.server().kinetics.generatorControls.generatorLargeInductionRotorInertia.getF();
    }

    @Override
    public BlockEntityType<? extends InductionRotorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR_LARGE_INDUCTION_ROTOR.get();
    }
}
