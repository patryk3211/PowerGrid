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

import com.simibubi.create.AllEnchantments;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.collections.ModdedConfigs;

public class BatteryUtils {
    public static int getMaxCharge(ItemStack stack) {
        return getMaxCharge(EnchantmentHelper.getLevel(AllEnchantments.CAPACITY.get(), stack));
    }

    public static int getMaxCharge(int level) {
        return ModdedConfigs.server().electricity.portableBatteryBaseCapacity.get() +
                ModdedConfigs.server().electricity.portableBatteryEnchantCapacity.get() * level;
    }

    public static int getCurrentCharge(ItemStack stack) {
        if(!stack.hasNbt())
            return 0;
        var nbt = stack.getNbt();
        return nbt.getInt("Charge");
    }

    public static boolean drawEnergy(PlayerEntity player, int fe) {
        if(player.getAbilities().creativeMode)
            return true;
        var stack = player.getEquippedStack(EquipmentSlot.CHEST);
        if(stack.isEmpty() || !(stack.getItem() instanceof PortableBatteryItem))
            return false;
        var charge = getCurrentCharge(stack);
        if(charge < fe)
            return false;
        stack.getNbt().putInt("Charge", charge - fe);
        return true;
    }

    public static ItemStack getBattery(PlayerEntity player) {
        var stack = player.getEquippedStack(EquipmentSlot.CHEST);
        if(!(stack.getItem() instanceof PortableBatteryItem))
            return null;
        return stack;
    }

    public static boolean isBarVisible(ItemStack stack, int fePerUse) {
        if(fePerUse == 0)
            return false;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> () -> MinecraftClient.getInstance().player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery != null && getCurrentCharge(battery) > fePerUse)
                        return true;
                    return stack.isDamaged();
                }).orElse(false);
    }

    public static int getBarWidth(ItemStack stack, int fePerUse) {
        if(fePerUse == 0)
            return 13;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> () -> MinecraftClient.getInstance().player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery == null)
                        return Math.round(13.0F - (float) stack.getDamage() / stack.getMaxDamage() * 13.0F);
                    return battery.getItemBarStep();
                }).orElse(13);
    }

    public static int getBarColor(ItemStack stack, int fePerUse) {
        if(fePerUse == 0)
            return 0;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> () -> MinecraftClient.getInstance().player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery == null)
                        return MathHelper.hsvToRgb(Math.max(0.0F, 1.0F - (float) stack.getDamage() / stack.getMaxDamage()) / 3.0F, 1.0F, 1.0F);
                    return battery.getItemBarColor();
                }).orElse(0);
    }
}
