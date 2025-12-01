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
package org.patryk3211.powergrid.electricity.carbonpile;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class CarbonPileCoilBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation, ElectricBehaviour.SyncAppender {
    private ElectricWire coil;
    private SwitchedWire pile;
    private float baseResistance;
    private float trim = 1;
    private float coilPull = 0;

    public CarbonPileCoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour.setSyncAppender(this);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(4);
        coil = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
        // Resistance values won't be valid here
        pile = builder.connectSwitch(1, builder.terminalNode(2), builder.terminalNode(3), false);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        baseResistance = tag.getFloat("Base");
        trim = tag.getFloat("Trim");
        coilPull = tag.getFloat("Coil");
        refreshResistance();
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        tag.putFloat("Trim", trim);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("Base", baseResistance);
        tag.putFloat("Trim", trim);
        tag.putFloat("Coil", coilPull);
    }

    public static float gain() {
        return ModdedConfigs.server().electricity.carbonPileGain.getF();
    }

    public void refreshResistance() {
        var R = baseResistance * trim * (1 + Math.sqrt(coilPull * coilPull * coilPull) * gain());
        pile.setState(R > 0);
        if(R > 0) {
            // Max coil current makes the resistance twice as high
            pile.setResistance(R);
        }
    }

    public void pileChanged() {
        assert level != null;
        baseResistance = 0;
        var pos = worldPosition.above();
        BlockState state;
        while(ModdedBlocks.CARBON_PILE.has(state = level.getBlockState(pos))) {
            baseResistance += ResistanceValues.get(state.getBlock());
            pos = pos.above();
        }
        if(baseResistance == 0) {
            pile.setState(false);
        }
        trim = 1.0f;
        refreshResistance();
        notifyUpdate();
    }

    @Override
    public void electricalTick() {
        applyPower(coil);
        if(coil.isConverged()) {
            coilPull = Mth.clamp(Math.abs(coil.current()) * 5, 0, 2) * 0.5f + coilPull * 0.5f;
            refreshResistance();
            setUnsaved();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!pile.getState())
            return false;
        Lang.translate("gui.carbon_pile.info_header")
                .forGoggles(tooltip);

        Lang.translate("gui.carbon_pile.min_resistance")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        Lang.numberConstant(baseResistance * trim)
                .style(ChatFormatting.AQUA)
                .add(Component.literal(" "))
                .add(Unit.RESISTANCE.get())
                .forGoggles(tooltip, 1);

        Lang.translate("gui.carbon_pile.current_resistance")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        Lang.numberConstant(pile.getResistance())
                .style(ChatFormatting.AQUA)
                .add(Component.literal(" "))
                .add(Unit.RESISTANCE.get())
                .forGoggles(tooltip, 1);
        return true;
    }

    public float getTrim() {
        return trim;
    }

    public void setTrim(float trim) {
        this.trim = trim;
        refreshResistance();
        notifyUpdate();
    }

    public AbstractElectricWire getPileWire() {
        return pile;
    }

    @Override
    public void writeToSync(FriendlyByteBuf buffer) {
        assert level != null;
        // Synchronized only for the goggle information.
        buffer.writeFloat((float) pile.getResistance());
        var pos = worldPosition.above();
        BlockState state;
        while(ModdedBlocks.CARBON_PILE.has(state = level.getBlockState(pos))) {
            if(state.getValue(CarbonPileBlock.TOP)) {
                var be = (CarbonPileBlockEntity) level.getBlockEntity(pos);
                buffer.writeFloat(be.thermal.getTemperature());
                break;
            }
            pos = pos.above();
        }
    }

    @Override
    public void readFromSync(FriendlyByteBuf buffer) {
        assert level != null;
        pile.setResistance(buffer.readFloat());
        var pos = worldPosition.above();
        BlockState state;
        while(ModdedBlocks.CARBON_PILE.has(state = level.getBlockState(pos))) {
            if(state.getValue(CarbonPileBlock.TOP)) {
                var be = (CarbonPileBlockEntity) level.getBlockEntity(pos);
                be.thermal.setTemperature(buffer.readFloat());
                break;
            }
            pos = pos.above();
        }
    }
}
