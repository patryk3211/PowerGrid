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
package org.patryk3211.powergrid.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(
            method = "onStruckByLightning(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LightningEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onLightningStrike(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        if((Object) this instanceof ItemEntity item) {
            var stack = item.getStack();
            if(stack.isOf(Items.IRON_INGOT)) {
                var r = world.random;
                if(r.nextFloat() < ModdedConfigs.server().recipes.lightningMagnetizationChance.getF()) {
                    // Success
                    var magnetItemEntity = new ItemEntity(world, item.getX(), item.getY(), item.getZ(), ModdedItems.MAGNET.asStack());
                    magnetItemEntity.setVelocity(
                            (r.nextFloat() - 0.5f) * 0.1f,
                            r.nextFloat() * 0.1f + 0.1f,
                            (r.nextFloat() - 0.5f) * 0.1f
                    );
                    magnetItemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(magnetItemEntity);

                    stack.decrement(1);
                    if(stack.isEmpty())
                        item.discard();
                }
                ci.cancel();
            } else if(stack.isOf(ModdedItems.MAGNET.get())) {
                // Don't damage magnet item
                ci.cancel();
            }
        }
    }
}
