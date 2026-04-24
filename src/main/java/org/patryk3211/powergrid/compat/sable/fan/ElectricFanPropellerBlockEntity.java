package org.patryk3211.powergrid.compat.sable.fan;

import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlock;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlockEntity;

import static com.simibubi.create.content.kinetics.base.KineticBlockEntity.convertToAngular;

public class ElectricFanPropellerBlockEntity extends ElectricFanBlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller {
    private boolean blocked;

    public ElectricFanPropellerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Direction getBlockDirection() {
        return getBlockState().getValue(ElectricFanBlock.FACING);
    }

    @Override
    public double getAirflow() {
        return getPropSpeed() * 0.1;
    }

    @Override
    public double getThrust() {
        return getPropSpeed() * 0.3;
    }

    @Override
    public boolean isActive() {
        return !blocked && Math.abs(getPropSpeed()) > 0.01f;
    }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    protected float getPropSpeed() {
        float rotationSpeed = convertToAngular(this.getSpeed());
        return getBlockDirection().getAxisDirection().getStep() * rotationSpeed * 3.0f;
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        BlockPos front = getBlockPos().relative(getBlockDirection());
        blocked = !level.getBlockState(front).isAir();
    }
}
