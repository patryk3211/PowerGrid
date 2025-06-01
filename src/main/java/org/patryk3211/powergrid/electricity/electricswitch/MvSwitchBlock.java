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
package org.patryk3211.powergrid.electricity.electricswitch;

import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class MvSwitchBlock extends SurfaceSwitchBlock {
    private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 0, 10, 3, 2),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 14, 10, 3, 16)
    };

    private static final VoxelShape SHAPE_DOWN = createCuboidShape(3, 0, 2, 13, 4, 14);
    private static final VoxelShape SHAPE_DOWN_2 = createCuboidShape(2, 0, 3, 14, 4, 13);

    public MvSwitchBlock(Settings settings) {
        super(settings);
        this.maxVoltage = 320;
        this.resistance = 0.05f;

        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(SHAPE_DOWN_2, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStatesExcept(state -> BlockStateTerminalCollection.each(DOWN_TERMINALS, terminal -> {
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
                }), OPEN)
                .withShapeMapper(state -> {
                    var facing = state.get(FACING);
                    var axis_along = state.get(ALONG_FIRST_AXIS);
                    var prov = (axis_along ^ facing.getAxis() == Direction.Axis.Y) ? shaper2 : shaper;
                    return prov.get(facing);
                })
                .build());
    }

    @Override
    public void useSound(World world, BlockPos pos, boolean open) {
        world.playSound(null, pos, ModdedSoundEvents.MV_SWITCH_CLICK.getMainEvent(), SoundCategory.BLOCKS, 0.3F, open ? 1.25f : 1.5f);
    }
}
