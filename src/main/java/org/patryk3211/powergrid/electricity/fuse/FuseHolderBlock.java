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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.SurfaceElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class FuseHolderBlock extends SurfaceElectricBlock implements IBE<FuseHolderBlockEntity> {
    public static final EnumProperty<FuseState> STATE = EnumProperty.of("state", FuseState.class);

    private static final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 0, 0, 9.5, 3, 1),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 0, 15, 9.5, 3, 16)
    };

    private static final VoxelShape SHAPE_DOWN_1 = VoxelShapes.union(
            createCuboidShape(4, 0, 1, 12, 6, 15),
            createCuboidShape(4, 6, 4, 12, 8, 12)
    );
    private static final VoxelShape SHAPE_DOWN_2 = VoxelShapes.union(
            createCuboidShape(1, 0, 4, 15, 6, 12),
            createCuboidShape(4, 6, 4, 12, 8, 12)
    );

    public FuseHolderBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(STATE, FuseState.OPEN));
        setTerminalCollection(surfaceTerminals(this, TERMINALS_DOWN, SHAPE_DOWN_1, SHAPE_DOWN_2, STATE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(STATE);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return onBlockEntityUse(world, pos, be -> {
            var stack = player.getStackInHand(hand);
            if(stack.isIn(ModdedTags.Item.FUSE_RESETTING.tag)) {
                if(be.resetFuse()) {
                    if(!player.isCreative())
                        stack.decrement(1);
                    return ActionResult.SUCCESS;
                }
            } else if(stack.isEmpty()) {
                if(be.removeBlown())
                    return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
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
