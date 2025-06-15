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

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.network.packets.ZapProjectileS2CPacket;

import java.util.ArrayList;

public class ZapProjectileEntity extends ProjectileEntity {
    public ZapProjectileEntity(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
    }

    public static ZapProjectileEntity create(World world, Vec3d position, Vec3d velocity, float yaw, float pitch) {
        var entity = new ZapProjectileEntity(ModdedEntities.ZAP_PROJECTILE.get(), world);
        entity.setPos(position.x, position.y, position.z);
        entity.setVelocity(velocity);
        ProjectileUtil.setRotationFromVelocity(entity, 1.0f);
        entity.resetPosition();
        entity.refreshPosition();
        return entity;
    }

    @Override
    protected void initDataTracker() {

    }

    public static void playHitSound(World world, Vec3d location) {
//        M.POTATO_HIT.playOnServer(world, BlockPos.ofFloored(location));
    }

    public static void playLaunchSound(World world, Vec3d location, float pitch) {
        ModdedSoundEvents.ELECTROZAPPER_SHOOT.playAt(world, location, 1, pitch, true);
    }

    @Override
    public void tick() {
        var hit = ProjectileUtil.getCollision(this, this::canHit);
        if(hit.getType() != HitResult.Type.MISS) {
            this.onCollision(hit);
        }

        checkBlockCollision();
        var velocity = this.getVelocity();
        var x = this.getX() + velocity.x;
        var y = this.getY() + velocity.y;
        var z = this.getZ() + velocity.z;
        ProjectileUtil.setRotationFromVelocity(this, 1.0f);
        if(this.isTouchingWater()) {
            kill();
            return;
        }

        var world = getWorld();
        var r = world.random;
        for(int i = 0; i < 4; ++i) {
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
        }

//        setVelocity(getVelocity().add(0, -0.05, 0));
        setPosition(x, y, z);
    }

    private DamageSource causeZapDamage() {
        Registry<DamageType> registry = getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
        return new DamageSource(registry.getEntry(ModdedDamageTypes.ZAP).get(), this, getOwner());
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        this.prevPitch = getPitch();
        this.prevYaw = getYaw();
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        var owner = getOwner();
        var target = hit.getEntity();
        if(!target.isAlive())
            return;
        if(owner instanceof LivingEntity living)
            living.onAttacking(target);

        if(target instanceof WitherEntity wither && wither.shouldRenderOverlay())
            return;

        float damage = 4;

        var effectBB = new Box(target.getBlockPos()).expand(2);
        var world = getWorld();
        var source = causeZapDamage();
        var affectedEntities = world.getOtherEntities(target, effectBB, e -> e instanceof LivingEntity && !e.isInvulnerableTo(source));

        var onServer = !world.isClient;
        damage /= Math.min(affectedEntities.size() + 1, 3);
        if(onServer && !target.damage(source, damage)) {
            kill();
            return;
        }

        if(onServer) {
            var damagedEntities = new ArrayList<Entity>();
            for(var entity : affectedEntities) {
                if(entity.damage(source, damage))
                    damagedEntities.add(entity);
                if(damagedEntities.size() >= 2)
                    break;
            }
            ModdedPackets.getChannel().sendToClientsAround(new ZapProjectileS2CPacket(target, damagedEntities), (ServerWorld) getWorld(), getPos(), 50);
        }

        if(target.getType() == EntityType.ENDERMAN)
            return;

        if(!(target instanceof LivingEntity livingTarget)) {
//            playHitSound(getWorld(), getPos());
            kill();
            return;
        }

//        if (onServer && knockback > 0) {
//            Vec3d appliedMotion = this.getVelocity()
//                    .multiply(1.0D, 0.0D, 1.0D)
//                    .normalize()
//                    .multiply(knockback * 0.6);
//            if (appliedMotion.lengthSquared() > 0.0D)
//                livingentity.addVelocity(appliedMotion.x, 0.1D, appliedMotion.z);
//        }

        if(onServer && owner instanceof LivingEntity livingOwner) {
            EnchantmentHelper.onUserDamaged(livingTarget, livingOwner);
            EnchantmentHelper.onTargetDamaged(livingOwner, livingTarget);
        }

        if(livingTarget != owner && livingTarget instanceof PlayerEntity && owner instanceof ServerPlayerEntity ownerPlayer && !isSilent()) {
            ownerPlayer.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER, 0.0F));
        }

        if(onServer && owner instanceof ServerPlayerEntity serverPlayer) {
//            if (!target.isAlive() && target.getType()
//                    .getSpawnGroup() == SpawnGroup.MONSTER || (target instanceof PlayerEntity && target != owner))
//                AllAdvancements.POTATO_CANNON.awardTo(serverplayerentity);
        }

        kill();

//        float damage = projectileType.getDamage() * additionalDamageMult;
//        float knockback = projectileType.getKnockback() + additionalKnockback;
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if(!getWorld().isClient) {
            ModdedPackets.getChannel().sendToClientsTracking(new ZapProjectileS2CPacket(hit), this);
        }
        kill();
    }
}
