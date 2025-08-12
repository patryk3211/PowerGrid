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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.patryk3211.powergrid.PowerGrid;

public class ZincArmorMaterial implements ArmorMaterial {
    public static final ZincArmorMaterial INSTANCE = new ZincArmorMaterial();

    private final int[] durabilities = new int[] {
            110, 160, 150, 130
    };

    private final int[] protections = new int[] {
            1, 3, 2, 1
    };

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return durabilities[type.ordinal()];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return protections[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GENERIC;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(AllItems.ZINC_INGOT);
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
