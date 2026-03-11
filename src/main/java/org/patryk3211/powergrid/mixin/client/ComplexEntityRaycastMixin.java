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

import net.minecraft.util.Mth;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ComplexEntityRaycastMixin {
    @Unique
    @Nullable
    private static Vec3 powerGrid$complexRaycast(Entity entity, Vec3 min, Vec3 max, double distance) {
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

    @Inject(
            method= "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(value = "RETURN"),
            cancellable = true
    )
    private static void powerGrid$complexRaycast(Entity shooter, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, double distance, CallbackInfoReturnable<EntityHitResult> cir) {
        EntityHitResult baseResult = cir.getReturnValue();

        Level world = shooter.level();
        double currentHitDistance = distance;
        Entity currentHitEntity = null;
        Vec3 currentHitPoint = null;

        if(baseResult != null) {
            currentHitPoint = baseResult.getLocation();
            currentHitDistance = startVec.distanceToSqr(currentHitPoint);
        }

        for(var potentialHitEntity : world.getEntities().getAll()) {
            if(potentialHitEntity.isSpectator() || !(potentialHitEntity instanceof IComplexRaycast))
                continue;
            // Perform a cheap bounding box distance check first before going for the complex cast.
            var bb = potentialHitEntity.getBoundingBox();
            // Closest point to start in bounding box
            var cX = Mth.clamp(startVec.x, bb.minX, bb.maxX);
            var cY = Mth.clamp(startVec.y, bb.minY, bb.maxY);
            var cZ = Mth.clamp(startVec.z, bb.minZ, bb.maxZ);
            if(startVec.distanceToSqr(cX, cY, cZ) >= currentHitDistance)
                continue;
            Vec3 hit = powerGrid$complexRaycast(potentialHitEntity, startVec, endVec, currentHitDistance);
            if(hit != null) {
                double hitSquaredDistance = startVec.distanceToSqr(hit);
                if(hitSquaredDistance < currentHitDistance) {
                    currentHitEntity = potentialHitEntity;
                    currentHitPoint = hit;
                    currentHitDistance = hitSquaredDistance;
                }
            }
        }

        if(currentHitEntity != null) {
            // We found a closer entity
            cir.setReturnValue(new EntityHitResult(currentHitEntity, currentHitPoint));
        }
    }
}
