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

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.network.packets.ZapProjectileS2CPacket;

import java.util.ArrayList;

public class ZapProjectileEntity extends AbstractHurtingProjectile {
    private float power;

    public ZapProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level world) {
        super(type, world);
    }

    public static ZapProjectileEntity create(Level world, Vec3 position, Vec3 velocity, float power) {
        var entity = new ZapProjectileEntity(ModdedEntities.ZAP_PROJECTILE.get(), world);
        entity.setPosRaw(position.x, position.y, position.z);
        entity.setDeltaMovement(velocity);
        ProjectileUtil.rotateTowardsMovement(entity, 1.0f);
        entity.setOldPosAndRot();
        entity.reapplyPosition();
        entity.power = power;
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    public static void playHitSound(Level world, Vec3 location) {
//        M.POTATO_HIT.playOnServer(world, BlockPos.ofFloored(location));
    }

    public static void playLaunchSound(Level world, Vec3 location, float pitch) {
        ModdedSoundEvents.ELECTROZAPPER_SHOOT.playAt(world, location, 1, pitch, true);
    }

    @Override
    public void tick() {
        super.tick();
        var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if(hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
        }

        checkInsideBlocks();
        var velocity = this.getDeltaMovement();
        var x = this.getX() + velocity.x;
        var y = this.getY() + velocity.y;
        var z = this.getZ() + velocity.z;
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
        if(this.isInWater()) {
            kill();
            return;
        }

        var world = level();
        var r = world.random;
        for(int i = 0; i < 4; ++i) {
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
        }

//        setVelocity(getVelocity().add(0, -0.05, 0));
        setPos(x, y, z);
    }

    private DamageSource causeZapDamage() {
        Registry<DamageType> registry = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolder(ModdedDamageTypes.ZAP).get(), this, getOwner());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.xRotO = getXRot();
        this.yRotO = getYRot();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        var owner = getOwner();
        var target = hit.getEntity();
        if(!target.isAlive())
            return;
        if(owner instanceof LivingEntity living)
            living.setLastHurtMob(target);

        if(target instanceof WitherBoss wither && wither.isPowered())
            return;

        float damage = 8 * power;

        var effectBB = new AABB(target.blockPosition()).inflate(2);
        var world = level();
        var source = causeZapDamage();
        var affectedEntities = world.getEntities(target, effectBB, e -> e instanceof LivingEntity && !e.isInvulnerableTo(source));

        var onServer = !world.isClientSide;
        damage /= Math.min(affectedEntities.size() + 1, 3);
        if(onServer && !target.hurt(source, damage)) {
            kill();
            return;
        }

        if(onServer && power > 0.6f) {
            var damagedEntities = new ArrayList<Entity>();
            for(var entity : affectedEntities) {
                if(entity.hurt(source, damage))
                    damagedEntities.add(entity);
                if(damagedEntities.size() >= 2)
                    break;
            }
            ModdedPackets.sendToClientsAround(new ZapProjectileS2CPacket(target, damagedEntities), (ServerLevel) level(), position(), 50);
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

            EnchantmentHelper.doPostAttackEffects(world.getServer().getLevel(livingTarget.level().dimension()), livingTarget, livingOwner.damageSources().playerAttack((Player) livingOwner));
        }

        if(livingTarget != owner && livingTarget instanceof Player && owner instanceof ServerPlayer ownerPlayer && !isSilent()) {
            ownerPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
        }

        if(onServer && owner instanceof ServerPlayer serverPlayer) {
//            if (!target.isAlive() && target.getType()
//                    .getSpawnGroup() == SpawnGroup.MONSTER || (target instanceof PlayerEntity && target != owner))
//                AllAdvancements.POTATO_CANNON.awardTo(serverplayerentity);
        }

        kill();

//        float damage = projectileType.getDamage() * additionalDamageMult;
//        float knockback = projectileType.getKnockback() + additionalKnockback;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Power", power);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        power = compound.getFloat("Power");
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if(!level().isClientSide) {
            ModdedPackets.sendToClientsTracking(new ZapProjectileS2CPacket(hit), this);
        }
        kill();
    }
}
