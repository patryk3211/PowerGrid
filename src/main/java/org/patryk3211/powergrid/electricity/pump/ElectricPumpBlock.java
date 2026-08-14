package org.patryk3211.powergrid.electricity.pump;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;

import java.util.List;

/**
 * @see com.simibubi.create.content.fluids.pump.PumpBlock
 */
public class ElectricPumpBlock extends DirectionalElectricBlock implements IBE<ElectricPumpBlockEntity>, IAcceptConnector, IHaveElectricProperties {
    private static final VoxelShaper SHAPER = VoxelShaper.forDirectional(Shapes.or(
            box(2, 2, 0, 14, 14, 3),
            box(0, 0, 3, 16, 16, 13),
            box(2, 2, 13, 14, 14, 16)
    ), Direction.NORTH);

    public ElectricPumpBlock(Properties settings) {
        super(settings);
    }

    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, otherBlock, neighborPos, isMoving);
        Direction dir = FluidPropagator.validateNeighbourChange(state, world, pos, otherBlock, neighborPos, isMoving);
        if(dir != null) {
            if(isOpenAt(state, dir)) {
                world.scheduleTick(pos, this, 1, TickPriority.HIGH);
            }
        }
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState toPlace = super.getStateForPlacement(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean isShiftKeyDown = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        Direction nearestLookingDirection = context.getNearestLookingDirection();
        Direction targetDirection = isShiftKeyDown ? nearestLookingDirection : nearestLookingDirection.getOpposite();
        Direction bestConnectedDirection = null;
        double bestDistance = Double.MAX_VALUE;

        for(Direction d : Iterate.directions) {
            BlockPos adjPos = pos.relative(d);
            BlockState adjState = level.getBlockState(adjPos);
            if (FluidPipeBlock.canConnectTo(level, adjPos, adjState, d)) {
                double distance = Vec3.atLowerCornerOf(d.getNormal()).distanceTo(Vec3.atLowerCornerOf(targetDirection.getNormal()));
                if (!(distance > bestDistance)) {
                    bestDistance = distance;
                    bestConnectedDirection = d;
                }
            }
        }

        if (bestConnectedDirection != null && bestConnectedDirection.getAxis() != targetDirection.getAxis() && !isShiftKeyDown) {
            return toPlace.setValue(FACING, bestConnectedDirection);
        } else {
            return toPlace;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPER.get(state.getValue(FACING));
    }

    public static boolean isPump(BlockState state) {
        return state.getBlock() instanceof ElectricPumpBlock;
    }

    public static boolean isOpenAt(BlockState state, Direction d) {
        return d.getAxis() == state.getValue(FACING).getAxis();
    }

    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if(world.isClientSide)
            return;
        if(state != oldState)
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);

        if(isPump(state) && isPump(oldState) && state.getValue(FACING) == oldState.getValue(FACING).getOpposite()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if(!(blockEntity instanceof ElectricPumpBlockEntity pump))
                return;

            pump.pressureUpdate = true;
        }
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
        FluidPropagator.propagateChangedPipe(world, pos, state);
    }

    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        boolean blockTypeChanged = !state.is(newState.getBlock());
        if (blockTypeChanged && !world.isClientSide) {
            FluidPropagator.propagateChangedPipe(world, pos, state);
        }

        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public Class<ElectricPumpBlockEntity> getBlockEntityClass() {
        return ElectricPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectricPumpBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ELECTRIC_PUMP.get();
    }

    @Override
    public boolean canConnect(LevelReader world, BlockPos pos, BlockState state, Direction side) {
        return state.getValue(FACING).getAxis() != side.getAxis();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        float power = 256 / ModdedConfigs.server().electricity.electricPumpPower.getF();
        Voltage.max((int) Math.round(Math.sqrt(power * resistance())), player, tooltip);
    }
}
