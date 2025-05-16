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
package org.patryk3211.powergrid.chemistry.reagent;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

public class ReagentStack {
    public static final ReagentStack EMPTY = new ReagentStack(Reagents.EMPTY, 0);

    private Reagent reagent;
    // Amount is an integer to avoid rounding errors and imprecise reagent usage.
    // This also dictates the minimum amount of reagent.
    // TODO: This might have to become a long
    private int amount;
    private float temperature;

    public ReagentStack(Reagent reagent, int amount) {
        this.reagent = reagent;
        this.amount = amount;
        this.temperature = 22.0f;
    }

    public ReagentStack(Reagent reagent, int amount, float temperature) {
        this.reagent = reagent;
        this.amount = amount;
        this.temperature = temperature;
    }

    public ReagentStack(NbtCompound tag) {
        var id = tag.getString("Id");
        this.reagent = ReagentRegistry.REGISTRY.get(new Identifier(id));
        this.amount = tag.getInt("Amount");
        this.temperature = tag.getFloat("Temperature");
    }

    public static ReagentStack fromNbt(NbtCompound tag) {
        try {
            return new ReagentStack(tag);
        } catch(RuntimeException e) {
            PowerGrid.LOGGER.debug("Tried to load invalid reagent: {}", tag, e);
            return EMPTY;
        }
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    public Reagent getReagent() {
        return isEmpty() ? Reagents.EMPTY : reagent;
    }

    /**
     * Get the amount of reagent in this stack. The unit is one one-thousandth of a mole.
     * @return Moles of the reagent * 1000
     */
    public int getAmount() {
        return amount;
    }

    public float getTemperature() {
        return temperature;
    }

    public void increment(int amount) {
        this.amount += amount;
        if(this.amount < 0)
            this.amount = 0;
    }

    public void decrement(int amount) {
        increment(-amount);
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public ReagentStack copy() {
        return new ReagentStack(reagent, amount, temperature);
    }

    public ReagentStack copyWithAmount(int amount) {
        return new ReagentStack(reagent, amount, temperature);
    }

    public ReagentState getState() {
        return reagent.properties.getState(temperature);
    }

    public NbtCompound serialize() {
        var tag = new NbtCompound();
        var id = ReagentRegistry.REGISTRY.getId(reagent);
        tag.putString("Id", id.toString());
        tag.putInt("Amount", amount);
        tag.putFloat("Temperature", temperature);
        return tag;
    }

    public static ReagentStack read(JsonObject json) {
        var reagentId = json.get("reagent").getAsString();
        var amount = json.get("amount").getAsInt();
        var reagent = ReagentRegistry.REGISTRY.get(new Identifier(reagentId));
        return new ReagentStack(reagent, amount);
    }

    public static ReagentStack read(PacketByteBuf buf) {
        var rId = buf.readIdentifier();
        var amount = buf.readInt();
        return new ReagentStack(ReagentRegistry.REGISTRY.get(rId), amount);
    }

    public void write(PacketByteBuf buf) {
        var id = ReagentRegistry.REGISTRY.getId(reagent);
        buf.writeIdentifier(id);
        buf.writeInt(amount);
    }

    public boolean isOf(Reagent reagent) {
        return this.reagent == reagent;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ReagentStack stack) {
            return stack.reagent == reagent && stack.amount == amount;
        }
        return false;
    }

    @Override
    public String toString() {
        return amount + " " + reagent + "(T=" + temperature + ")";
    }
}
