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

import com.simibubi.create.AllEnchantments;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.utility.ClientSideAccess;

public class BatteryUtils {

    public static int getMaxCharge(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int level = 0;
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(AllEnchantments.CAPACITY)) {
                level = entry.getIntValue();
                break;
            }
        }
        return getMaxCharge(level);
    }

    public static int getMaxCharge(int level) {
        return ModdedConfigs.server().electricity.portableBatteryBaseCapacity.get() +
                ModdedConfigs.server().electricity.portableBatteryEnchantCapacity.get() * level;
    }

    public static int getCurrentCharge(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if(data == null)
            return 0;

        CompoundTag tag = data.copyTag();
        return tag.getInt("Charge");
    }

    public static boolean drawEnergy(Player player, int fe) {
        if(player.getAbilities().instabuild)
            return true;
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(stack.isEmpty() || !(stack.getItem() instanceof PortableBatteryItem))
            return false;
        var charge = getCurrentCharge(stack);
        if(charge < fe)
            return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag newTag = data.copyTag();

        newTag.putInt("Charge", charge - fe);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
        return true;
    }

    public static ItemStack getBattery(Player player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!(stack.getItem() instanceof PortableBatteryItem))
            return null;
        return stack;
    }

    public static boolean isBarVisible(ItemStack stack, int fePerUse) {
        if(fePerUse == 0)
            return false;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery != null && getCurrentCharge(battery) >= fePerUse)
                        return true;
                    return stack.isDamaged();
                }).orElse(false);
    }

    public static int getBarWidth(ItemStack stack, int fePerUse) {
        if(fePerUse == 0)
            return 13;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery == null || getCurrentCharge(battery) < fePerUse)
                        return Math.round(13.0F - (float) stack.getDamageValue() / stack.getMaxDamage() * 13.0F);
                    return battery.getBarWidth();
                }).orElse(13);
    }

    public static int getBarColor(ItemStack stack, int fePerUse) {
        if(fePerUse == 0)
            return 0;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery == null || getCurrentCharge(battery) < fePerUse)
                        return Mth.hsvToRgb(Math.max(0.0F, 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage()) / 3.0F, 1.0F, 1.0F);
                    return battery.getBarColor();
                }).orElse(0);
    }
}
