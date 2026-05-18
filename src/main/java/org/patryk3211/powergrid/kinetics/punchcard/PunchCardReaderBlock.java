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
package org.patryk3211.powergrid.kinetics.punchcard;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlock;
import org.patryk3211.powergrid.utility.Lang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PunchCardReaderBlock extends ElectricKineticBlock implements IBE<PunchCardReaderBlockEntity>, IHaveElectricProperties {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static Component bit(int i) {
        return Lang.builder()
                .text(Integer.toString(i))
                .style(ChatFormatting.DARK_GREEN)
                .component();
    }

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.COMMON, 7, 12, 11, 9, 14, 13),

            new TerminalBoundingBox(bit(1), 4, 11.5, 12, 6, 13.5, 13.5),
            new TerminalBoundingBox(bit(2), 4, 8.75, 13, 6, 10.75, 14.5),
            new TerminalBoundingBox(bit(3), 4, 6, 14, 6, 8, 15.5),
            new TerminalBoundingBox(bit(4), 4, 3, 15, 6, 5, 16.5),

            new TerminalBoundingBox(bit(5), 10, 3, 15, 12, 5, 16.5),
            new TerminalBoundingBox(bit(6), 10, 6, 14, 12, 8, 15.5),
            new TerminalBoundingBox(bit(7), 10, 8.75, 13, 12, 10.75, 14.5),
            new TerminalBoundingBox(bit(8), 10, 11.5, 12, 12, 13.5, 13.5),
    };

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 17, 12),
            box(0, 0, 12, 16, 5, 16)
    );

    public PunchCardReaderBlock(Properties properties) {
        super(properties);
        setTerminalCollection(HorizontalElectricBlock.horizontalNorthTerminals(this, TERMINALS, SHAPE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ? ctx.getHorizontalDirection().getOpposite() : ctx.getHorizontalDirection();
        return defaultBlockState().setValue(HORIZONTAL_FACING, player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public Class<PunchCardReaderBlockEntity> getBlockEntityClass() {
        return PunchCardReaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PunchCardReaderBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.PUNCH_CARD_READER.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.switchResistance(resistance(), player, tooltip);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if(!state.is(newState.getBlock())) {
            var be = getBlockEntity(level, pos);
            if(be != null) {
                be.dropItems();
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos)
                .map(PunchCardReaderBlockEntity::getRedstoneOutput)
                .orElse(0);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUse(level, pos, be -> {
            var side = hit.getDirection();
            if(side != Direction.UP && side != state.getValue(HORIZONTAL_FACING))
                return InteractionResult.PASS;
            var stack = player.getItemInHand(hand);
            if(stack.isEmpty()) {
                var extracted = be.extractCard();
                player.setItemInHand(hand, extracted);
                return InteractionResult.SUCCESS;
            } else if(stack.getItem() instanceof PunchCardItem) {
                if(be.insertCard(stack, side)) {
                    stack.shrink(1);
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.FAIL;
                }
            } else {
                return InteractionResult.FAIL;
            }
        });
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING)));
    }
}
