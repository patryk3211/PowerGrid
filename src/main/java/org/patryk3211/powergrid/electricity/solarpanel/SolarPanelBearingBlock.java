package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlock;

public class SolarPanelBearingBlock extends ElectricKineticBlock implements IBE<SolarPanelBearingBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 9, 13, 10, 12, 16, 13)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 4, 13, 10, 7, 16, 13)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    public static final VoxelShape SHAPE = Shapes.or(
            box(2, 2, 4, 14, 14, 16),
            box(1, 1, 0, 15, 15, 4)
    );
    public static final VoxelShape SHAPE2 = Shapes.or(
            box(2, 0, 2, 14, 12, 14),
            box(1, 12, 1, 15, 16, 15)
    );

    public SolarPanelBearingBlock(Properties properties) {
        super(properties);
        var shaper = VoxelShaper.forDirectional(SHAPE, Direction.NORTH);
        shaper.withVerticalShapes(SHAPE2);
        var terminalstate = BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS, terminal -> switch(state.getValue(FACING)) {
                    case NORTH -> terminal;
                    case SOUTH -> terminal.rotateAroundY(180);
                    case EAST -> terminal.rotateAroundY(90);
                    case WEST -> terminal.rotateAroundY(-90);
                    case UP -> terminal.rotateAroundX(-90);
                    case DOWN -> terminal.rotateAroundX(90);
                }))
                .withShapeMapper(state -> shaper.get(state.getValue(FACING)))
                .build();
        setTerminalCollection(terminalstate);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide)
                return InteractionResult.SUCCESS;
            withBlockEntityDo(worldIn, pos, be -> {
                if (be.running) {
                    be.disassemble();
                    return;
                }
                be.assembleNextTick = true;
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<SolarPanelBearingBlockEntity> getBlockEntityClass() {
        return SolarPanelBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SolarPanelBearingBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SOLAR_PANEL_BEARING.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    public Direction getPreferredFacing(BlockPlaceContext context) {
        Direction prefferedSide = null;
        for (Direction side : Iterate.directions) {
            BlockState blockState = context.getLevel()
                    .getBlockState(context.getClickedPos()
                    .relative(side));
            if (blockState.getBlock() instanceof IRotate) {
                if (((IRotate) blockState.getBlock()).hasShaftTowards(context.getLevel(), context.getClickedPos()
                        .relative(side), blockState, side.getOpposite()))
                    if (prefferedSide != null && prefferedSide.getAxis() != side.getAxis()) {
                        prefferedSide = null;
                        break;
                    } else {
                        prefferedSide = side;
                    }
            }
        }
        return prefferedSide == null ? null : prefferedSide.getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if (preferred == null || (context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown())) {
            Direction nearestLookingDirection = context.getHorizontalDirection();
            return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                    .isShiftKeyDown() ? nearestLookingDirection : nearestLookingDirection.getOpposite());
        }
        return defaultBlockState().setValue(FACING, preferred.getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace().getAxis() != Direction.Axis.Y || context.getClickedFace().getAxis() != Direction.Axis.Y) {
            return InteractionResult.PASS;
        }
        var be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (be instanceof SolarPanelBearingBlockEntity blockEntity) {
            if (!context.getLevel().isClientSide) {
                blockEntity.disassemble();
            }
            blockEntity.getPlacedBlockRotation();
        }
        return super.onWrenched(state, context);
    }
}
