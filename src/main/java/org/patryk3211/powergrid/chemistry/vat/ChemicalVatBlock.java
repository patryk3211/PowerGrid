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
import net.minecraft.util.function.BooleanBiFunction;
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

    public static final float CORNER = 2 / 16f;
    public static final float SIDE = 12 / 16f;
    public static final float FLUID_SPAN = 13 / 16f;

    private static final VoxelShape OPEN_NORTH = VoxelShapes.cuboid(CORNER, CORNER, 0.0f, CORNER + SIDE, 1.0f, CORNER);
    private static final VoxelShape OPEN_SOUTH = VoxelShapes.cuboid(CORNER, CORNER, CORNER + SIDE, CORNER + SIDE, 1.0f, 1.0f);
    private static final VoxelShape OPEN_WEST = VoxelShapes.cuboid(0.0f, CORNER, CORNER, CORNER, 1.0f, CORNER + SIDE);
    private static final VoxelShape OPEN_EAST = VoxelShapes.cuboid(CORNER + SIDE, CORNER, CORNER, 1.0f, 1.0f, CORNER + SIDE);

    private static final VoxelShape OPEN_NW = VoxelShapes.cuboid(0.0f, CORNER, 0.0f, CORNER, 1.0f, CORNER);
    private static final VoxelShape OPEN_SW = VoxelShapes.cuboid(0.0f, CORNER, CORNER + SIDE, CORNER, 1.0f, 1.0f);
    private static final VoxelShape OPEN_NE = VoxelShapes.cuboid(CORNER + SIDE, CORNER, 0.0f, 1.0f, 1.0f, CORNER);
    private static final VoxelShape OPEN_SE = VoxelShapes.cuboid(CORNER + SIDE, CORNER, CORNER + SIDE, 1.0f, 1.0f, 1.0f);

    private static final VoxelShape[] shapeLookup = new VoxelShape[] {
            OPEN_NW, OPEN_NORTH, OPEN_NE,
            OPEN_WEST, null, OPEN_EAST,
            OPEN_SW, OPEN_SOUTH, OPEN_SE
    };

    private static final VoxelShape BOTTOM = VoxelShapes.cuboid(0.0f, 0.0f, 0.0f, 1.0f, CORNER, 1.0f);

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

    public static boolean checkState(Block block, BlockState state) {
        return !(state.isOf(block) && state.get(ChemicalVatBlock.OPEN));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(!state.get(OPEN)) {
            return VoxelShapes.fullCube();
        } else {
            VoxelShape shape = BOTTOM;

            for(int x = -1; x <= 1; ++x) {
                for(int z = -1; z <= 1; ++z) {
                    if(x == 0 && z == 0)
                        continue;
                    int index = x + 1 + (z + 1) * 3;
                    if(x == 0 || z == 0) {
                        var neighbor = world.getBlockState(pos.add(x, 0, z));
                        if(checkState(state.getBlock(), neighbor)) {
                            shape = VoxelShapes.combine(shape, shapeLookup[index], BooleanBiFunction.OR);
                        }
                    } else {
                        var neighbor1 = world.getBlockState(pos.add(x, 0, 0));
                        var neighbor2 = world.getBlockState(pos.add(0, 0, z));
                        var corner = world.getBlockState(pos.add(x, 0, z));

                        var block = state.getBlock();
                        if(checkState(block, neighbor1) || checkState(block, neighbor2) || checkState(block, corner)) {
                            shape = VoxelShapes.combine(shape, shapeLookup[index], BooleanBiFunction.OR);
                        }
                    }
                }
            }

            return shape;
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
