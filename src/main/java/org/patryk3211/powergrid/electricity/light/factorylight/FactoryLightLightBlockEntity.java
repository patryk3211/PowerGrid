package org.patryk3211.powergrid.electricity.light.factorylight;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static org.patryk3211.powergrid.electricity.light.factorylight.FactoryLightBlockEntity.projectionDistance;

public class FactoryLightLightBlockEntity extends SmartBlockEntity {

    public FactoryLightLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        setLazyTickRate(10);
    }
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void lazyTick() {
        boolean hit = false;
        for (int y = 0; y < projectionDistance(); y++) {
            BlockPos pos = new BlockPos(worldPosition.getX(), worldPosition.getY() + y, worldPosition.getZ());
            var state = level.getBlockEntity(pos);
            if ((state instanceof FactoryLightBlockEntity be)) {
                if (be.getPowerLevel() > 0){
                    hit = true;
                    break;
                }
            }
        }
        if (!hit) {
            onDelete();
        }
        super.lazyTick();
    }

    public void onDelete(){
        if (level != null) {
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
