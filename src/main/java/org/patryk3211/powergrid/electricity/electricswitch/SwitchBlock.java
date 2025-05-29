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

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireItem;

public abstract class SwitchBlock extends ElectricBlock implements IBE<SwitchBlockEntity> {
    public static final BooleanProperty OPEN = Properties.OPEN;

//    private static final TerminalBoundingBox NORTH_TERMINAL_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 1, 0, 10, 4, 2);
//    private static final TerminalBoundingBox NORTH_TERMINAL_2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 1, 14, 10, 4, 16);
//
//    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_180);
//    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_180);
//
//    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
//    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);
//
//    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
//    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
//
//    private static final VoxelShape SHAPE_NORTH_SOUTH = VoxelShapes.union(
//            createCuboidShape(3, 0, 2, 13, 7, 14),
//            NORTH_TERMINAL_1.getShape(),
//            NORTH_TERMINAL_2.getShape()
//    );
//    private static final VoxelShape SHAPE_EAST_WEST = VoxelShapes.union(
//            createCuboidShape(2, 0, 3, 14, 7, 13),
//            EAST_TERMINAL_1.getShape(),
//            EAST_TERMINAL_2.getShape()
//    );
    float resistance = 0.01f;
    float maxVoltage = 200f;

    public SwitchBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OPEN, true));
    }

    public static <B extends SwitchBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setResistance(float resistance) {
        return b -> {
            b.onRegister(block -> block.resistance = resistance);
            return b;
        };
    }

    public static <B extends SwitchBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setMaxVoltage(float voltage) {
        return b -> {
            b.onRegister(block -> block.maxVoltage = voltage);
            return b;
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPEN);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!player.isSneaking()) {
            if(!IWire.holdsWire(player)) {
                var isOpen = !state.get(OPEN);
                world.setBlockState(pos, state.with(OPEN, isOpen));
                if(world.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
                    entity.setState(!isOpen);
                }
                return ActionResult.SUCCESS;
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public Class<SwitchBlockEntity> getBlockEntityClass() {
        return SwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SwitchBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SWITCH.get();
    }

    public float getResistance() {
        return resistance;
    }

    public float getMaxVoltage() {
        return maxVoltage;
    }
}
