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

import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BlueprintOverlayRenderer.class)
public interface BlueprintOverlayRendererAccessor {
    @Accessor(value = "ingredients", remap = false)
    static List<Pair<ItemStack, Boolean>> getIngredients() {
        throw new AssertionError();
    }

    @Accessor(value = "active",remap = false)
    static boolean getActive() {
        throw new AssertionError();
    }

    @Accessor(value = "active", remap = false)
    static void setActive(boolean value) {
        throw new AssertionError();
    }

    @Accessor(value = "empty", remap = false)
    static void setEmpty(boolean value) {
        throw new AssertionError();
    }

    @Accessor(value = "noOutput", remap = false)
    static void setNoOutput(boolean value) {
        throw new AssertionError();
    }
}
