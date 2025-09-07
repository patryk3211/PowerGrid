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
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy property used for documenting certain properties of components.
 */
public class ConstantProperty extends ComponentProperty<String> {
    private final Component value;

    public ConstantProperty(String namespace, String name, Component value) {
        super(namespace, name);
        this.value = value;
    }

    @Override
    public String parse(String value) throws RuntimeException {
        return this.value.getString();
    }

    @Override
    public String toString(String value) {
        return this.value.getString();
    }

    @Override
    public String read(@Nullable Tag element) {
        return this.value.getString();
    }

    @Override
    public Tag write(String value) {
        return null;
    }

    @Override
    public String defaultValue() {
        return this.value.getString();
    }
}
