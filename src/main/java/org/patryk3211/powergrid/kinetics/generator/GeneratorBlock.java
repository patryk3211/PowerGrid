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
package org.patryk3211.powergrid.kinetics.generator;

import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class GeneratorBlock extends HorizontalKineticBlock implements IBE<GeneratorBlockEntity> {
    public GeneratorBlock(Settings settings) {
        super(settings);
    }

    public static BlockEntry<GeneratorBlock> register(final Registrate registrate) {
        return registrate.block("generator", GeneratorBlock::new)
                .transform(BlockStressDefaults.setImpact(4.0))
                .simpleItem()
                .register();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.get(Properties.HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return state.get(Properties.HORIZONTAL_FACING) == face;
    }

    @Override
    public Class<GeneratorBlockEntity> getBlockEntityClass() {
        return GeneratorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GeneratorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR.get();
    }
}
