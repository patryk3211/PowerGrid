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
package org.patryk3211.powergrid.electricity.electrode;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.electrolysis.ElectrolysisGetter;
import org.patryk3211.powergrid.chemistry.electrolysis.ElectrolysisRecipe;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.utility.Directions;

import java.util.*;

public class VatElectrodeBlockEntity extends ElectricBlockEntity {
    private FloatingNode tieNode;
    private List<SwitchedWire> wires;
    private final Map<VatElectrodeBlockEntity, ElectricWire> connectedElectrodes = new HashMap<>();
    private float resistance;

    private int bubbleRate = 0;

    public VatElectrodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void initialize() {
        electricBehaviour.rebuildCircuit();
        super.initialize();
    }

    private List<VatElectrodeBlockEntity> findNegativeReceivers() {
        var vat = getVat();
        assert vat != null;

        var negativeReceivers = new ArrayList<VatElectrodeBlockEntity>();
        for(var wire : wires) {
            if(calculateSurfacePotential(wire) < 0)
                negativeReceivers.add(this);
        }
        for(var connections : connectedElectrodes.entrySet()) {
            var wire = connections.getValue();
            var otherVat = connections.getKey().getVat();
            if(otherVat == null)
                continue;
            var current = wire.current();
            if(wire.node2 == tieNode)
                current = -current;
            if(current < 0)
                negativeReceivers.add(connections.getKey());
        }
        return negativeReceivers;
    }

    private void applyRecipes() {
        var vat = getVat();
        if(vat == null)
            return;
        var mixture = vat.getReagentMixture();
        var recipes = ElectrolysisGetter.getPossibleRecipes(world.getRecipeManager(), mixture);
        if(recipes.isEmpty())
            return;

        // Apply recipes. Only positive electrodes check for potentials and apply recipes,
        // negative electrodes only receive the results, just to simplify the code a bit.
        List<VatElectrodeBlockEntity> negativeReceivers = null;
        bubbleRate = 0;

        for(var wire : wires) {
            var potential = calculateSurfacePotential(wire);
            if(potential <= 0) {
                // Negative electrode
                continue;
            }
            if(negativeReceivers == null) {
                negativeReceivers = findNegativeReceivers();
                if(negativeReceivers.isEmpty())
                    return;
            }
            // Filter for valid recipes
            var valid = new ArrayList<ElectrolysisRecipe>();
            for(var recipe : recipes) {
                if(potential >= recipe.getMinimumPotential())
                    valid.add(recipe);
            }
            // Apply random recipe
            if(valid.isEmpty())
                continue;
            var recipe = valid.size() == 1 ? valid.get(0) : valid.get(world.random.nextInt(valid.size()));
            var negativeReceiver = negativeReceivers.get(world.random.nextInt(negativeReceivers.size()));
            var rate = mixture.applyReaction(recipe, potential, negativeReceiver.getVat().getReagentMixture());
            negativeReceiver.bubbleRate += rate;
            bubbleRate += rate;
        }
    }

    @Override
    public void tick() {
        super.tick();

        applyRecipes();

        var vat = getVat();
        if(vat == null)
            return;

        float power = 0;
        for(var wire : wires) {
            power += wire.power();
        }
        for(var conn : connectedElectrodes.values()) {
            power += conn.power() * 0.5f;
        }

        // Apply tick power from resistive losses
        vat.getReagentMixture().addEnergy(power * 0.05f);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if(updateConductance()) {
            // Is conducting.
            scanForOtherVats();
        } else {
            disconnectAll();
        }
    }

    public static float calculateSurfacePotential(ElectricWire wire) {
        var current = wire.current();
        var resistance = (float) wire.getResistance() / 16f;
        return current * resistance;
    }

    public static float calculateConductance(ChemicalVatBlockEntity vat) {
        var reagents = new HashSet<Reagent>();
        int totalAmount = 0;
        var mixture = vat.getReagentMixture();
        for(var reagent : mixture.getReagents()) {
            if(mixture.getState(reagent) != ReagentState.LIQUID)
                continue;
            totalAmount += mixture.getAmount(reagent);
            reagents.add(reagent);
        }
        float conductance = 0;
        for(var reagent : reagents) {
            float concentration = (float) mixture.getAmount(reagent) / totalAmount;
            conductance += reagent.getLiquidConductance() * concentration;
        }
        return vat.getFluidLevel() * conductance;
    }

    public boolean updateConductance() {
        var vat = getVat();
        if(vat == null)
            return false;
        float conductance = calculateConductance(vat);
        // Half resistance since each wire goes half way.
        resistance = conductance > 0 ? 0.5f / conductance : 1;

        for(var wire : wires) {
            wire.setState(false);
            wire.setResistance(resistance);
            wire.setState(conductance > 0);
        }
        return conductance > 0;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        disconnectAll();
        builder.setTerminalCount(4);
        if(world == null)
            return;

        var vat = getVat();
        if(vat == null) {
            world.breakBlock(pos, false);
            return;
        }
        tieNode = builder.addInternalNode();
        float conductance = calculateConductance(getVat());
        // Half resistance since each wire goes half way.
        resistance = conductance > 0 ? 0.5f / conductance : 1;

        if(wires == null)
            wires = new ArrayList<>();
        wires.clear();
        var state = getCachedState();
        for(var dir : Directions.HORIZONTAL) {
            var present = state.get(Directions.property(dir));
            int index = VatElectrodeBlock.getTerminalIndex(dir);
            builder.setExternalNode(index, present);
            if(!present)
                continue;

            var wire = builder.connectSwitch(resistance, tieNode, builder.terminalNode(index), false);
            wires.add(wire);
            if(conductance > 0)
                wire.setState(true);
        }
    }

    @Nullable
    public ChemicalVatBlockEntity getVat() {
        if(world == null)
            return null;
        return world.getBlockEntity(pos.down(), ModdedBlockEntities.CHEMICAL_VAT.get()).orElse(null);
    }

    private void disconnectAll() {
        if(connectedElectrodes == null)
            return;
        for(var connection : connectedElectrodes.entrySet()) {
            var be = connection.getKey();
            be.connectedElectrodes.remove(this);
            connection.getValue().remove();
        }
        connectedElectrodes.clear();
    }

    private void scanForOtherVats() {
        var map = new HashMap<BlockPos, ScanNode>();
        var queue = new PriorityQueue<ScanNode>((a, b) -> Float.compare(a.resistance, b.resistance));
        var initialNode = new ScanNode(pos.down(), resistance * 2, resistance * 2);
        map.put(pos.down(), initialNode);
        queue.add(initialNode);

        // TODO: Block connection if it goes through another vat with electrodes.

        var electrodeVats = new ArrayList<ScanNode>();
        while(!queue.isEmpty()) {
            var node = queue.poll();
            for(var dir : Direction.values()) {
                var neighborPos = node.pos.offset(dir);
                var be = world.getBlockEntity(neighborPos);
                if(!(be instanceof ChemicalVatBlockEntity vat))
                    continue;
                if(vat.getFluidLevel() == 0)
                    continue;

                var neighborNode = map.get(neighborPos);
                if(neighborNode != null) {
                    if(neighborNode.resistance == 0)
                        continue;
                    float newResistance = node.totalResistance + neighborNode.resistance;
                    if(neighborNode.totalResistance < newResistance)
                        continue;
                    neighborNode.totalResistance = newResistance;
                } else {
                    var conductance = calculateConductance(vat);
                    if(conductance == 0) {
                        neighborNode = new ScanNode(neighborPos, 0, 0);
                        map.put(neighborPos, neighborNode);
                        continue;
                    }
                    float resistance = 1 / conductance;
                    neighborNode = new ScanNode(neighborPos, resistance, node.totalResistance + resistance);
                    map.put(neighborPos, neighborNode);
                }
                if(world.getBlockEntity(neighborPos.up()) instanceof VatElectrodeBlockEntity) {
                    // Electrodes above vat
                    electrodeVats.add(neighborNode);
                }
                queue.add(neighborNode);
            }
        }

        var electrodes = new HashSet<VatElectrodeBlockEntity>();
        for(var vat : electrodeVats) {
            var electrode = (VatElectrodeBlockEntity) world.getBlockEntity(vat.pos.up());
            assert electrode != null;
            electrodes.add(electrode);

            var wire = connectedElectrodes.get(electrode);
            float connectionResistance = vat.totalResistance - resistance * 0.5f - vat.resistance * 0.5f;
            if(wire != null) {
                // Existing connection
                wire.setResistance(connectionResistance);
            } else {
                // New wire
                if(electrode.tieNode == null)
                    continue;
                wire = GlobalElectricNetworks.makeConnection(electricBehaviour, tieNode, electrode.electricBehaviour, electrode.tieNode, connectionResistance);
                connectedElectrodes.put(electrode, wire);
                electrode.connectedElectrodes.put(this, wire);
            }
        }
        var iter = connectedElectrodes.entrySet().iterator();
        while(iter.hasNext()) {
            var connection = iter.next();
            if(!electrodes.contains(connection.getKey())) {
                connection.getKey().connectedElectrodes.remove(this);
                connection.getValue().remove();
                iter.remove();
            }
        }
    }

    private static class ScanNode {
        public final BlockPos pos;
        public float resistance;
        public float totalResistance;
        public ScanNode backtrace;

        public ScanNode(BlockPos pos, float resistance, float totalResistance) {
            this.pos = pos;
            this.resistance = resistance;
            this.totalResistance = totalResistance;
            this.backtrace = null;
        }
    }
}
