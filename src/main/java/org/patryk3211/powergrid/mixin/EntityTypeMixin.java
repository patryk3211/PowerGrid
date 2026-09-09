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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    @Unique
    private static final Set<String> powergrid$WIRE_ENTITY_PATHS = Set.of(
            "hanging_wire",
            "block_wire",
            "cord",
            "string_light_cord");

    @Inject(method = "clientTrackingRange()I", at = @At("RETURN"), cancellable = true)

    private void powergrid$configureWireTrackingRange(CallbackInfoReturnable<Integer> cir) {
        var serverConfig = ModdedConfigs.server();

        if (serverConfig == null)
            return;

        EntityType<?> type = (EntityType<?>) (Object) this;
        ResourceLocation typeId = EntityType.getKey(type);

        if (typeId == null || !PowerGrid.MOD_ID.equals(typeId.getNamespace())
                || !powergrid$WIRE_ENTITY_PATHS.contains(typeId.getPath()))
            return;

        cir.setReturnValue(serverConfig.electricity.wireTrackingRangeChunks.get());
    }
}
