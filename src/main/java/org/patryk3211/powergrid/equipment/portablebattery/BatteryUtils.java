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
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.equipment.ItemBoostUtils;
import org.patryk3211.powergrid.utility.ClientSideAccess;
import org.patryk3211.powergrid.utility.Lang;

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
        return ModdedConfigs.server().equipment.portableBatteryBaseCapacity.get() +
                ModdedConfigs.server().equipment.portableBatteryEnchantCapacity.get() * level;
    }

    public static int getCurrentCharge(ItemStack stack) {
        return stack.getOrDefault(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get(), 0);
    }

    public static float drawEnergy(Player player, int energy) {
        if(player.getAbilities().instabuild)
            return 1.0f;
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(stack.isEmpty() || !(stack.getItem() instanceof PortableBatteryItem))
            return 0.0f;
        var charge = getCurrentCharge(stack);
        float chargePercent = (float) getCurrentCharge(stack) / getMaxCharge(stack);
        float outputPercent = 1.0f;
        if(chargePercent < 0.5f) {
            // High ESR causing lower energy output
            outputPercent = chargePercent / 0.5f;
            if(outputPercent < 0.25f)
                outputPercent = 0.25f;
        }
        if(charge < energy) {
            stack.remove(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get());
            return 0.0f;
        }
        stack.set(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get(), Math.max(charge - energy, 0));
        if(player instanceof ServerPlayer serverPlayer) {
            float maxCharge = getMaxCharge(stack);
            sendWarning(serverPlayer, charge, charge - energy, (maxCharge / 10));
            sendWarning(serverPlayer, charge, charge - energy, 25);
        }
        return outputPercent;
    }
    public static int tryDrawEnergy(ItemStack stack, int fe) {
        if(stack.isEmpty() || !(stack.getItem() instanceof PortableBatteryItem))
            return 0;
        var charge = getCurrentCharge(stack);
        if(charge < fe)
            fe = charge;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag newTag = data.copyTag();

        newTag.putInt("Charge", charge - fe);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
        return fe;
    }

    public static ItemStack getBattery(Player player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if(!(stack.getItem() instanceof PortableBatteryItem))
            return null;
        return stack;
    }

    public static float tryDrawEnergy(ItemStack battery, int energy) {
        var charge = getCurrentCharge(battery);
        float chargePercent = (float) getCurrentCharge(battery) / getMaxCharge(battery);
        float outputPercent = 1.0f;
        if(chargePercent < 0.5f) {
            // High ESR causing lower energy output
            outputPercent = chargePercent / 0.5f;
            if(outputPercent < 0.25f)
                outputPercent = 0.25f;
        }
        if(energy == 0 || !battery.has(ModdedDataComponents.PORTABLE_BATTERY_CHARGE.get()))
            return 0.0f;
        if(charge < energy)
            return 0.0f;
        return outputPercent;
    }

    private static void sendWarning(ServerPlayer player, float charge, float newCharge, float threshold) {
        if (newCharge > threshold)
            return;
        if (charge <= threshold)
            return;

        boolean depleted = threshold <= 25;
        MutableComponent component = Lang.translateDirect(depleted ? "gui.portable.battery.depleted" : "gui.portable.battery.low");

        ModdedSoundEvents.UI_FAIL.play(player.level(), null, player.blockPosition(), .75f, 1);
        ModdedSoundEvents.WIRE_BURNED.play(player.level(), null, player.blockPosition(), 1, .5f);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("\u26A0 ").withStyle(depleted ? ChatFormatting.RED : ChatFormatting.GOLD)
                        .append(component.withStyle(ChatFormatting.GRAY))));
        player.connection.send(new ClientboundSetTitleTextPacket(CommonComponents.EMPTY));
    }

    public static boolean isBarVisible(ItemStack stack, int energyPerUse, float minPower) {
        if(energyPerUse == 0)
            return false;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery != null && tryDrawEnergy(battery, energyPerUse) > minPower)
                        return true;
                    return stack.isDamaged();
                }).orElse(false);
    }

    public static int getBarWidth(ItemStack stack, int energyPerUse, float minPower) {
        if(energyPerUse == 0)
            return 13;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery == null || tryDrawEnergy(battery, energyPerUse) <= minPower)
                        return Math.round(13.0F - (float) stack.getDamageValue() / stack.getMaxDamage() * 13.0F);
                    return battery.getBarWidth();
                }).orElse(13);
    }

    public static int getBarColor(ItemStack stack, int energyPerUse, float minPower) {
        if(energyPerUse == 0)
            return 0;
        return EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(player -> {
                    var battery = getBattery(player);
                    if(battery == null || tryDrawEnergy(battery, energyPerUse) <= minPower)
                        return Mth.hsvToRgb(Math.max(0.0F, 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage()) / 3.0F, 1.0F, 1.0F);
                    if(ItemBoostUtils.isBoosted(stack))
                        return 0x34a8eb;
                    return battery.getBarColor();
                }).orElse(0);
    }

    public static boolean isBarVisible(ItemStack stack, int energyPerUse) {
        return isBarVisible(stack, energyPerUse, 0);
    }

    public static int getBarWidth(ItemStack stack, int energyPerUse) {
        return getBarWidth(stack, energyPerUse, 0);
    }

    public static int getBarColor(ItemStack stack, int energyPerUse) {
        return getBarColor(stack, energyPerUse, 0);
    }
}
