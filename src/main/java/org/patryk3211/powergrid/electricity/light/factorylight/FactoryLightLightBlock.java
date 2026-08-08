package org.patryk3211.powergrid.electricity.light.factorylight;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;

public class FactoryLightLightBlock extends Block implements IBE<FactoryLightLightBlockEntity> {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 1);

    public FactoryLightLightBlock(Properties properties) {
        super(properties
                .lightLevel(state -> state.getValue(POWER) == 1 ? ILightBulb.LIGHT_LEVEL_FULL_POWER : ILightBulb.LIGHT_LEVEL_LOW_POWER)
                .noCollission()
                .noLootTable()
                .noOcclusion()
                .replaceable()
                .noTerrainParticles()
                .pushReaction(PushReaction.DESTROY)
                .air());
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

    @Override
    public Class<FactoryLightLightBlockEntity> getBlockEntityClass() {
        return FactoryLightLightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FactoryLightLightBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.LIGHT_LIGHT.get();
    }
}
