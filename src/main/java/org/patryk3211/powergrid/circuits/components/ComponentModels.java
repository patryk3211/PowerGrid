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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ComponentModels {
    public static ModelResourceLocation modelId(ResourceLocation componentId) {
        return new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(componentId.getNamespace(), "component/" + componentId.getPath()), "component");
    }

    public static ResourceLocation rawModelId(ResourceLocation componentId) {
        return ResourceLocation.fromNamespaceAndPath(componentId.getNamespace(), "component/" + componentId.getPath());
    }

    public static Set<ModelResourceLocation> collectIds() {
        var ids = new HashSet<ModelResourceLocation>();
        for(var component : ComponentRegistry.entries()) {
            for(var id : component.requestedModels()) {
                ids.add(modelId(id));
            }
        }
        return ids;
    }

    public static Set<ResourceLocation> collectRawIds() {
        var ids = new HashSet<ResourceLocation>();
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
