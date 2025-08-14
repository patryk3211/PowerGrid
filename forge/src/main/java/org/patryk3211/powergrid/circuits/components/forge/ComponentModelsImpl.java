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
package org.patryk3211.powergrid.circuits.components.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import static org.patryk3211.powergrid.circuits.components.ComponentModels.rawModelId;

public class ComponentModelsImpl {
    public static BakedModel getModel(PlacedComponent placed) {
        var manager = Minecraft.getInstance().getModelManager();
        return manager.getModel(rawModelId(placed.component.getModelId(placed)));
    }
}
