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
package org.patryk3211.powergrid.electricity.resistor;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.SurfaceElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class AbstractResistorBlock extends SurfaceElectricBlock {
    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 7, 0, 10, 9, 2),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 7, 14, 10, 9, 16)
    };

    private static final VoxelShape SHAPE1 = Shapes.or(
            box(4, 0, 0, 12, 2, 16),
            box(5, 3, 3, 11, 9, 13),
            box(6, 2, 0, 10, 9, 3),
            box(6, 2, 13, 10, 9, 16)
    );

    private static final VoxelShape SHAPE2 = Shapes.or(
            box(0, 0, 4, 16, 2, 12),
            box(3, 3, 5, 13, 9, 11),
            box(0, 2, 6, 3, 9, 10),
            box(13, 2, 6, 16, 9, 10)
    );

    public AbstractResistorBlock(Properties settings) {
        super(settings);
        setTerminalCollection(surfaceTerminals(this, TERMINALS, SHAPE1, SHAPE2));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
        var state = super.getStateForPlacement(ctx);
        if(state == null)
            return null;
        return state.cycle(ALONG_FIRST_AXIS);
    }
}
