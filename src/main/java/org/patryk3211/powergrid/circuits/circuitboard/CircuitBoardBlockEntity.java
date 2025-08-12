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

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
        if(level.isClientSide)
            return;
        if(schematic == null) {
            level.destroyBlock(worldPosition, false);
            return;
        }
        this.schematic = new CircuitSchematic(schematic);
        bakeCircuit();
        notifyUpdate();
    }

    public void setAdditionalData(CompoundTag tag) {
        if(baked == null)
            return;
        baked.read(tag);
        notifyUpdate();
    }

    private void bakeCircuit() {
        componentCache.clear();
        baked = BakedCircuit.from(schematic, () -> level, worldPosition);
        for(var placed : schematic.components()) {
            placed.withWorld(this::getLevel, worldPosition);
        }
        electricBehaviour.rebuildCircuit();
        if(level != null && level.isClientSide)
            Component.modelChanged(worldPosition);
    }

    @Override
    public void tick() {
        super.tick();
        if(baked != null) {
            baked.tick();
            if(!level.isClientSide)
                setChanged();
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        tag.put("Schematic", schematic.serializeNbt());
        baked.write(tag);
        super.write(tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        if(!tag.contains("Schematic")) {
            level.destroyBlock(worldPosition, false);
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
    public boolean addToGoggleTooltip(List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        if(baked == null || !baked.isDamaged())
            return false;

        Lang.translate("gui.circuit_board.damage_header")
                .forGoggles(tooltip);
        Lang.translate("gui.circuit_board.damage_body")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        return true;
    }
}
