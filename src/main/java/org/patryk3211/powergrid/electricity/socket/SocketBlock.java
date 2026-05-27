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
package org.patryk3211.powergrid.electricity.socket;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

@MethodsReturnNonnullByDefault
public class SocketBlock extends Rotation4ElectricBlock implements IBE<SocketBlockEntity>, ISocketElectric {
    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 3, 10, 2, 4)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 12, 10, 2, 13)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private final TerminalBoundingBox SOCKET_DOWN = new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 6, 3, 6, 10, 5, 10);
    private final TerminalBoundingBox SOCKET_UP = SOCKET_DOWN.rotateAroundX(180);
    private final TerminalBoundingBox SOCKET_NORTH = SOCKET_DOWN.rotateAroundX(-90);
    private final TerminalBoundingBox SOCKET_SOUTH = SOCKET_DOWN.rotateAroundX(90);
    private final TerminalBoundingBox SOCKET_EAST = SOCKET_DOWN.rotateAroundX(90).rotateAroundY(-90);
    private final TerminalBoundingBox SOCKET_WEST = SOCKET_DOWN.rotateAroundX(90).rotateAroundY(90);

    private static final VoxelShape SHAPE_DOWN = box(4, 0, 4, 12, 4, 12);

    public SocketBlock(Properties settings) {
        super(settings);
        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS_DOWN,
                        terminal -> {
                            var facing = state.getValue(FACING);
                            terminal = switch(facing) {
                                case DOWN -> terminal;
                                case UP -> terminal.rotateAroundX(180);
                                case EAST -> terminal.rotateAroundZ(-90);
                                case WEST -> terminal.rotateAroundZ(90);
                                case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                                case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                            };
                            var rotation = state.getValue(ROTATION);
                            terminal = terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                            return terminal;
                        })
                )
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    return shaper.get(facing);
                })
                .build());
    }

    @Override
    public Class<SocketBlockEntity> getBlockEntityClass() {
        return SocketBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SocketBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SOCKET.get();
    }

    @Override
    public ITerminalPlacement socket(BlockState state) {
        return switch(state.getValue(FACING)) {
            case DOWN -> SOCKET_DOWN;
            case UP -> SOCKET_UP;
            case NORTH -> SOCKET_NORTH;
            case SOUTH -> SOCKET_SOUTH;
            case EAST -> SOCKET_EAST;
            case WEST -> SOCKET_WEST;
        };
    }
}
