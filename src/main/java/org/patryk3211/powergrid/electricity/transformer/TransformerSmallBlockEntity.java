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

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class TransformerSmallBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private TransformerCoilParameters primaryCoil;
    private TransformerCoilParameters secondaryCoil;

    private float couplingFactor;
    private float coreAl;

    private ElectricWire primaryStray;
    private ElectricWire secondaryStray;
    private ElectricWire mutualInductance;

    public TransformerSmallBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 2.0f, 0.5f);
    }

    @Override
    public void tick() {
        super.tick();

        float power = 0;
        if(primaryStray != null) {
            var I1 = primaryStray.current();
            var P1 = I1 * I1 * primaryStray.getResistance();
            power += P1;
        }
        if(secondaryStray != null) {
            var I2 = secondaryStray.current();
            var P2 = I2 * I2 * secondaryStray.getResistance();
            power += P2;
        }
        if(mutualInductance != null) {
            var I3 = mutualInductance.current();
            var P3 = I3 * I3 * mutualInductance.getResistance();
            power += P3;
        }
        applyLostPower(power);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        boolean rebuild = false;
        if(tag.contains("Primary")) {
            var primary = tag.getCompound("Primary");
            rebuild |= primaryCoil.readNbt(primary);
        } else {
            rebuild |= primaryCoil.clear();
        }

        if(tag.contains("Secondary")) {
            var secondary = tag.getCompound("Secondary");
            rebuild |= secondaryCoil.readNbt(secondary);
        } else {
            rebuild |= secondaryCoil.clear();
        }

        if(rebuild) {
            electricBehaviour.rebuildCircuit();
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if(primaryCoil.isDefined()) {
            var primary = new NbtCompound();
            primaryCoil.writeNbt(primary);
            tag.put("Primary", primary);
        }

        if(secondaryCoil.isDefined()) {
            var secondary = new NbtCompound();
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

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(primaryCoil == null) {
            primaryCoil = new TransformerCoilParameters();
            secondaryCoil = new TransformerCoilParameters();
        }

        if(world != null && !world.isClient) {
            int coilCount = secondaryCoil.isDefined() ? 2 : primaryCoil.isDefined() ? 1 : 0;
            world.setBlockState(pos, getCachedState().with(TransformerSmallBlock.COILS, coilCount));
        }

        coreAl = 1.5f;
        couplingFactor = 0.9999f;
        var primaryTurns = primaryCoil.getTurns();
        var secondaryTurns = secondaryCoil.getTurns();

        builder.setTerminalCount(4);

        float primaryInductance = primaryTurns * primaryTurns * coreAl;
        float secondaryInductance = secondaryTurns * secondaryTurns * coreAl;

        if(primaryCoil.isDefined() && secondaryCoil.isDefined()) {
            float ratio = (float) secondaryTurns / primaryTurns;
            float mutualInductance = couplingFactor * primaryInductance;

            float primaryStray = primaryInductance -mutualInductance;
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
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        if(!isPlayerSneaking)
            return false;

        Lang.builder().translate("gui.transformer.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.transformer.ratio")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var primaryTurns = primaryCoil.getTurns();
        var secondaryTurns = secondaryCoil.getTurns();

        int largestCommonDenominator = 1;
        for(int i = 2; i <= Math.max(primaryTurns, secondaryTurns); ++i) {
            if(primaryTurns % i == 0 && secondaryTurns % i == 0)
                largestCommonDenominator = i;
        }
        var n1 = Lang.number(primaryTurns / largestCommonDenominator);
        var n2 = Lang.number(secondaryTurns / largestCommonDenominator);
        var ratio = n1.add(Text.of(":")).add(n2);
        ratio.style(Formatting.AQUA).forGoggles(tooltip, 1);
        return true;
    }
}
