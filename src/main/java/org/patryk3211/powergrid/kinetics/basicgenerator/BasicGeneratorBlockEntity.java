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
package org.patryk3211.powergrid.kinetics.basicgenerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

public class BasicGeneratorBlockEntity extends ElectricKineticBlockEntity {
    private VoltageSourceNode sourceNode;

    public BasicGeneratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        var outputCurrent = sourceNode.getCurrent();
        var powerDrop = outputCurrent * outputCurrent * BasicGeneratorBlock.resistance();
        applyLostPower(powerDrop);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 2.0f, 0.1f);
    }

    private static float ratio() {
        return ModdedConfigs.server().kinetics.basicGeneratorConversionRatio.getF();
    }

    @Override
    protected void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        sourceNode.setVoltage(getSpeed() * ratio());
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        sourceNode.setVoltage(getSpeed() * ratio());
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        sourceNode = builder.addInternalNode(VoltageSourceNode.class);
        var positive = builder.addExternalNode();
        var negative = builder.addExternalNode();
        builder.couple(1, BasicGeneratorBlock.resistance(), sourceNode, positive, negative);
    }
}
