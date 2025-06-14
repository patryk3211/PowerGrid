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
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class LvSwitchBlock extends SurfaceSwitchBlock {
    private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 0, 1, 9, 2, 3),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 0, 13, 9, 2, 15)
    };

    private static final VoxelShape SHAPE_DOWN = createCuboidShape(4, 0, 3, 12, 3, 13);
    private static final VoxelShape SHAPE_DOWN_2 = createCuboidShape(3, 0, 4, 13, 3, 12);

    public LvSwitchBlock(Settings settings) {
        super(settings);
        this.maxVoltage = 120;
        this.resistance = 0.15f;

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
        world.playSound(null, pos, ModdedSoundEvents.LV_SWITCH_CLICK.getMainEvent(), SoundCategory.BLOCKS, 0.3F, open ? 0.65f : 0.75f);
    }
}
