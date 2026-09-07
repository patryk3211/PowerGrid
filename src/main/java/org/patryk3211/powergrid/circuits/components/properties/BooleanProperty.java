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

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class BooleanProperty extends ComponentProperty<Boolean> {
    public BooleanProperty(String namespace, String name) {
        super(namespace, name);
    }

    @Override
    public Boolean parse(String value) {
        return Boolean.valueOf(value);
    }

    @Override
    public String toString(Boolean value) {
        return Boolean.toString(value);
    }

    @Override
    public Boolean read(HolderLookup.Provider registries, @Nullable Tag element) {
        if(element == null)
            return false;
        if(element.getId() != Tag.TAG_BYTE)
            return false;
        return ((ByteTag) element).getAsByte() != 0;
    }

    @Override
    public Tag write(HolderLookup.Provider registries, Boolean value) {
        return ByteTag.valueOf(value);
    }

    @Override
    public Boolean defaultValue() {
        return false;
    }
}
