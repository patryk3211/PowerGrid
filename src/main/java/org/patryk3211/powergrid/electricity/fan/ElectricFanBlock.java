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
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

/**
 * @see com.simibubi.create.content.kinetics.fan.EncasedFanBlock
 */
public class ElectricFanBlock extends DirectionalElectricBlock implements IBE<ElectricFanBlockEntity>, IHaveElectricProperties {
    private static final TerminalBoundingBox[] UP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 5, 6, 7, 6, 7, 9).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 10, 6, 7, 11, 7, 9).withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_UP = Shapes.or(
            box(0, 10, 0, 16, 16, 16),
            box(5, 7, 5, 11, 10, 11)
    );

    public ElectricFanBlock(Properties settings) {
        super(settings.noOcclusion());

        var shaper = VoxelShaper.forDirectional(SHAPE_UP, Direction.UP);
        setTerminalCollection(BlockStateTerminalCollection
                .builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(UP_TERMINALS, terminal -> switch(state.getValue(FACING)) {
                    case UP -> terminal;
                    case DOWN -> terminal.rotateAroundX(180);
                    case NORTH -> terminal.rotateAroundX(90);
                    case SOUTH -> terminal.rotateAroundX(-90);
                    case EAST -> terminal.rotateAroundX(90).rotateAroundY(90);
                    case WEST -> terminal.rotateAroundX(90).rotateAroundY(-90);
                }))
                .withShapeMapper(state -> shaper.get(state.getValue(FACING)))
                .build());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var face = context.getClickedFace();

        var placedOn = world.getBlockState(pos.relative(face.getOpposite()));
        var placedOnOpposite = world.getBlockState(pos.relative(face));
        if(AbstractChuteBlock.isChute(placedOn))
            return defaultBlockState().setValue(FACING, face.getOpposite());
        if(AbstractChuteBlock.isChute(placedOnOpposite))
            return defaultBlockState().setValue(FACING, face);

        var preferredFacing = context.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown() ? preferredFacing : preferredFacing.getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, worldIn, pos, oldState, isMoving);
        blockUpdate(state, worldIn, pos);
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState stateIn, LevelAccessor worldIn, BlockPos pos, int flags, int count) {
        super.updateIndirectNeighbourShapes(stateIn, worldIn, pos, flags, count);
        blockUpdate(stateIn, worldIn, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        blockUpdate(state, worldIn, pos);
    }

    protected void blockUpdate(BlockState state, LevelAccessor worldIn, BlockPos pos) {
        if(worldIn instanceof WrappedLevel)
            return;
        notifyFanBlockEntity(worldIn, pos);
    }

    protected void notifyFanBlockEntity(LevelAccessor world, BlockPos pos) {
        withBlockEntityDo(world, pos, ElectricFanBlockEntity::blockInFrontChanged);
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        blockUpdate(newState, context.getLevel(), context.getClickedPos());
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

    public static float resistance() {
        return 20f;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }
}
