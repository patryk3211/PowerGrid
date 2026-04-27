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
package org.patryk3211.powergrid.utility.proxy;

import com.tterrag.registrate.builders.BlockEntityBuilder;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SubstituteBlockEntityProvider extends SubstituteProvider<BlockEntityBuilder.BlockEntityFactory<?>> {
    public static final SubstituteBlockEntityProvider INSTANCE = new SubstituteBlockEntityProvider();

    private SubstituteBlockEntityProvider() { }

    public <T extends BlockEntity> void register(Class<T> clazz, BlockEntityBuilder.BlockEntityFactory<T> value) {
        super.register(clazz, value);
    }

    public <T extends BlockEntity> void registerDefault(Class<T> clazz, BlockEntityBuilder.BlockEntityFactory<T> value) {
        super.registerDefault(clazz, value);
    }

    public <T extends BlockEntity> BlockEntityBuilder.BlockEntityFactory<T> get(Class<T> clazz) {
        return (BlockEntityBuilder.BlockEntityFactory<T>) super.getObject(clazz);
    }
}
