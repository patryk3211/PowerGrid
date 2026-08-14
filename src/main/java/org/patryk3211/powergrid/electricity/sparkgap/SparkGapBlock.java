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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class SparkGapBlock extends HorizontalAxisElectricBlock implements IBE<SparkGapBlockEntity> {
    public static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(3, 0, 2, 13, 3, 14),
            box(5, 3, 2.5, 11, 9, 5.5),
            box(5, 3, 10.5, 11, 9, 13.5)
    );

    public static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 4.5, -0.5, 9.5, 7.5, 2.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 4.5, 13.5, 9.5, 7.5, 16.5)
    };

    public SparkGapBlock(Properties settings) {
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
