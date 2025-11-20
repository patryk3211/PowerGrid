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
package org.patryk3211.powergrid.kinetics.variac;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.kinetics.base.TunedBlockEntity;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.sound.SoundScapes;

import java.util.List;

public class VariacBlockEntity extends TunedBlockEntity implements IHaveGoggleInformation {
    public static final float PRIMARY_TURNS = 25;
    public static final float CORE_AL = 1.5f;
    public static final float COUPLING_FACTOR = 0.9999f;
    public static final float PRIMARY_INDUCTANCE = PRIMARY_TURNS * PRIMARY_TURNS * CORE_AL;

    protected ElectricWire primaryStray;
    protected ElectricWire mutualInductance;
    protected TransformerCoupling coupling;

    public float lastCurrent;

    public VariacBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    public float getRatio() {
        if(arm == null)
            return 0.01f;
        return arm.getValue() * 0.99f + 0.01f;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);

        var secondaryTurns = getRatio() * PRIMARY_TURNS;
        float secondaryInductance = secondaryTurns * secondaryTurns * CORE_AL;

        float mutualInductance = secondaryInductance * COUPLING_FACTOR;

        float ratio = getRatio();
        float primaryStray = PRIMARY_INDUCTANCE - mutualInductance;
        float secondaryStray = secondaryInductance - ratio * ratio * mutualInductance;

        var Tnode = builder.addInternalNode();

        this.primaryStray = builder.connect(primaryStray, builder.terminalNode(0), Tnode);
        this.mutualInductance = builder.connect(mutualInductance, Tnode, builder.terminalNode(1));
        this.coupling = builder.couple(ratio, secondaryStray * ratio * ratio, Tnode, builder.terminalNode(1),
                builder.terminalNode(2), builder.terminalNode(1));
    }

    @Override
    public void refreshParameters() {
        var secondaryTurns = getRatio() * PRIMARY_TURNS;
        float secondaryInductance = secondaryTurns * secondaryTurns * CORE_AL;
        float ratio = getRatio();

        float mutualInductance = secondaryInductance / (ratio * ratio) * COUPLING_FACTOR;
        float primaryStray = PRIMARY_INDUCTANCE - mutualInductance;
        float secondaryStray = secondaryInductance - ratio * ratio * mutualInductance;
        this.primaryStray.setResistance(primaryStray);
        this.mutualInductance.setResistance(mutualInductance);
        this.coupling.setRatio(ratio);
        this.coupling.setResistance(secondaryStray * ratio * ratio);
    }

    @Override
    public void tick() {
        float power = 0;
        lastCurrent = 0;
        if(primaryStray != null && primaryStray.isConverged()) {
            var I1 = primaryStray.current();
            power += (float) (I1 * I1 * primaryStray.getResistance());
            lastCurrent += Math.abs(I1);
        }
        if(mutualInductance != null && mutualInductance.isConverged()) {
            var I3 = mutualInductance.current();
            power += (float) (I3 * I3 * mutualInductance.getResistance());
            lastCurrent += Math.abs(I3);
        }
        if(thermalBehaviour != null)
            thermalBehaviour.applyTickPower(power);
        super.tick();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        super.tickAudio();
        SoundScapes.play(SoundScapes.AmbienceGroup.HUM, worldPosition, 1, lastCurrent / 20);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!isPlayerSneaking)
            return false;

        Lang.builder().translate("gui.transformer.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.transformer.ratio")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var ratio = Lang.number(1 / getRatio()).add(Component.literal(":1"));
        ratio.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        return true;
    }
}
