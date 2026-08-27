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

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class LvSwitchBlock extends SurfaceSwitchBlock {
    private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
            // (Common / Input)
            new TerminalBoundingBox(IDecoratedTerminal.INPUT, 6.5, 0, 2.5, 9.5, 2, 4.5),

            // (Output A)
            new TerminalBoundingBox(IDecoratedTerminal.OUTPUT, 5.5, 0, 11.5, 7.5, 2, 13.5),

            // (Output B)
            new TerminalBoundingBox(IDecoratedTerminal.OUTPUT, 8.5, 0, 11.5, 10.5, 2, 13.5)
    };

    private static final VoxelShape SHAPE_DOWN = Shapes.or(
            box(5.5, 0, 4.5, 10.5, 2, 11.5),
            box(5.5, 2, 5.5, 10.5, 5, 10.5)
    );

    public LvSwitchBlock(Properties settings) {
        super(settings);
        this.maxVoltage = 320;
        this.isSPDT = true;
        setTerminalCollection(switchDownTerminals(this, DOWN_TERMINALS, SHAPE_DOWN));
    }

    @Override
    public void useSound(Level world, BlockPos pos, boolean open) {
        world.playSound(null, pos, ModdedSoundEvents.LV_SWITCH_CLICK.getMainEvent(), SoundSource.BLOCKS, 0.3F, open ? 0.65f : 0.75f);
    }
}