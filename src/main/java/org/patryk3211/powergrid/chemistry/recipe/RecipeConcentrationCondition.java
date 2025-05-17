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
package org.patryk3211.powergrid.chemistry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.mixture.ReagentMixture;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;

public class RecipeConcentrationCondition implements IReactionCondition {
    private final Reagent reagent;
    private final Float min;
    private final Float max;

    public RecipeConcentrationCondition(JsonObject object) {
        var id = object.get("reagent").getAsString();
        reagent = ReagentRegistry.REGISTRY.get(new Identifier(id));

        if(object.has("min")) {
            min = object.get("min").getAsFloat();
        } else {
            min = null;
        }
        if(object.has("max")) {
            max = object.get("max").getAsFloat();
        } else {
            max = null;
        }

        if(min != null && max != null) {
            if(min > max) {
                throw new IllegalArgumentException("Minimum reagent concentration must be smaller than maximum concentration");
            }
        }
        if(min != null && (min < 0 || min > 1)) {
            throw new IllegalArgumentException("Minimum reagent concentration must be in [0; 1] range");
        }
        if(max != null && (max < 0 || max > 1)) {
            throw new IllegalArgumentException("Maximum reagent concentration must be in [0; 1] range");
        }
        if(min == null && max == null) {
            throw new IllegalStateException("Empty concentration condition");
        }
    }

    public RecipeConcentrationCondition(PacketByteBuf buf) {
        reagent = ReagentRegistry.REGISTRY.get(buf.readIdentifier());
        var bits = buf.readByte();
        if((bits & 1) != 0) {
            min = buf.readFloat();
        } else {
            min = null;
        }
        if((bits & 2) != 0) {
            max = buf.readFloat();
        } else {
            max = null;
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        var id = ReagentRegistry.REGISTRY.getId(reagent);
        buf.writeIdentifier(id);
        buf.writeByte((min != null ? 1 : 0) | (max != null ? 2 : 0));
        if(min != null) {
            buf.writeFloat(min);
        }
        if(max != null) {
            buf.writeFloat(max);
        }
    }

    @Override
    public boolean test(ReagentMixture reagentMixture) {
        var concentration = reagentMixture.getConcentration(reagent);
        if(min != null && concentration < min)
            return false;
        if(max != null && concentration > max)
            return false;
        return true;
    }
}
