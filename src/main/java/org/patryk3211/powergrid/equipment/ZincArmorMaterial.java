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

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.recipes.RecipeTags;

public class ZincArmorMaterial implements ArmorMaterial {
    public static final ZincArmorMaterial INSTANCE = new ZincArmorMaterial();

    private final int[] durabilities = new int[] {
            110, 160, 150, 130
    };

    private final int[] protections = new int[] {
            1, 3, 2, 1
    };

    @Override
    public int getDurability(ArmorItem.Type type) {
        return durabilities[type.ordinal()];
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return protections[type.ordinal()];
    }

    @Override
    public int getEnchantability() {
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_GENERIC;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.fromTag(RecipeTags.zincIngot());
    }

    @Override
    public String getName() {
        return PowerGrid.asResource("zinc").toString();
    }

    @Override
    public float getToughness() {
        return 0;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }
}
