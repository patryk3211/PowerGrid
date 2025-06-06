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
package org.patryk3211.powergrid.electricity.febridge;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.SurfaceElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class FEBridgeBlock extends SurfaceElectricBlock implements IBE<FEBridgeBlockEntity> {
    private static final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 10, 3, 9, 14, 5).withOrigin(8, 13, 4),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 10, 11, 9, 14, 13).withOrigin(8, 13, 12)
    };

    private static final VoxelShape SHAPE_DOWN = VoxelShapes.union(
            createCuboidShape(2, 0, 2, 14, 2, 14),
            createCuboidShape(4, 2, 3, 12, 6, 13),
            createCuboidShape(6, 6, 5, 10, 11, 11)
    );

    private static final VoxelShape SHAPE_DOWN2 = VoxelShapes.union(
            createCuboidShape(2, 0, 2, 14, 2, 14),
            createCuboidShape(3, 2, 4, 13, 6, 12),
            createCuboidShape(5, 6, 6, 11, 11, 10)
    );

    public FEBridgeBlock(Settings settings) {
        super(settings);

        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(SHAPE_DOWN2, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS_DOWN, terminal -> {
                    var facing = state.get(FACING);
                    terminal = switch(facing) {
                        case DOWN -> terminal;
                        case UP -> terminal.rotateAroundX(180);
                        case EAST -> terminal.rotateAroundZ(-90);
                        case WEST -> terminal.rotateAroundZ(90);
                        case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                        case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                    };
                    if(!state.get(ALONG_FIRST_AXIS)) {
                        terminal = terminal.rotate(facing.getAxis(), 90);
                    }
                    return terminal;
                }))
                .withShapeMapper(state -> {
                    var facing = state.get(FACING);
                    var axis_along = state.get(ALONG_FIRST_AXIS);
                    var prov = (axis_along ^ facing.getAxis() == Direction.Axis.Y) ? shaper2 : shaper;
                    return prov.get(facing);
                })
                .build());
    }

    @Override
    public Class<FEBridgeBlockEntity> getBlockEntityClass() {
        return FEBridgeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FEBridgeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.FE_BRIDGE.get();
    }
}
