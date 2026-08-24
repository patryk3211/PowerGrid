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
package org.patryk3211.powergrid.kinetics.rheostat;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.resistor.ResistorValueBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.kinetics.base.TunedBlockEntity;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class RheostatBlockEntity extends TunedBlockEntity implements IHaveGoggleInformation {
    protected ResistorValueBehaviour value;

    protected ElectricWire half1;
    protected ElectricWire half2;

    public RheostatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        value = new ResistorValueBehaviour(Lang.translateDirect("devices.resistor.resistance"),
                this, new RheostatBox(), -2, 18);
        value.setValue(0);
        value.withCallback($ -> refreshParameters());
        behaviours.add(value);
        super.addBehaviours(behaviours);
    }

    @Override
    public @Nullable AThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    public float getRatio() {
        if(arm == null)
            return 0.01f;
        return arm.getValue() * 0.98f + 0.01f;
    }

    @Override
    public float resistance() {
        return value.getResistance();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);

        float ratio = getRatio();
        half1 = builder.connect((1 - ratio) * resistance(), builder.terminalNode(0), builder.terminalNode(1));
        half2 = builder.connect(ratio * resistance(), builder.terminalNode(1), builder.terminalNode(2));
    }

    @Override
    public void refreshParameters() {
        float ratio = getRatio();
        half1.setResistance((1 - ratio) * resistance());
        half2.setResistance(ratio * resistance());
    }

    @Override
    public void electricalTick() {
        applyPower(half1);
        applyPower(half2);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!isPlayerSneaking)
            return false;

        Lang.builder().translate("gui.rheostat.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.rheostat.ratio")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var ratio = getRatio();
        var ratioText = Lang.number(1 - ratio)
                .add(Component.literal(" : "))
                .add(Lang.number(ratio));
        ratioText.style(ChatFormatting.AQUA)
                .forGoggles(tooltip, 1);
        return true;
    }

    public static class RheostatBox extends CenteredSideValueBoxTransform {
        public RheostatBox() {
            super((state, dir) -> dir != state.getValue(RheostatBlock.HORIZONTAL_FACING) && dir.getAxis() != Direction.Axis.Y);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 6.0f, 13.5f);
        }
    }
}
