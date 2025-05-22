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
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
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

    private static final VoxelShape COLLISION_SHAPE = createCuboidShape(0, 0, 0, 16, 14, 16);

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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(!state.get(OPEN))
            return VoxelShapes.fullCube();
        if(context instanceof EntityShapeContext entityContext && entityContext.getEntity() instanceof ItemEntity)
            return COLLISION_SHAPE;
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public void onEntityLand(BlockView world, Entity entity) {
        super.onEntityLand(world, entity);
        if(!world.getBlockState(entity.getBlockPos()).isOf(this))
            return;
        if(!(entity instanceof ItemEntity itemEntity))
            return;
        if(!entity.isAlive())
            return;

        withBlockEntityDo(world, entity.getBlockPos(), be -> {
            var inventory = be.getItemStorage(null);
            var stack = itemEntity.getStack().copy();

            try(var transaction = Transaction.openOuter()) {
                var inserted = inventory.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                transaction.commit();
                if(inserted == stack.getCount()) {
                    itemEntity.discard();
                    return;
                }

                stack.decrement((int) inserted);
                itemEntity.setStack(stack);
            }
        });
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return onBlockEntityUse(world, pos, be -> be.use(player, hand, hit));
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
