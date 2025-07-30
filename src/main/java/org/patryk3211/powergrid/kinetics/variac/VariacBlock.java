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
package org.patryk3211.powergrid.kinetics.variac;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlock;
import org.patryk3211.powergrid.utility.Lang;

public class VariacBlock extends ElectricKineticBlock implements IBE<VariacBlockEntity> {
    public static final DirectionProperty HORIZONTAL_FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            createCuboidShape(0, 0, 0, 16, 2, 16),
            createCuboidShape(2, 2, 2, 14, 10, 14),
            createCuboidShape(5, 10, 5, 11, 16, 11)
    );

    private static final Text TAP = Lang.builder()
            .translate("variac.tap")
            .style(Formatting.GRAY)
            .component();

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 10, 2, 0, 12, 4, 1),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 2, 0, 9, 4, 1),
            new TerminalBoundingBox(TAP, 4, 2, 0, 6, 4, 1)
    };

    public VariacBlock(Settings properties) {
        super(properties);
        setTerminalCollection(HorizontalElectricBlock.horizontalNorthTerminals(this, TERMINALS_NORTH, SHAPE_NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState()
                .with(HORIZONTAL_FACING, context.getHorizontalPlayerFacing()
                        .getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rot) {
        return state.with(HORIZONTAL_FACING, rot.rotate(state.get(HORIZONTAL_FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.get(HORIZONTAL_FACING)));
    }

    @Override
    public Class<VariacBlockEntity> getBlockEntityClass() {
        return VariacBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VariacBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.VARIAC.get();
    }
}
