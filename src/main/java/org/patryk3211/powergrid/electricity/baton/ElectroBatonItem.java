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
package org.patryk3211.powergrid.electricity.baton;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.electricity.portablebattery.BatteryUtils;
import org.patryk3211.powergrid.equipment.ZincToolMaterial;

import java.util.UUID;

public class ElectroBatonItem extends SwordItem {
    protected static final UUID ATTACK_KNOCKBACK_MODIFIER_ID = UUID.fromString("14d913a8-879e-45ed-ab47-b5883ebac880");

    private final Multimap<EntityAttribute, EntityAttributeModifier> modifiers;

    public ElectroBatonItem(Settings settings) {
        super(ZincToolMaterial.INSTANCE, -1, -2.6f, settings.maxDamage(20));

        float attackDamage = getAttackDamage();
        float attackSpeed = -2.6f;
        float knockback = 1.0f;
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", attackDamage, EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", attackSpeed, EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, new EntityAttributeModifier(ATTACK_KNOCKBACK_MODIFIER_ID, "Weapon modifier", knockback, EntityAttributeModifier.Operation.ADDITION));
        modifiers = builder.build();
    }

    public static int fePerUse() {
        return 100;
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return BatteryUtils.getBarColor(stack, fePerUse());
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return BatteryUtils.isBarVisible(stack, fePerUse());
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return BatteryUtils.getBarWidth(stack, fePerUse());
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? modifiers : super.getAttributeModifiers(slot);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if(attacker instanceof PlayerEntity player) {
            if(BatteryUtils.drawEnergy(player, fePerUse())) {
                // Apply stun
                var health = target.getMaxHealth();
                var stunStrength = MathHelper.clamp(Math.round(30 - health), 0, 10);
                if(stunStrength > 0)
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, stunStrength, false, false));
                return true;
            }
        }
        stack.damage(1, attacker, (e) -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return true;
    }
}
