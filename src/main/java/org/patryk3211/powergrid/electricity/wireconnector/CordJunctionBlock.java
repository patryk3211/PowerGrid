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
package org.patryk3211.powergrid.electricity.wireconnector;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;

public class CordJunctionBlock extends ElectricBlock implements IBE<CordJunctionBlockEntity>, IAcceptCord {
    private static final VoxelShaper SHAPER = VoxelShaper.forDirectional(box(4, 0, 4, 12, 3, 12), Direction.DOWN);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public CordJunctionBlock(Properties settings) {
        super(settings);
    }

    @Override
    public Class<CordJunctionBlockEntity> getBlockEntityClass() {
        return CordJunctionBlockEntity.class;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    public BlockEntityType<? extends CordJunctionBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CORD_JUNCTION.get();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPER.get(state.getValue(FACING));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(FACING);
        return canSupportCenter(world, pos.relative(facing), facing.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING) && !canSurvive(state, world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    private static double pickPoint(double location, double center) {
        var diff = location - center;
        if(diff >= 0.125)
            return 0.25;
        if(diff <= -0.125)
            return -0.25;
        return 0;
    }

    @Override
    public @Nullable AutoCordEndpoint getEndpoint(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var facing = state.getValue(FACING);

        var center = Vec3.atCenterOf(pos);
        var normal = facing.getNormal();
        var point = center.add(normal.getX() * 0.40625, normal.getY() * 0.40625, normal.getZ() * 0.40625);

        var loc = context.getClickLocation();
        var offset = new Vec3(
                pickPoint(loc.x, center.x),
                pickPoint(loc.y, center.y),
                pickPoint(loc.z, center.z)
        );
        switch(facing.getAxis()) {
            case X -> {
                if(offset.y == 0 && offset.z == 0)
                    point = point.add(0.0625, 0, 0);
                else
                    point = point.add(0, offset.y, offset.z);
            }
            case Y -> {
                if(offset.x == 0 && offset.z == 0)
                    point = point.add(0, 0.0625, 0);
                else
                    point = point.add(offset.x, 0, offset.z);
            }
            case Z -> {
                if(offset.x == 0 && offset.y == 0)
                    point = point.add(0, 0, 0.0625);
                else
                    point = point.add(offset.x, offset.y, 0);
            }
        }
        return new AutoCordEndpoint(pos, 0, 1, point, null);
    }
}
