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
package org.patryk3211.powergrid.electricity.fan;

import com.simibubi.create.content.logistics.chute.AbstractChuteBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

/**
 * @see com.simibubi.create.content.kinetics.fan.EncasedFanBlock
 */
public class ElectricFanBlock extends DirectionalElectricBlock implements IBE<ElectricFanBlockEntity> {
    private static final TerminalBoundingBox[] UP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 5, 6, 7, 6, 7, 9).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 10, 6, 7, 11, 7, 9).withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_UP = VoxelShapes.union(
            createCuboidShape(0, 10, 0, 16, 16, 16),
            createCuboidShape(5, 7, 5, 11, 10, 11)
    );

    public ElectricFanBlock(Settings settings) {
        super(settings.nonOpaque());

        var shaper = VoxelShaper.forDirectional(SHAPE_UP, Direction.UP);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(UP_TERMINALS, terminal -> switch(state.get(FACING)) {
                    case UP -> terminal;
                    case DOWN -> terminal.rotateAroundX(180);
                    case NORTH -> terminal.rotateAroundX(90);
                    case SOUTH -> terminal.rotateAroundX(-90);
                    case EAST -> terminal.rotateAroundX(90).rotateAroundY(90);
                    case WEST -> terminal.rotateAroundX(90).rotateAroundY(-90);
                }))
                .withShapeMapper(state -> shaper.get(state.get(FACING)))
                .build());
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        var world = context.getWorld();
        var pos = context.getBlockPos();
        var face = context.getSide();

        var placedOn = world.getBlockState(pos.offset(face.getOpposite()));
        var placedOnOpposite = world.getBlockState(pos.offset(face));
        if(AbstractChuteBlock.isChute(placedOn))
            return getDefaultState().with(FACING, face.getOpposite());
        if(AbstractChuteBlock.isChute(placedOnOpposite))
            return getDefaultState().with(FACING, face);

        var preferredFacing = context.getPlayerLookDirection();
        return getDefaultState().with(FACING, context.getPlayer() != null && context.getPlayer()
                .isSneaking() ? preferredFacing : preferredFacing.getOpposite());
    }

    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onBlockAdded(state, worldIn, pos, oldState, isMoving);
        blockUpdate(state, worldIn, pos);
    }

    @Override
    public void prepare(BlockState stateIn, WorldAccess worldIn, BlockPos pos, int flags, int count) {
        super.prepare(stateIn, worldIn, pos, flags, count);
        blockUpdate(stateIn, worldIn, pos);
    }

    @Override
    public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        blockUpdate(state, worldIn, pos);
    }

    protected void blockUpdate(BlockState state, WorldAccess worldIn, BlockPos pos) {
        if(worldIn instanceof WrappedWorld)
            return;
        notifyFanBlockEntity(worldIn, pos);
    }

    protected void notifyFanBlockEntity(WorldAccess world, BlockPos pos) {
        withBlockEntityDo(world, pos, ElectricFanBlockEntity::blockInFrontChanged);
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, ItemUsageContext context) {
        blockUpdate(newState, context.getWorld(), context.getBlockPos());
        return newState;
    }

    @Override
    public Class<ElectricFanBlockEntity> getBlockEntityClass() {
        return ElectricFanBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectricFanBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ELECTRIC_FAN.get();
    }
}
