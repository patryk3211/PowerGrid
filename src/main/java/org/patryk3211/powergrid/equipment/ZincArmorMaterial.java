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
package org.patryk3211.powergrid.equipment;

import com.simibubi.create.AllItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.patryk3211.powergrid.PowerGrid;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ZincArmorMaterial {
    private static final Map<ArmorItem.Type, Integer> defenses = new EnumMap<>(ArmorItem.Type.class);

    static {
        defenses.put(ArmorItem.Type.HELMET, 1);
        defenses.put(ArmorItem.Type.CHESTPLATE, 3);
        defenses.put(ArmorItem.Type.LEGGINGS, 2);
        defenses.put(ArmorItem.Type.BOOTS, 1);
        defenses.put(ArmorItem.Type.BODY, 3);
    }

    public static final Holder<ArmorMaterial> INSTANCE = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(
            new ArmorMaterial(
                defenses,
                12,
                SoundEvents.ARMOR_EQUIP_GENERIC,
                () -> Ingredient.of(AllItems.ZINC_INGOT),
                List.of(new ArmorMaterial.Layer(PowerGrid.asResource("zinc"))),
                0, 0)
    );
}
