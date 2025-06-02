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
package org.patryk3211.powergrid.electricity.electromagnet;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class ElectromagnetBlock extends ElectricBlock implements IBE<ElectromagnetBlockEntity> {
    public static final DirectionProperty FACING = Properties.FACING;

    private static final TerminalBoundingBox[] DOWN_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 3, 14, 3, 6, 16, 6).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 10, 14, 10, 13, 16, 13).withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape DOWN_SHAPE = createCuboidShape(0, 2, 0, 16, 14, 16);

    public ElectromagnetBlock(Settings settings) {
        super(settings);

        var shaper = VoxelShaper.forDirectional(DOWN_SHAPE, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(DOWN_TERMINALS, terminal -> {
                    var facing = state.get(FACING);
                    terminal = switch(facing) {
                        case DOWN -> terminal;
                        case UP -> terminal.rotateAroundX(180);
                        case EAST -> terminal.rotateAroundX(90).rotateAroundY(-90);
                        case WEST -> terminal.rotateAroundX(90).rotateAroundY(90);
                        case NORTH -> terminal.rotateAroundX(-90);
                        case SOUTH -> terminal.rotateAroundX(90);
                    };
                    return terminal;
                }))
                .withShapeMapper(state -> shaper.get(state.get(FACING)))
                .build());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getPlayerLookDirection();
        if(ctx.getPlayer() != null && ctx.getPlayer().isSneaking())
            facing = facing.getOpposite();
        return getDefaultState().with(FACING, facing);
    }

    @Override
    public Class<ElectromagnetBlockEntity> getBlockEntityClass() {
        return ElectromagnetBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectromagnetBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ELECTROMAGNET.get();
    }

    public static float resistance() {
        return 2.5f;
    }
}
