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
package org.patryk3211.powergrid.electricity.battery;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.patryk3211.powergrid.PowerGridRegistrate;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalPlacement;

public class BatteryBlock extends ElectricBlock implements IBE<BatteryBlockEntity> {
    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            Block.createCuboidShape(1, 0, 1, 15, 10, 15),
            Block.createCuboidShape(3, 10, 3, 6, 12, 6),
            Block.createCuboidShape(10, 10, 3, 13, 12, 6)
    );

    private static final TerminalPlacement TERMINAL_1 = new TerminalPlacement(4.5, 11.0, 4.5, 2.0);
    private static final TerminalPlacement TERMINAL_2 = new TerminalPlacement(11.5, 11.0, 4.5, 2.0);

    public BatteryBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE_NORTH;
    }

    @Override
    public Class<BatteryBlockEntity> getBlockEntityClass() {
        return BatteryBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BatteryBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.BATTERY.get();
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public TerminalPlacement terminal(BlockState state, int index) {
        return switch (index) {
            case 0 -> TERMINAL_1;
            case 1 -> TERMINAL_2;
            default -> null;
        };
    }
}
