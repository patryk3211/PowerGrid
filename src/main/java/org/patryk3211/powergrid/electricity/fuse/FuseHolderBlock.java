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
package org.patryk3211.powergrid.electricity.fuse;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.SurfaceElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FuseHolderBlock extends SurfaceElectricBlock implements IBE<FuseHolderBlockEntity> {
    public static final EnumProperty<FuseState> STATE = EnumProperty.create("state", FuseState.class);

    private static final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 1, 2.5, 9, 3, 3.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 1, 12.5, 9, 3, 13.5)
    };

    private static final VoxelShape SHAPE_DOWN_1 = box(6.5, 0, 3.5, 9.5, 4, 12.5);
    private static final VoxelShape SHAPE_DOWN_2 = box(3.5, 0, 6.5, 12.5, 4, 9.5);

    public FuseHolderBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(STATE, FuseState.OPEN));
        setTerminalCollection(surfaceTerminals(this, TERMINALS_DOWN, SHAPE_DOWN_1, SHAPE_DOWN_2, STATE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATE);
    }

    @NotNull
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUse(world, pos, be -> {
            var stack = player.getItemInHand(hand);
            if(stack.is(ModdedTags.Item.FUSE_RESETTING.tag)) {
                if(be.resetFuse()) {
                    if(!player.isCreative())
                        stack.shrink(1);
                    return InteractionResult.SUCCESS;
                }
            } else if(stack.isEmpty()) {
                if(be.removeBlown())
                    return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        });
    }

    @Override
    public Class<FuseHolderBlockEntity> getBlockEntityClass() {
        return FuseHolderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FuseHolderBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.FUSE_HOLDER.get();
    }
}
