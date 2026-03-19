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
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedAdvancements;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.CurrentSourceWire;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;
import org.patryk3211.powergrid.utility.sound.SoundScapes;

import java.util.ArrayList;
import java.util.List;

public abstract class TransformerBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    private static final int AVG_SAMPLE_COUNT = 10;

    protected TransformerCoilParameters primaryCoil;
    protected TransformerCoilParameters secondaryCoil;

    protected ElectricWire primaryStray;
    protected ElectricWire mutualInductance;
    protected TransformerCoupling coupling;

    protected CurrentSourceWire couplingI1;
    protected CurrentSourceWire couplingI2;

    private double sinkConductance;
    private double sourceConductance;

    private int avgHead = 0;
    private final double[] avgV1 = new double[AVG_SAMPLE_COUNT];
    private final double[] avgV2 = new double[AVG_SAMPLE_COUNT];

    public float lastCurrent;

    public TransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    public abstract double coreAl();
    public abstract double couplingFactor();

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, ModdedAdvancements.TRANSFORMER);
    }

    @Override
    public void tick() {
        float power = 0;
        lastCurrent = 0;
        if(primaryStray != null && primaryStray.isConverged()) {
            var I1 = primaryStray.current();
            var P1 = I1 * I1 * primaryStray.getResistance();
            power += P1;
            lastCurrent += Math.abs(I1);
        }
        if(mutualInductance != null && mutualInductance.isConverged()) {
            var I3 = mutualInductance.current();
            var P3 = I3 * I3 * mutualInductance.getResistance();
            power += P3;
            lastCurrent += Math.abs(I3);
            if(Math.abs(I3) > 0.001) {
                award(ModdedAdvancements.TRANSFORMER);
            }
        }
        if(thermalBehaviour != null && !level.isClientSide)
            thermalBehaviour.applyTickPower(power);
        super.tick();
    }

    @Override
    public void electricalTick() {
        if(couplingI1 != null && couplingI1.isConverged() && couplingI2.isConverged()) {
            assert couplingI1.getNode2() != null && couplingI2.getNode2() != null;
            double V1 = couplingI1.getNetwork() != null
                    ? couplingI1.potentialDifference()
                    : couplingI1.getCurrent() / couplingI1.conductance();
            double V2 = couplingI2.getNetwork() != null
                    ? couplingI2.potentialDifference()
                    : couplingI2.getCurrent() / couplingI2.conductance();

            // No ratio multiplication since only 1:1 transformers are allowed here for now.
            var I1S = V1 * couplingI1.conductance() - couplingI1.getCurrent();
            var I2S = V2 * couplingI2.conductance() - couplingI2.getCurrent();

            boolean s1 = Math.signum(I1S) != Math.signum(V1), s2 = Math.signum(I2S) != Math.signum(V2);
            if(s1 && !s2) {
                // Sourced current.
                var Is = V1 * couplingI1.conductance() - couplingI1.getCurrent();
                V1 = Is / couplingI2.conductance();
                if(Math.abs(Is) > 1e-6) {
                    couplingI1.setConductance(sourceConductance);
                    couplingI2.setConductance(sinkConductance);
                }
            } else if(s2 && !s1) {
                // Sourced current.
                var Is = V2 * couplingI2.conductance() - couplingI2.getCurrent();
                V2 = Is / couplingI1.conductance();
                if(Math.abs(Is) > 1e-6) {
                    couplingI1.setConductance(sinkConductance);
                    couplingI2.setConductance(sourceConductance);
                }
            } else {
                couplingI1.setConductance(sinkConductance);
                couplingI2.setConductance(sinkConductance);
            }

            avgV1[avgHead] = V1;
            avgV2[avgHead] = V2;

            double V1a = 0, V2a = 0;
            avgHead = (avgHead + 1) % AVG_SAMPLE_COUNT;
            for(int i = 0; i < AVG_SAMPLE_COUNT; ++i) {
                V1a += avgV1[i] * (1.0f / AVG_SAMPLE_COUNT);
                V2a += avgV2[i] * (1.0f / AVG_SAMPLE_COUNT);
            }

            couplingI1.setCurrent(V2a * couplingI1.conductance());
            couplingI2.setCurrent(V1a * couplingI2.conductance());

            setUnsaved();
        }
    }

    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        SoundScapes.play(SoundScapes.AmbienceGroup.HUM, worldPosition, 1.0f, lastCurrent / 20);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
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
            electricBehaviour.rebuildCircuit(false);
            tag.remove("Rebuild");
        }

        if(splittingTransformers()) {
            var avgData = tag.getList("TrAvgDat", CompoundTag.TAG_DOUBLE);
            for(int i = 0; i < AVG_SAMPLE_COUNT; ++i) {
                avgV1[i] = avgData.getDouble(i * 2);
                avgV2[i] = avgData.getDouble(i * 2 + 1);
            }
            avgHead = tag.getInt("TrAvgHead");
        }

        super.read(tag, clientPacket);
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

        if(splittingTransformers()) {
            var avgData = new ListTag();
            for(int i = 0; i < AVG_SAMPLE_COUNT; ++i) {
                avgData.add(DoubleTag.valueOf(avgV1[i]));
                avgData.add(DoubleTag.valueOf(avgV2[i]));
            }
            tag.put("TrAvgDat", avgData);
            tag.putInt("TrAvgHead", avgHead);
        }
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);

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

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        var count = 0;
        if(primaryCoil != null)
            count += primaryCoil.getTurns();
        if(secondaryCoil != null)
            count += secondaryCoil.getTurns();
        var stacks = new ArrayList<ItemStack>();
        while(count > 0) {
            stacks.add(ModdedItems.WIRE.asStack(Math.min(count, 64)));
            count -= 64;
        }
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, stacks);
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
        electricBehaviour.rebuildCircuit(false);
        if(level != null && !level.isClientSide) {
            updateCoilBlockState();
        }
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
        electricBehaviour.rebuildCircuit(false);
        if(level != null && !level.isClientSide) {
            updateCoilBlockState();
        }
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
        electricBehaviour.rebuildCircuit(false);
        if(level != null && !level.isClientSide) {
            updateCoilBlockState();
        }
        notifyUpdate();
    }

    public void removePrimary() {
        primaryCoil.clear();
        electricBehaviour.rebuildCircuit(false);
        if(level != null && !level.isClientSide) {
            updateCoilBlockState();
        }
        notifyUpdate();
    }

    public void updateCoilBlockState() {

    }

    public static float mutualMultiplier() {
        return ModdedConfigs.server().electricity.transformerMutualInductanceMultiplier.getF();
    }

    public static boolean splittingTransformers() {
        return ModdedConfigs.server().electricity.solver.splittingTransformers.get();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(primaryCoil == null) {
            primaryCoil = new TransformerCoilParameters();
            secondaryCoil = new TransformerCoilParameters();
        }
        coupling = null;
        couplingI1 = null;
        couplingI2 = null;

        var coreAl = coreAl();
        var couplingFactor = couplingFactor();
        var primaryTurns = primaryCoil.getTurns();
        var secondaryTurns = secondaryCoil.getTurns();

        builder.setTerminalCount(4);

        double primaryInductance = primaryTurns * primaryTurns * coreAl;
        double secondaryInductance = secondaryTurns * secondaryTurns * coreAl;

        if(primaryCoil.isDefined() && secondaryCoil.isDefined()) {
            boolean flipped = primaryTurns > secondaryTurns;
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
            double mutualInductance = couplingFactor * primaryInductance;

            float primaryStray = (float) (primaryInductance - mutualInductance);
            float secondaryStray = (float) (secondaryInductance - ratio * ratio * mutualInductance);

            var Tnode = builder.addInternalNode();

            var P1 = builder.terminalNode(primaryCoil.getTerminal1());
            var P2 = builder.terminalNode(primaryCoil.getTerminal2());

            this.primaryStray = builder.connect(primaryStray, P1, Tnode);
            this.mutualInductance = builder.connect((float) mutualInductance * mutualMultiplier(), Tnode, P2);
            if(primaryTurns == secondaryTurns && splittingTransformers()) {
                // TODO: Consider allowing more variation in the ratio that splits networks.
                this.couplingI1 = new CurrentSourceWire(P1, P2, 1 / secondaryStray);
                this.couplingI2 = new CurrentSourceWire(builder.terminalNode(secondaryCoil.getTerminal1()),
                        builder.terminalNode(secondaryCoil.getTerminal2()), 1 / secondaryStray);
                sourceConductance = 1 / secondaryStray;
                sinkConductance = 1 / mutualInductance;
                builder.add(couplingI1);
                builder.add(couplingI2);
            } else {
                this.coupling = builder.couple(ratio, secondaryStray, Tnode, P2, builder.terminalNode(secondaryCoil.getTerminal1()), builder.terminalNode(secondaryCoil.getTerminal2()));
            }
        } else if(primaryCoil.isDefined()) {
            this.primaryStray = builder.connect((float) primaryInductance * mutualMultiplier(), builder.terminalNode(primaryCoil.getTerminal1()), builder.terminalNode(primaryCoil.getTerminal2()));
            this.mutualInductance = null;
            this.coupling = null;
        } else if(secondaryCoil.isDefined()) {
            this.primaryStray = builder.connect((float) secondaryInductance * mutualMultiplier(), builder.terminalNode(secondaryCoil.getTerminal1()), builder.terminalNode(secondaryCoil.getTerminal2()));
            this.mutualInductance = null;
            this.coupling = null;
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
        if(primaryStray != null && mutualInductance != null) {
            float primary = (float) primaryStray.getResistance();
            float secondary = coupling != null ? coupling.getResistance() : couplingI1.getResistance();// * 0.01f;
            if(primaryTurns > secondaryTurns) {
                var r = primary;
                primary = secondary;
                secondary = r;
            }
            Lang.builder().translate("gui.transformer.primary_stray")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Unit.RESISTANCE.formatWithPrefixes(primary)
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip, 1);
            Lang.builder().translate("gui.transformer.secondary_stray")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Unit.RESISTANCE.formatWithPrefixes(secondary)
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip, 1);
            Lang.builder().translate("gui.transformer.parallel_resistance")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Unit.RESISTANCE.formatWithPrefixes(mutualInductance.getResistance())
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip, 1);
        } else if(primaryStray != null) {
            Lang.builder().translate("gui.transformer.parallel_resistance")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Unit.RESISTANCE.formatWithPrefixes(primaryStray.getResistance())
                    .style(ChatFormatting.AQUA)
                    .forGoggles(tooltip, 1);
        }
        return true;
    }
}
