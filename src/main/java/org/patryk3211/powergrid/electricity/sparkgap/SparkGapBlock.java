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
package org.patryk3211.powergrid.electricity.sparkgap;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.shape.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class SparkGapBlock extends HorizontalAxisElectricBlock implements IBE<SparkGapBlockEntity> {
    public static final VoxelShape SHAPE_NORTH = createCuboidShape(2, 0, 0, 14, 9, 16);

    public static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 9, 0.5, 10, 11, 2.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 9, 13.5, 10, 11, 15.5)
    };

    public SparkGapBlock(Settings settings) {
        super(settings);
        setTerminalCollection(horizontalZTerminals(this, TERMINALS_NORTH, SHAPE_NORTH));
    }

    @Override
    public Class<SparkGapBlockEntity> getBlockEntityClass() {
        return SparkGapBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SparkGapBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SPARK_GAP.get();
    }
}
