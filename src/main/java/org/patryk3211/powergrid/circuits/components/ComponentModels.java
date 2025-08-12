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
package org.patryk3211.powergrid.circuits.components;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.*;

@Environment(EnvType.CLIENT)
public class ComponentModels {
    public static ModelIdentifier modelId(Identifier componentId) {
        return new ModelIdentifier(new Identifier(componentId.getNamespace(), "component/" + componentId.getPath()), "component");
    }

    public static Identifier rawModelId(Identifier componentId) {
        return new Identifier(componentId.getNamespace(), "component/" + componentId.getPath());
    }

    public static Set<ModelIdentifier> collectIds() {
        var ids = new HashSet<ModelIdentifier>();
        for(var component : ComponentRegistry.entries()) {
            for(var id : component.requestedModels()) {
                ids.add(modelId(id));
            }
        }
        return ids;
    }

    public static Set<Identifier> collectRawIds() {
        var ids = new HashSet<Identifier>();
        for(var component : ComponentRegistry.entries()) {
            for(var id : component.requestedModels()) {
                ids.add(rawModelId(id));
            }
        }
        return ids;
    }

    @ExpectPlatform
    public static BakedModel getModel(PlacedComponent placed) {
        throw new AssertionError();
    }
}
