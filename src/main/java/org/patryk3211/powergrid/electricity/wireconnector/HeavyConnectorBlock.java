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
package org.patryk3211.powergrid.electricity.wireconnector;

import net.minecraft.item.ItemStack;
import net.minecraft.util.shape.VoxelShapes;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class HeavyConnectorBlock extends AbstractConnectorBlock {
    private static final TerminalBoundingBox TERMINAL_DOWN = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 5, 0, 5, 11, 13, 11)
            .withOrigin(8, 12, 8);


    public HeavyConnectorBlock(Settings settings) {
        super(settings);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStates(state -> {
                    var terminal = switch(state.get(FACING)) {
                        case UP -> TERMINAL_DOWN.rotateAroundX(180);
                        case DOWN -> TERMINAL_DOWN;
                        case NORTH -> TERMINAL_DOWN.rotateAroundX(-90);
                        case SOUTH -> TERMINAL_DOWN.rotateAroundX(90);
                        case EAST -> TERMINAL_DOWN.rotateAroundX(90).rotateAroundY(-90);
                        case WEST -> TERMINAL_DOWN.rotateAroundX(90).rotateAroundY(90);
                    };
                    return new TerminalBoundingBox[] { terminal };
                })
                .withShapeMapper(state -> VoxelShapes.empty())
                .build()
        );
    }

    @Override
    public boolean accepts(ItemStack wireStack) {
        return true;
    }
}
