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
package org.patryk3211.powergrid.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.utility.IComplexRaycast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class ComplexEntityRaycastMixin {
    @Unique
    @Nullable
    private static Vec3 complexRaycast(Entity entity, Vec3 min, Vec3 max, double distance) {
        assert entity instanceof IComplexRaycast;
        IComplexRaycast checker = (IComplexRaycast) entity;

        AABB entityBB = entity.getBoundingBox().inflate(entity.getPickRadius());
        Optional<Vec3> potentialHit = entityBB.clip(min, max);
        if(entityBB.contains(min)) {
            // Casting entity inside of potential hit entity
            return checker.raycast(min, max);
        } else if(potentialHit.isPresent()) {
            if(min.distanceToSqr(potentialHit.get()) < distance) {
                // Ray hits bounding box of potential hit entity
                return checker.raycast(min, max);
            }
        }
        return null;
    }

    @Redirect(
            method="pick(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"
            )
    )
    private EntityHitResult complexRaycast(Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, double d) {
        EntityHitResult baseResult = ProjectileUtil.getEntityHitResult(entity, min, max, box, predicate, d);

        Level world = entity.level();
        double currentHitDistance = d;
        Entity currentHitEntity = null;
        Vec3 currentHitPoint = null;

        if(baseResult != null) {
            currentHitEntity = baseResult.getEntity();
            currentHitPoint = baseResult.getLocation();
            currentHitDistance = min.distanceToSqr(currentHitPoint);
        }

        for(Entity potentialHitEntity : world.getEntities(entity, box, testEntity -> !testEntity.isSpectator() && testEntity instanceof IComplexRaycast)) {
            Vec3 hit = complexRaycast(potentialHitEntity, min, max, currentHitDistance);
            if(hit != null) {
                double hitSquaredDistance = min.distanceToSqr(hit);
                if(hitSquaredDistance < currentHitDistance) {
                    currentHitEntity = potentialHitEntity;
                    currentHitPoint = hit;
                    currentHitDistance = hitSquaredDistance;
                }
            }
        }

        if(currentHitEntity == null) {
            return null;
        } else {
            return new EntityHitResult(currentHitEntity, currentHitPoint);
        }
    }
}
