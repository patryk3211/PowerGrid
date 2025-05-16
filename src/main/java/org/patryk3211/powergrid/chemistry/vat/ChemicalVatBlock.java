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
package org.patryk3211.powergrid.chemistry.vat;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
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

public class ChemicalVatBlock extends Block implements IBE<ChemicalVatBlockEntity>, IWrenchable {
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final VoxelShape OPEN_SHAPE = VoxelShapes.union(
            createCuboidShape(0, 0, 0, 16, 2, 16),
            createCuboidShape(0, 2, 0, 2, 16, 16),
            createCuboidShape(14, 2, 0, 16, 16, 16),
            createCuboidShape(0, 2, 0, 14, 16, 2),
            createCuboidShape(0, 2, 14, 14, 16, 16)
    );

    public ChemicalVatBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OPEN, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPEN);
    }

    @Override
    public Class<ChemicalVatBlockEntity> getBlockEntityClass() {
        return ChemicalVatBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChemicalVatBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CHEMICAL_VAT.get();
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var world = ctx.getWorld();
        if(world.getBlockState(ctx.getBlockPos().offset(Direction.UP)).isOf(this))
            return getDefaultState().with(OPEN, false);
        return getDefaultState();
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if(pos.offset(Direction.UP).equals(sourcePos)) {
            if(world.getBlockState(sourcePos).isOf(this)) {
                if(state.get(OPEN)) {
                    world.setBlockState(pos, state.with(OPEN, false));
                }
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(!state.get(OPEN)) {
            return VoxelShapes.fullCube();
        } else {
            return OPEN_SHAPE;
        }
    }

    @Override
    public void onEntityLand(BlockView world, Entity entity) {
        super.onEntityLand(world, entity);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var stack = player.getStackInHand(hand);
        if(stack.isOf(Items.FLINT_AND_STEEL)) {
            if(!world.isClient) {
                var be = getBlockEntity(world, pos);
                if (be != null) {
                    if (!player.isCreative())
                        stack.damage(1, player, v -> {});
                    be.light();
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var pos = context.getBlockPos();
        var world = context.getWorld();
        if(world.getBlockState(pos.offset(Direction.UP)).isOf(this)) {
            return ActionResult.FAIL;
        }

        var newState = state.with(OPEN, !state.get(OPEN));
        newState = updateAfterWrenched(newState, context);
        world.setBlockState(pos, newState);
        return ActionResult.SUCCESS;
    }
}
