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
package org.patryk3211.powergrid.electricity.zapper;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.utility.Components;
import io.github.fabricators_of_create.porting_lib.item.EntitySwingListenerItem;
import io.github.fabricators_of_create.porting_lib.item.ReequipAnimationItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ElectroZapperItem extends RangedWeaponItem implements CustomArmPoseItem, EntitySwingListenerItem, ReequipAnimationItem {
    public ElectroZapperItem(Settings settings) {
        super(settings.maxDamage(50));
    }

    @Override
    public Predicate<ItemStack> getProjectiles() {
        return $ -> false;
    }

    public boolean isZapper(ItemStack stack) {
        return stack.getItem() instanceof ElectroZapperItem;
    }

    @Override
    public int getRange() {
        return 15;
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return false;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if(world.isClient) {
            PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.dontAnimateItem(hand);
            return TypedActionResult.success(stack);
        }

        var barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(user, hand == Hand.MAIN_HAND,
                new Vec3d(.25f, -0.15f, 1.0f));
        var correction = ShootableGadgetItemMethods.getGunBarrelVec(user, hand == Hand.MAIN_HAND,
                new Vec3d(0, 0, 0)).subtract(user.getPos().add(0, user.getStandingEyeHeight(), 0));

        var lookVec = user.getRotationVector();
        var motion = lookVec.add(correction)
                .normalize()
                .multiply(4);

        var projectile = ZapProjectileEntity.create(world, barrelPos, motion, (float) lookVec.y, (float) lookVec.x);
        projectile.setOwner(user);
        world.spawnEntity(projectile);

        ShootableGadgetItemMethods.applyCooldown(user, stack, hand, this::isZapper, 10);
        Function<Boolean, ElectroZapperPacket> factory = b -> new ElectroZapperPacket(barrelPos, lookVec.normalize(), stack, hand, 1, b);
        ModdedPackets.getChannel().sendToClientsTracking(factory.apply(false), user);
        ModdedPackets.getChannel().sendToClient(factory.apply(true), (ServerPlayerEntity) user);
        stack.damage(1, user, $ -> {});
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Components.immutableEmpty());
        tooltip.add(Components.translatable("powergrid.electrozapper.bolt").append(Components.literal(":"))
                .formatted(Formatting.GRAY));
        var spacing = Components.literal(" ");

        float damageF = 4;//type.getDamage() * additionalDamageMult;
        var damage = Components.literal(damageF == MathHelper.floor(damageF) ? "" + MathHelper.floor(damageF) : "" + damageF);
        var reloadTicks = Components.literal("10");

//        damage = damage.formatted(Formatting.DARK_GREEN);

        tooltip.add(spacing.copyContentOnly().append(Lang.translateDirect("electrozapper.bolt.damage", damage).formatted(Formatting.DARK_GREEN)));
        tooltip.add(spacing.copyContentOnly().append(Lang.translateDirect("electrozapper.bolt.reload", reloadTicks).formatted(Formatting.DARK_GREEN)));
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public BipedEntityModel.@Nullable ArmPose getArmPose(ItemStack stack, AbstractClientPlayerEntity player, Hand hand) {
        if(!player.handSwinging) {
            return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        }
        return null;
    }

    @Override
    public boolean onEntitySwing(ItemStack itemStack, LivingEntity livingEntity) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }
}
