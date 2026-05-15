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
package org.patryk3211.powergrid.equipment.portablebattery;

import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.equipment.armor.BaseArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;
import java.util.function.Supplier;

public class PortableBatteryItem extends BaseArmorItem implements IHaveElectricProperties {
    public static final int BAR_COLOR = 0xEFEFDE;
    private Supplier<BacktankItem.BacktankBlockItem> blockItem;

    public PortableBatteryItem(Holder<ArmorMaterial> material, Properties settings, ResourceLocation textureLoc, Supplier<BacktankItem.BacktankBlockItem> placeable) {
        super(material, Type.CHESTPLATE, settings, textureLoc);
        this.blockItem = placeable;
    }

    public static PortableBatteryItem getWornBy(Entity entity) {
        if(!(entity instanceof LivingEntity livingEntity))
            return null;
        if(livingEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof PortableBatteryItem battery)
            return battery;
        return null;
    }

    public Block getBlock() {
        return blockItem.get().getBlock();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float current = Mth.clamp((float) BatteryUtils.getCurrentCharge(stack) / BatteryUtils.getMaxCharge(stack), 0, 1);
        return Math.round(13.0f * current);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return blockItem.get()
                .useOn(ctx);
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(ModdedBlocks.PORTABLE_BATTERY.get().resistance(), player, tooltip);
        Power.max(ModdedBlocks.PORTABLE_BATTERY.asStack(), player, tooltip);
    }
}
