package org.patryk3211.powergrid.electricity.light.factorylight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FactoryLightLightBlock extends Block {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 1);

    public FactoryLightLightBlock(Properties properties) {
        super(properties
                .lightLevel(state -> state.getValue(POWER) == 1 ? 15 : 10)
                .noCollission()
                .noLootTable()
                .noOcclusion()
                .replaceable()
                .noParticlesOnBreak()
                .pushReaction(PushReaction.DESTROY));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    private static boolean isPoweredLight(BlockState state) {
        if(!(state.getBlock() instanceof FactoryLightBlock))
            return false;
        var level = state.getValue(FactoryLightBlock.POWER);
        return level >= 2;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if(direction == Direction.UP) {
            if(!neighborState.is(this) && !isPoweredLight(neighborState)) {
                return Blocks.AIR.defaultBlockState();
            } else if(neighborState.is(this)) {
                int power = neighborState.getValue(POWER);
                return state.setValue(POWER, power);
            } else if(neighborState.getBlock() instanceof FactoryLightBlock) {
                int power = neighborState.getValue(FactoryLightBlock.POWER);
                return state.setValue(POWER, power - 2);
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
