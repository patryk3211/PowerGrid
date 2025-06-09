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
package org.patryk3211.powergrid.chemistry.vat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class ChemicalVatUpgrade extends Item {
    public ChemicalVatUpgrade(Settings settings) {
        super(settings);
    }

    public abstract void applyUpgrade(ChemicalVatBlockEntity vat, ItemStack stack);
    public abstract void removeUpgrade(ChemicalVatBlockEntity vat, ItemStack stack);
}
