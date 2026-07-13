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
package org.patryk3211.powergrid.equipment.baton;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.equipment.ItemBoostUtils;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;
import org.patryk3211.powergrid.equipment.ZincToolMaterial;

import java.util.UUID;

public class ElectroBatonItem extends SwordItem {
    protected static final UUID ATTACK_KNOCKBACK_MODIFIER_ID = UUID.fromString("14d913a8-879e-45ed-ab47-b5883ebac880");

    private final Multimap<Attribute, AttributeModifier> modifiers;

    public ElectroBatonItem(Properties settings) {
        super(ZincToolMaterial.INSTANCE, -1, -2.6f, settings.durability(20));

        float attackDamage = getDamage();
        float attackSpeed = -2.6f;
        float knockback = 1.0f;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", attackSpeed, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(ATTACK_KNOCKBACK_MODIFIER_ID, "Weapon modifier", knockback, AttributeModifier.Operation.ADDITION));
        modifiers = builder.build();
    }

    public static int energyPerUse() {
        return ModdedConfigs.server().equipment.electroBatonEnergyPerUse.get();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BatteryUtils.getBarColor(stack, energyPerUse());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BatteryUtils.isBarVisible(stack, energyPerUse());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return BatteryUtils.getBarWidth(stack, energyPerUse());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? modifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if(attacker instanceof Player player) {
            boolean boosted = ItemBoostUtils.useBoost(stack, attacker);
            float power = BatteryUtils.drawEnergy(player, energyPerUse());
            if(power > 0.3f) {
                // Apply stun
                var health = target.getMaxHealth();
                var stunStrength = (int) Mth.clamp(Math.round((boosted ? 60 : 30) - health) * power, 0, 10);
                if(stunStrength > 0)
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boosted ? 120 : 60, stunStrength, false, false));
            }
            return true;
        }
        stack.hurtAndBreak(1, attacker, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
}
