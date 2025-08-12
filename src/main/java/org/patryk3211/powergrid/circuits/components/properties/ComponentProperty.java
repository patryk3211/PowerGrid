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
package org.patryk3211.powergrid.circuits.components.properties;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public abstract class ComponentProperty<T> {
    private final String namespace;
    private final String name;
    private boolean hidden;

    public ComponentProperty(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
        this.hidden = false;
    }

    public ComponentProperty<T> hidden() {
        this.hidden = true;
        return this;
    }

    public ResourceLocation id() {
        return new ResourceLocation(namespace, name);
    }

    public String translationKey() {
        return namespace + ".component.property." + name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public abstract T parse(String value) throws RuntimeException;
    public abstract String toString(T value);

    public abstract T read(@Nullable Tag element);
    public abstract Tag write(T value);

    public abstract T defaultValue();

    @Override
    public String toString() {
        return super.toString() + "(" + id().toString() + ")";
    }
}
