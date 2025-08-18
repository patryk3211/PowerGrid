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
package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public abstract class TransformerBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation, TransformerVolumeProvider {
    protected TransformerCoilParameters primaryCoil;
    protected TransformerCoilParameters secondaryCoil;

    protected float couplingFactor;
    protected float coreAl;

    protected ElectricWire primaryStray;
    protected ElectricWire secondaryStray;
    protected ElectricWire mutualInductance;

    public float lastCurrent;
    private boolean hasSoundSource;

    public TransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void tick() {
        super.tick();

        float power = 0;
        lastCurrent = 0;
        if(primaryStray != null) {
            var I1 = primaryStray.current();
            var P1 = I1 * I1 * primaryStray.getResistance();
            power += P1;
            lastCurrent += Math.abs(I1);
        }
        if(secondaryStray != null) {
            var I2 = secondaryStray.current();
            var P2 = I2 * I2 * secondaryStray.getResistance();
            power += P2;
            lastCurrent += Math.abs(I2);
        }
        if(mutualInductance != null) {
            var I3 = mutualInductance.current();
            var P3 = I3 * I3 * mutualInductance.getResistance();
            power += P3;
            lastCurrent += Math.abs(I3);
        }
        applyLostPower(power);
        if(level.isClientSide) {
            tickAudio();
        }
    }

    @Override
    public float getVolume() {
        var volume = lastCurrent / 80;
        return Mth.clamp(volume * volume, 0, 0.5f);
    }

    @Environment(EnvType.CLIENT)
    protected void tickAudio() {
        if(!hasSoundSource && getVolume() > 0) {
            Minecraft.getInstance().getSoundManager().play(new TransformerSoundInstance(this));
            hasSoundSource = true;
        } else if(hasSoundSource && getVolume() <= 0) {
            hasSoundSource = false;
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        boolean rebuild = false;
        if(tag.contains("Primary")) {
            var primary = tag.getCompound("Primary");
            rebuild |= primaryCoil.readNbt(primary);
        } else if(primaryCoil != null) {
            rebuild |= primaryCoil.clear();
        }

        if(tag.contains("Secondary")) {
            var secondary = tag.getCompound("Secondary");
            rebuild |= secondaryCoil.readNbt(secondary);
        } else if(primaryCoil != null) {
            rebuild |= secondaryCoil.clear();
        }

        if(rebuild) {
            electricBehaviour.rebuildCircuit();
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if(primaryCoil != null && primaryCoil.isDefined()) {
            var primary = new CompoundTag();
            primaryCoil.writeNbt(primary);
            tag.put("Primary", primary);
        }

        if(secondaryCoil != null && secondaryCoil.isDefined()) {
            var secondary = new CompoundTag();
            secondaryCoil.writeNbt(secondary);
            tag.put("Secondary", secondary);
        }
    }

    public boolean isTerminalUsed(int index) {
        if(primaryCoil.isDefined()) {
            if(primaryCoil.getTerminal1() == index || primaryCoil.getTerminal2() == index)
                return true;
        }
        if(secondaryCoil.isDefined()) {
            if(secondaryCoil.getTerminal1() == index || secondaryCoil.getTerminal2() == index)
                return true;
        }
        return false;
    }

    public void makePrimary(int terminal1, int terminal2, int turns, Item item) {
        primaryCoil.set(terminal1, terminal2, turns, item);
        electricBehaviour.rebuildCircuit();
        notifyUpdate();
    }

    public boolean hasPrimary() {
        return primaryCoil.isDefined();
    }

    public TransformerCoilParameters getPrimary() {
        return primaryCoil;
    }

    public void makeSecondary(int terminal1, int terminal2, int turns, Item item) {
        secondaryCoil.set(terminal1, terminal2, turns, item);
        electricBehaviour.rebuildCircuit();
        notifyUpdate();
    }

    public boolean hasSecondary() {
        return secondaryCoil.isDefined();
    }

    public TransformerCoilParameters getSecondary() {
        return secondaryCoil;
    }

    public void removeSecondary() {
        secondaryCoil.clear();
        electricBehaviour.rebuildCircuit();
        notifyUpdate();
    }

    public void removePrimary() {
        primaryCoil.clear();
        electricBehaviour.rebuildCircuit();
        notifyUpdate();
    }

    public void updateCoilBlockState() {

    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(primaryCoil == null) {
            primaryCoil = new TransformerCoilParameters();
            secondaryCoil = new TransformerCoilParameters();
        }

        if(level != null && !level.isClientSide) {
            updateCoilBlockState();
        }

        coreAl = 1.5f;
        couplingFactor = 0.9999f;
        var primaryTurns = primaryCoil.getTurns();
        var secondaryTurns = secondaryCoil.getTurns();

        builder.setTerminalCount(4);

        float primaryInductance = primaryTurns * primaryTurns * coreAl;
        float secondaryInductance = secondaryTurns * secondaryTurns * coreAl;

        if(primaryCoil.isDefined() && secondaryCoil.isDefined()) {
            boolean flipped = primaryTurns > secondaryInductance;
            var primaryCoil = flipped ? this.secondaryCoil : this.primaryCoil;
            var secondaryCoil = flipped ? this.primaryCoil : this.secondaryCoil;
            if(flipped) {
                var b = primaryTurns;
                primaryTurns = secondaryTurns;
                secondaryTurns = b;

                var b2 = primaryInductance;
                primaryInductance = secondaryInductance;
                secondaryInductance = b2;
            }

            float ratio = (float) secondaryTurns / primaryTurns;
            float mutualInductance = couplingFactor * primaryInductance;

            float primaryStray = primaryInductance - mutualInductance;
            float secondaryStray = secondaryInductance - ratio * ratio * mutualInductance;

            var Tnode = builder.addInternalNode();
            var Pnode = builder.addInternalNode();

            var P1 = builder.terminalNode(primaryCoil.getTerminal1());
            var P2 = builder.terminalNode(primaryCoil.getTerminal2());

            this.primaryStray = builder.connect(primaryStray, P1, Tnode);
            this.secondaryStray = builder.connect(secondaryStray, Tnode, Pnode);
            this.mutualInductance = builder.connect(mutualInductance, Tnode, P2);
            builder.couple(ratio, Pnode, P2, builder.terminalNode(secondaryCoil.getTerminal1()), builder.terminalNode(secondaryCoil.getTerminal2()));
        } else if(primaryCoil.isDefined()) {
            this.primaryStray = builder.connect(primaryInductance, builder.terminalNode(primaryCoil.getTerminal1()), builder.terminalNode(primaryCoil.getTerminal2()));
            this.secondaryStray = null;
            this.mutualInductance = null;
        } else if(secondaryCoil.isDefined()) {
            this.secondaryStray = builder.connect(secondaryInductance, builder.terminalNode(secondaryCoil.getTerminal1()), builder.terminalNode(secondaryCoil.getTerminal2()));
            this.primaryStray = null;
            this.mutualInductance = null;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!isPlayerSneaking)
            return false;

        Lang.builder().translate("gui.transformer.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.transformer.ratio")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var primaryTurns = primaryCoil.getTurns();
        var secondaryTurns = secondaryCoil.getTurns();

        int largestCommonDenominator = 1;
        if(primaryTurns > 0 && secondaryTurns > 0) {
            for (int i = 2; i <= Math.max(primaryTurns, secondaryTurns); ++i) {
                if (primaryTurns % i == 0 && secondaryTurns % i == 0)
                    largestCommonDenominator = i;
            }
        }
        var n1 = Lang.number(primaryTurns / largestCommonDenominator);
        var n2 = Lang.number(secondaryTurns / largestCommonDenominator);
        var ratio = n1.add(Component.nullToEmpty(":")).add(n2);
        ratio.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        return true;
    }
}
