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

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
    private static Vec3d complexRaycast(Entity entity, Vec3d min, Vec3d max, double distance) {
        assert entity instanceof IComplexRaycast;
        IComplexRaycast checker = (IComplexRaycast) entity;

        Box entityBB = entity.getBoundingBox().expand(entity.getTargetingMargin());
        Optional<Vec3d> potentialHit = entityBB.raycast(min, max);
        if(entityBB.contains(min)) {
            // Casting entity inside of potential hit entity
            return checker.raycast(min, max);
        } else if(potentialHit.isPresent()) {
            if(min.squaredDistanceTo(potentialHit.get()) < distance) {
                // Ray hits bounding box of potential hit entity
                return checker.raycast(min, max);
            }
        }
        return null;
    }

    @Redirect(
            method="updateTargetedEntity(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
            )
    )
    private EntityHitResult complexRaycast(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, double d) {
        EntityHitResult baseResult = ProjectileUtil.raycast(entity, min, max, box, predicate, d);

        World world = entity.getWorld();
        double currentHitDistance = d;
        Entity currentHitEntity = null;
        Vec3d currentHitPoint = null;

        if(baseResult != null) {
            currentHitEntity = baseResult.getEntity();
            currentHitPoint = baseResult.getPos();
            currentHitDistance = min.squaredDistanceTo(currentHitPoint);
        }

        for(Entity potentialHitEntity : world.getOtherEntities(entity, box, testEntity -> !testEntity.isSpectator() && testEntity instanceof IComplexRaycast)) {
            Vec3d hit = complexRaycast(potentialHitEntity, min, max, currentHitDistance);
            if(hit != null) {
                double hitSquaredDistance = min.squaredDistanceTo(hit);
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
