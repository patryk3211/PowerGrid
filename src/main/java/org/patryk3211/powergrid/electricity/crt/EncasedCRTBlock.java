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
package org.patryk3211.powergrid.electricity.crt;

import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.function.Supplier;

import static org.patryk3211.powergrid.electricity.crt.CRTBlock.*;

public class EncasedCRTBlock extends HorizontalElectricBlock implements IBE<CRTBlockEntity>, EncasedBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 16, 14),
            box(0, 0, 14, 16, 1, 16),
            box(0, 1, 14, 3, 16, 16),
            box(13, 1, 14, 16, 16, 16),
            box(3, 11, 14, 13, 16, 16),
            box(5, 3, 14, 11, 9, 15)
    );

    public static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(CATHODE, 5, 5, 15, 6, 7, 16)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(HEATER, 10, 5, 15, 11, 7, 16)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(GRID, 7, 3, 15, 9, 4, 16),
            new TerminalBoundingBox(ANODE, 7, 7, 15, 9, 8, 16)
                    .withColor(IDecoratedTerminal.RED),

            new TerminalBoundingBox(X_COIL, 4, 2, 14, 5, 4, 15)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(Y_COIL, 11, 2, 14, 12, 4, 15)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(COMMON_COIL, 7, 9, 14, 9, 10, 15)
                    .withColor(IDecoratedTerminal.BLUE),
    };

    private final Supplier<Block> casing;

    public EncasedCRTBlock(Properties settings, Supplier<Block> casing) {
        super(settings);
        this.casing = casing;
        setTerminalCollection(horizontalNorthTerminals(this, TERMINALS, SHAPE));
    }

    @Override
    public Class<CRTBlockEntity> getBlockEntityClass() {
        return CRTBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CRTBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CRT.get();
    }

    @Override
    public Block getCasing() {
        return casing.get();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof DyeItem dye) {
            return onBlockEntityUse(level, pos, be -> be.setColor(dye.getDyeColor()));
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        var level = context.getLevel();
        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        var pos = context.getClickedPos();
        var newState = ModdedBlocks.CRT.getDefaultState()
                .setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING));
        level.setBlock(pos, newState, UPDATE_ALL);
        refreshConnectionEntities(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray) {
        if(level.isClientSide)
            return;
        BlockState encasedState = defaultBlockState()
                .setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING));
        level.setBlock(pos, encasedState, UPDATE_ALL);
        refreshConnectionEntities(level, pos);
    }
}
