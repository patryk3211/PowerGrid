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

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class TransformerCoilParameters {
    private int turns;
    private int terminal1;
    private int terminal2;
    private Item item;

    public void writeNbt(NbtCompound tag) {
        tag.putInt("Turns", turns);
        tag.putInt("Terminal1", terminal1);
        tag.putInt("Terminal2", terminal2);

        var identifier = Registries.ITEM.getId(item);
        tag.putString("Item", identifier.toString());
    }

    public boolean readNbt(NbtCompound tag) {
        var oldTurns = turns;
        var oldT1 = terminal1;
        var oldT2 = terminal2;

        turns = tag.getInt("Turns");
        terminal1 = tag.getInt("Terminal1");
        terminal2 = tag.getInt("Terminal2");

        var identifier = new Identifier(tag.getString("Item"));
        item = Registries.ITEM.get(identifier);

        return turns != oldTurns || terminal1 != oldT1 || terminal2 != oldT2;
    }

    public boolean clear() {
        if (item != null) {
            turns = 0;
            terminal1 = -1;
            terminal2 = -1;
            item = null;
            return true;
        }
        return false;
    }

    public boolean isDefined() {
        return item != null;
    }

    public int getTurns() {
        return turns;
    }

    public int getTerminal1() {
        return terminal1;
    }

    public int getTerminal2() {
        return terminal2;
    }

    public void set(int terminal1, int terminal2, int turns, Item item) {
        this.terminal1 = terminal1;
        this.terminal2 = terminal2;
        this.turns = turns;
        this.item = item;
    }

    public Item getItem() {
        return item;
    }
}
