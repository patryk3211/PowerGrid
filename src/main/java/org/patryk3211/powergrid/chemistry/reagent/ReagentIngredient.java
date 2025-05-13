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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.function.Predicate;

public class ReagentIngredient implements Predicate<ReagentStack> {
    public static final ReagentIngredient EMPTY = new ReagentIngredient();

    private Reagent reagent;
    private int amountRequired;

    public ReagentIngredient() {

    }

    public Reagent getReagent() {
        return reagent;
    }

    public int getRequiredAmount() {
        return amountRequired;
    }

    public static ReagentIngredient fromReagent(Reagent reagent, int amount) {
        var ingredient = new ReagentIngredient();
        ingredient.reagent = reagent;
        ingredient.amountRequired = amount;
        return ingredient;
    }

    public static ReagentIngredient read(JsonObject json) {
        var reagentId = json.get("reagent").getAsString();
        var amount = json.get("amount").getAsInt();
        var reagent = ReagentRegistry.REGISTRY.get(new Identifier(reagentId));
        return fromReagent(reagent, amount);
    }

    public static ReagentIngredient read(PacketByteBuf buf) {
        var rId = buf.readIdentifier();
        var amount = buf.readInt();
        return fromReagent(ReagentRegistry.REGISTRY.get(rId), amount);
    }

    public void write(PacketByteBuf buf) {
        var id = ReagentRegistry.REGISTRY.getId(reagent);
        buf.writeIdentifier(id);
        buf.writeInt(amountRequired);
    }

    @Override
    public boolean test(ReagentStack reagentStack) {
        return reagentStack.isOf(reagent) && reagentStack.getAmount() >= amountRequired;
    }

    public boolean isOf(Reagent reagent) {
        return this.reagent == reagent;
    }
}
