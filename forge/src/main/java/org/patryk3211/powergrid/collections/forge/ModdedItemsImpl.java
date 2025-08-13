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
package org.patryk3211.powergrid.collections.forge;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.world.item.Item;
import org.patryk3211.powergrid.mixin.forge.ItemAccessor;

import java.util.function.Supplier;

public class ModdedItemsImpl {
    public static <T extends Item, P> NonNullUnaryOperator<ItemBuilder<T, P>> customRenderer(Supplier<Supplier<CustomRenderedItemModelRenderer>> renderer) {
        return b -> b.onRegister(item ->
            EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                ((ItemAccessor) item).setRenderProperties(SimpleCustomRenderer.create(item, renderer.get().get()))
            )
        );
    }
}
