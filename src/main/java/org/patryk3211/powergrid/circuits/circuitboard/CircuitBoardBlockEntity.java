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
package org.patryk3211.powergrid.circuits.circuitboard;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.utility.Lang;

import java.util.*;

public class CircuitBoardBlockEntity extends ElectricBlockEntity implements IElectric, IHaveGoggleInformation {
    private CircuitSchematic schematic = new CircuitSchematic();
    private BakedCircuit baked;
    private final Map<Class<?>, Collection<PlacedComponent>> componentCache = new HashMap<>();

    public CircuitBoardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void withSchematic(CircuitSchematic schematic) {
        if(world.isClient)
            return;
        if(schematic == null) {
            world.breakBlock(pos, false);
            return;
        }
        this.schematic = new CircuitSchematic(schematic);
        bakeCircuit();
        notifyUpdate();
    }

    public void setAdditionalData(NbtCompound tag) {
        if(baked == null)
            return;
        baked.read(tag);
        notifyUpdate();
    }

    private void bakeCircuit() {
        componentCache.clear();
        baked = BakedCircuit.from(schematic, () -> world, pos);
        for(var placed : schematic.components()) {
            placed.withWorld(this::getWorld, pos);
        }
        electricBehaviour.rebuildCircuit();
        if(world != null && world.isClient)
            Component.modelChanged(pos);
    }

    @Override
    public void tick() {
        super.tick();
        if(baked != null) {
            baked.tick();
            if(!world.isClient)
                markDirty();
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        tag.put("Schematic", schematic.serializeNbt());
        baked.write(tag);
        super.write(tag, clientPacket);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        if(!tag.contains("Schematic")) {
            world.breakBlock(pos, false);
            return;
        }
        super.read(tag, clientPacket);
        if(!clientPacket || tag.getBoolean("Rebuild") || baked == null) {
            schematic.deserializeNbt(tag.getCompound("Schematic"));
            bakeCircuit();
        }
        baked.read(tag);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(baked != null)
            builder.setTo(baked);
    }

    @Override
    public int terminalCount() {
        return baked == null ? 0 : baked.externalNodes.size();
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        // TODO: Apply block state
        return baked == null ? null : baked.terminals.get(index);
    }

    public CircuitSchematic getSchematic() {
        return schematic;
    }

    public <T> Collection<PlacedComponent> getComponents(Class<T> ofClass) {
        if(componentCache.containsKey(ofClass))
            return componentCache.get(ofClass);
        var components = new ArrayList<PlacedComponent>();
        for(var placed : schematic.components()) {
            if(ofClass.isInstance(placed.component))
                components.add(placed);
        }
        componentCache.put(ofClass, components);
        return components;
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        if(baked == null || !baked.isDamaged())
            return false;

        Lang.translate("gui.circuit_board.damage_header")
                .forGoggles(tooltip);
        Lang.translate("gui.circuit_board.damage_body")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);
        return true;
    }
}
