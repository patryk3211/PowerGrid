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
package org.patryk3211.powergrid.kinetics.base;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public abstract class GeneratorBlockEntity extends ElectricKineticBlockEntity {
    private static final float TIME_STEP = 1f / 20;

    private final float windingResistance;
    private final float armatureInertia;
    // Loop area * number of turns
    private final float coilConstant;

    public GeneratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state,
                                float windingResistance, float armatureInertia, float coilConstant) {
        super(typeIn, pos, state);
        this.windingResistance = windingResistance;
        this.armatureInertia = armatureInertia;
        this.coilConstant = coilConstant;
    }

    public abstract float windingCurrent();
    public abstract float fieldStrength();
    public abstract void updateVoltage(float voltage);

    public float emfVoltage() {
        return -speed * fieldStrength() * coilConstant;
    }

    @Override
    public void tick() {
        super.tick();

        float current = windingCurrent();
        float torque = coilConstant * fieldStrength() * current;

        speed += torque / armatureInertia * TIME_STEP;
        updateVoltage(emfVoltage());
    }
}
