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
package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.customdisplay.CustomDisplayBehaviour;

import java.util.List;

public class PowerGaugeBlock extends HorizontalElectricBlock implements IBE<PowerGaugeBlockEntity>, IGaugeBlock, IHaveElectricProperties {
    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.INPUT, 14, 13, 6, 16, 15, 10),
            new TerminalBoundingBox(IDecoratedTerminal.OUTPUT, 0, 13, 6, 2, 15, 10),
            new TerminalBoundingBox(IDecoratedTerminal.COMMON, 6, 13, 13, 10, 15, 15)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE = box(1, 0, 2, 15, 14, 14);

    public PowerGaugeBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, TERMINALS_NORTH, SHAPE));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(CustomDisplayBehaviour.use(level, pos, player, hand))
            return InteractionResult.SUCCESS;
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        var be = getBlockEntity(level, pos);
        if(be == null)
            return 0;
        return be.redstoneOutput;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public boolean shouldRenderHeadOnFace(Level world, BlockPos pos, BlockState state, Direction dir) {
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return dir.getAxis() == facing.getAxis();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ? ctx.getHorizontalDirection().getOpposite() : ctx.getHorizontalDirection();
        return defaultBlockState().setValue(HORIZONTAL_FACING, player);
    }

    @Override
    public Class<PowerGaugeBlockEntity> getBlockEntityClass() {
        return PowerGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PowerGaugeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.POWER_METER.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Current.max(stack, player, tooltip);
    }
}
