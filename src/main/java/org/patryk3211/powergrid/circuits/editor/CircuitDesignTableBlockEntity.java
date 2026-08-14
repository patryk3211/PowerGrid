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
package org.patryk3211.powergrid.circuits.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.ISchematicHolder;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class CircuitDesignTableBlockEntity extends ElectricBlockEntity implements IMultiScreenHandlerFactory, ISchematicHolder {
    private final Container inventory = new SimpleContainer(3);

    private ElectricWire wire;
    CircuitSchematic schematic = new CircuitSchematic();
    boolean schematicChanged = false;

    private boolean wasPowered;

    public CircuitDesignTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        wasPowered = state.getValue(CircuitDesignTableBlock.POWERED);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        applyPower(wire);
        if(wasPowered && !isPowered()) {
            level.setBlock(worldPosition,
                    getBlockState().setValue(CircuitDesignTableBlock.POWERED, false),
                    CircuitDesignTableBlock.UPDATE_ALL_IMMEDIATE);
            wasPowered = false;
        } else if(!wasPowered && isPowered()) {
            level.setBlock(worldPosition,
                    getBlockState().setValue(CircuitDesignTableBlock.POWERED, true),
                    CircuitDesignTableBlock.UPDATE_ALL_IMMEDIATE);
            wasPowered = true;
        }
    }

    public boolean isPowered() {
        return wire.power() >= 30;
    }

    public float power() {
        return (float) wire.power();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Schematic", schematic.serializeNbt());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        schematic.deserializeNbt(tag.getCompound("Schematic"));
        if(clientPacket)
            schematicChanged = true;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player, int menuIndex) {
        return switch(menuIndex) {
            case 0 -> CircuitDesignTableMenu.create(syncId, playerInventory, this);
            case 1 -> CircuitDesignTableEditMenu.create(syncId, playerInventory, this);
            case 2 -> CircuitDesignTableLoadMenu.create(syncId, playerInventory, this);
            default -> null;
        };
    }

    public void writeToItem(boolean isCreative) {
        if(!isCreative) {
            var stack = inventory.getItem(1);
            if (stack.isEmpty() || level.isClientSide)
                return;
            stack.shrink(1);
        }
        var result = schematic.toItemStack();
        inventory.setItem(2, result);
        schematic.clear();
        notifyUpdate();
    }

    public void readFromItem() {
        var stack = inventory.getItem(0);
        if(stack.isEmpty() || level.isClientSide || !stack.hasTag())
            return;
        if(inventory.getItem(1).isEmpty() && stack.is(ModdedItems.CIRCUIT_SCHEMATIC.get())) {
            // Move to save slot
            inventory.setItem(0, ItemStack.EMPTY);
            inventory.setItem(1, stack);
        }
        try {
            schematic.deserializeNbt(stack.getTag().getCompound("Schematic"));
        } catch(RuntimeException e) {
            PowerGrid.LOGGER.error("Failed to load schematic from item: ", e);
        }
        notifyUpdate();
    }

    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("Circuit Designer");
    }

    @Override
    @NotNull
    public String getSchematicName() {
        var name = schematic.getName();
        return name != null ? name : Component.translatable("item.powergrid.circuit_schematic.empty").getString();
    }

    @Override
    public void setSchematicName(String name) {
        schematic.setName(name);
        if(!level.isClientSide)
            notifyUpdate();
    }

    public Container getInventory() {
        return inventory;
    }

    @Override
    public CircuitSchematic getSchematic() {
        return schematic;
    }

    @Override
    public void setSchematic(CircuitSchematic schematic) {
        this.schematic = schematic;
        if(!level.isClientSide)
            notifyUpdate();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }
}
