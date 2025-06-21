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
package org.patryk3211.powergrid.circuits.schematic;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.patryk3211.powergrid.collections.ModdedItems;

public class CircuitSchematic {
    private final CircuitLayer front = new CircuitLayer();
    private final CircuitLayer back = new CircuitLayer();

    // This layer is generated from components.
    private final CircuitLayer pads = new CircuitLayer();

    public CircuitSchematic() {

    }

    public CircuitSchematic(CircuitSchematic schematic) {
        front.from(schematic.front);
        back.from(schematic.back);
    }

    public NbtCompound serializeNbt() {
        var tag = new NbtCompound();
        tag.put("Front", front.serializeNbt());
        tag.put("Back", back.serializeNbt());
        return tag;
    }

    public void deserializeNbt(NbtCompound tag) {
        front.deserialize(tag.getLongArray("Front"));
        back.deserialize(tag.getLongArray("Back"));
    }

    public CircuitLayer front() {
        return front;
    }

    public CircuitLayer back() {
        return back;
    }

    public CircuitLayer pads() {
        return pads;
    }

    public ItemStack toItemStack() {
        var stack = ModdedItems.CIRCUIT_SCHEMATIC.asStack();
        stack.setNbt(serializeNbt());
        return stack;
    }
}
