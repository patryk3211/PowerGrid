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
package org.patryk3211.powergrid.equipment.zapper;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.equipment.BoostingChipItem;
import org.patryk3211.powergrid.equipment.ItemBoostUtils;
import org.patryk3211.powergrid.equipment.portablebattery.BatteryUtils;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ElectroZapperItem extends ProjectileWeaponItem implements CustomArmPoseItem {
    public static final int MAX_DAMAGE = 50;

    public ElectroZapperItem(Properties settings) {
        super(settings.durability(MAX_DAMAGE));
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return $ -> false;
    }

    public boolean isZapper(ItemStack stack) {
        return stack.getItem() instanceof ElectroZapperItem;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
        return false;
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
    public int getBarColor(ItemStack stack) {
        return BatteryUtils.getBarColor(stack, energyPerUse());
    }

    public static int energyPerUse() {
        return ModdedConfigs.server().equipment.electroZapperEnergyPerShot.get();
    }

    @Environment(EnvType.CLIENT)
    private static void clientUse(InteractionHand hand) {
        PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.dontAnimateItem(hand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        boolean boosted = ItemBoostUtils.useBoost(stack, user);
        if(world.isClientSide) {
            clientUse(hand);
            return InteractionResultHolder.success(stack);
        }
        if(!boosted) {
            var otherStack = user.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if(otherStack.getItem() instanceof BoostingChipItem)
                return InteractionResultHolder.pass(stack);
        }

        float power = BatteryUtils.drawEnergy(user, energyPerUse());
        var barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(user, hand == InteractionHand.MAIN_HAND,
                new Vec3(.25f, -0.15f, 1.0f));
        var correction = ShootableGadgetItemMethods.getGunBarrelVec(user, hand == InteractionHand.MAIN_HAND,
                new Vec3(0, 0, 0)).subtract(user.position().add(0, user.getEyeHeight(), 0));

        var lookVec = user.getLookAngle();
        var motion = lookVec.add(correction)
                .normalize()
                .scale(4);

        var projectile = ZapProjectileEntity.create(world, barrelPos, motion, Math.max(power, 0.5f) * (boosted ? 2 : 1));
        projectile.setOwner(user);
        world.addFreshEntity(projectile);

        ShootableGadgetItemMethods.applyCooldown(user, stack, hand, this::isZapper, 10);
        Function<Boolean, ElectroZapperS2CPacket> factory = b -> new ElectroZapperS2CPacket(barrelPos, lookVec.normalize(), stack, hand, 1, b);
        ModdedPackets.sendToClientsTracking(factory.apply(false), user);
        ModdedPackets.sendToClient(factory.apply(true), (ServerPlayer) user);
        if(power == 0)
            stack.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("powergrid.electrozapper.bolt").append(Component.literal(":"))
                .withStyle(ChatFormatting.GRAY));
        var spacing = Component.literal(" ");

        float damageF = 8;//type.getDamage() * additionalDamageMult;
        var damage = Component.literal(damageF == Mth.floor(damageF) ? "" + Mth.floor(damageF) : "" + damageF);
        var reloadTicks = Component.literal("10");

//        damage = damage.formatted(Formatting.DARK_GREEN);

        tooltip.add(spacing.plainCopy().append(Lang.translateDirect("electrozapper.bolt.damage", damage).withStyle(ChatFormatting.DARK_GREEN)));
        tooltip.add(spacing.plainCopy().append(Lang.translateDirect("electrozapper.bolt.reload", reloadTicks).withStyle(ChatFormatting.DARK_GREEN)));
        super.appendHoverText(stack, world, tooltip, context);
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand) {
        if(!player.swinging) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return null;
    }

    public boolean onEntitySwing(ItemStack itemStack, LivingEntity livingEntity) {
        return true;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }
}
