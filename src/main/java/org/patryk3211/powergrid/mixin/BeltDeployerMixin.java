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

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BeltDeployerCallbacks.class)
public class BeltDeployerMixin {
    @Inject(method = "activate",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity;award(Lcom/simibubi/create/foundation/advancement/CreateAdvancement;)V"))
    private static void activateMixin(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler, DeployerBlockEntity blockEntity, Recipe<?> recipe, CallbackInfo ci, @Local List<TransportedItemStack> collect) {
        var player = blockEntity.getPlayer();
        var stack = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        if(stack.is(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)) {
            if(!stack.hasTag())
                return;
            var schematic = stack.getTagElement("Schematic");
            if(schematic == null)
                return;
            for(var item : collect) {
                if(!item.stack.is(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag))
                    continue;
                var tag = item.stack.getOrCreateTag();
                tag.put("Schematic", schematic.copy());
            }
        }
    }
}
