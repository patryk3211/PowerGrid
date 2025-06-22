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

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;

public class PlacedComponent {
    public final Component component;
    public final int x;
    public final int y;

    public PlacedComponent(NbtCompound tag) {
        component = ComponentRegistry.REGISTRY.get(new Identifier(tag.getString("Id")));
        x = tag.getInt("X");
        y = tag.getInt("Y");
    }

    public PlacedComponent(Component component, int x, int y) {
        this.component = component;
        this.x = x;
        this.y = y;
    }

    public NbtCompound serializeNbt() {
        var tag = new NbtCompound();

        var id = ComponentRegistry.REGISTRY.getId(component);
        tag.putString("Id", id.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);

        return tag;
    }


}
