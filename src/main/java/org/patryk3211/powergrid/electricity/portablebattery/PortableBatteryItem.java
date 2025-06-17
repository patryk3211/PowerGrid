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
package org.patryk3211.powergrid.electricity.portablebattery;

import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.equipment.armor.BaseArmorItem;
import com.simibubi.create.content.equipment.armor.CapacityEnchantment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.function.Supplier;

public class PortableBatteryItem extends BaseArmorItem implements CapacityEnchantment.ICapacityEnchantable {
    public static final int BAR_COLOR = 0xEFEFDE;
    private Supplier<BacktankItem.BacktankBlockItem> blockItem;

    public PortableBatteryItem(ArmorMaterial material, Settings settings, Identifier textureLoc, Supplier<BacktankItem.BacktankBlockItem> placeable) {
        super(material, Type.CHESTPLATE, settings, textureLoc);
        this.blockItem = placeable;
    }

    public static PortableBatteryItem getWornBy(Entity entity) {
        if(!(entity instanceof LivingEntity livingEntity))
            return null;
        if(livingEntity.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof PortableBatteryItem battery)
            return battery;
        return null;
    }

    public Block getBlock() {
        return blockItem.get().getBlock();
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        float current = MathHelper.clamp((float) BatteryUtils.getCurrentCharge(stack) / BatteryUtils.getMaxCharge(stack), 0, 1);
        return Math.round(13.0f * current);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        return blockItem.get()
                .useOnBlock(ctx);
    }
}
