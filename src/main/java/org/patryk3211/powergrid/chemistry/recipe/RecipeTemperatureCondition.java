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
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentMixture;

public class RecipeTemperatureCondition implements IReactionCondition {
    private final float min;
    private final Float max;

    public RecipeTemperatureCondition() {
        min = 0;
        max = null;
    }

    public RecipeTemperatureCondition(JsonObject json) {
        if(json.has("min")) {
            min = json.get("min").getAsFloat();
        } else {
            // TODO: Reactions should always have a min temperature constraint
            //  to prevent endothermic reactions from reaching absolute zero but
            //  I'm not sure if a fixed value here is a good solution.
            min = 0.0f;
        }

        if(json.has("max")) {
            max = json.get("max").getAsFloat();
            if(max < min) {
                PowerGrid.LOGGER.error("Invalid reaction temperature condition (min > max)");
            }
        } else {
            max = null;
        }
    }

    public RecipeTemperatureCondition(PacketByteBuf buf) {
        var hasMax = buf.readByte() != 0;
        min = buf.readFloat();
        if(hasMax) {
            max = buf.readFloat();
        } else {
            max = null;
        }
    }

    @Override
    public boolean test(ReagentMixture mixture) {
        var temperature = mixture.getTemperature();
        if(temperature < min)
            return false;
        if(max != null) {
            return temperature < max;
        } else {
            return true;
        }
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeByte(max == null ? 0 : 1);
        buf.writeFloat(min);
        if(max != null)
            buf.writeFloat(max);
    }
}
