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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class ZincToolMaterial implements Tier {
    public static final ZincToolMaterial INSTANCE = new ZincToolMaterial();

    @Override
    public int getUses() {
        return 150;
    }

    @Override
    public float getSpeed() {
        return 6.0f;
    }

    @Override
    public float getAttackDamageBonus() {
        return 1.5f;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return null;
    }

//    @Override
//    public int getLevel() {
//        return 1;
//    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(AllItems.ZINC_INGOT);
    }

    @Override
    public Tool createToolProperties(TagKey<Block> block) {
        return Tier.super.createToolProperties(block);
    }
}
