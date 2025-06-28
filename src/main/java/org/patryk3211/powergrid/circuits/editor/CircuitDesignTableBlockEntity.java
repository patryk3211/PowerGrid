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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.List;

public class CircuitDesignTableBlockEntity extends SmartBlockEntity implements IMultiScreenHandlerFactory {
    private final CircuitDesignTableInventory inventory = new CircuitDesignTableInventory();

    private String schematicName = null;
    CircuitSchematic schematic = new CircuitSchematic();
    boolean schematicChanged = false;

    private class CircuitDesignTableInventory extends ItemStackHandler {
        public CircuitDesignTableInventory() {
            super(3);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
        }
    }

    public CircuitDesignTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Inventory", inventory.serializeNBT());
        tag.put("Schematic", schematic.serializeNbt());
        if(schematicName != null) {
            tag.putString("Name", schematicName);
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        schematic.deserializeNbt(tag.getCompound("Schematic"));
        if(clientPacket)
            schematicChanged = true;
        if(tag.contains("Name")) {
            schematicName = tag.getString("Name");
        } else {
            schematicName = null;
        }
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player, int menuIndex) {
        return switch(menuIndex) {
            case 0 -> CircuitDesignTableMenu.create(syncId, playerInventory, this);
            case 1 -> CircuitDesignTableEditMenu.create(syncId, playerInventory, this);
            default -> null;
        };
    }

    public void writeToItem() {
        var stack = inventory.getStackInSlot(1);
        if(stack.isEmpty() || world.isClient)
            return;
        stack.decrement(1);
        var result = schematic.toItemStack();
        if(schematicName != null)
            result.setCustomName(Text.literal(schematicName));
        inventory.setStackInSlot(2, result);
        schematic.clear();
        schematicName = null;
        notifyUpdate();
    }

    public void readFromItem() {
        var stack = inventory.getStackInSlot(0);
        if(stack.isEmpty() || world.isClient || !stack.hasNbt())
            return;
        if(stack.hasCustomName()) {
            schematicName = stack.getName().getString();
        } else {
            schematicName = null;
        }
        if(inventory.getStackInSlot(1).isEmpty() && stack.isOf(ModdedItems.CIRCUIT_SCHEMATIC.get())) {
            // Move to save slot
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            inventory.setStackInSlot(1, stack);
        }
        try {
            schematic.deserializeNbt(stack.getNbt().getCompound("Schematic"));
        } catch(RuntimeException e) {
            PowerGrid.LOGGER.error("Failed to load schematic from item: ", e);
        }
        notifyUpdate();
    }

    @Override
    public Text getDisplayName() {
        return Text.of("Circuit Designer");
    }

    @NotNull
    public String getSchematicName() {
        return schematicName != null ? schematicName : "Empty Schematic";
    }

    public void setSchematicName(String name) {
        this.schematicName = name;
        if(!world.isClient)
            notifyUpdate();
    }

    public SlottedStackStorage getInventory() {
        return inventory;
    }

    public CircuitSchematic getSchematic() {
        return schematic;
    }
}
