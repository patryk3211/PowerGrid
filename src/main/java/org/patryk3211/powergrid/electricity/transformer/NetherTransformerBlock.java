package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.content.trains.CubeParticleData;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;

public class NetherTransformerBlock extends ElectricBlock implements IAcceptConnector, IBE<NetherTransformerBlockEntity> {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

    public static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 1, 14, 6, 5, 16, 10),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 14, 6, 15, 16, 10)
    };

    public static final VoxelShape SHAPE_BOTTOM = box(2, 0, 0, 14, 16, 16);
    public static final VoxelShape SHAPE_TOP = Shapes.or(
            box(2, 0, 0, 14, 12, 16),
            box(1, 10, 6, 5, 14, 10),
            box(11, 10, 6, 15, 14, 10)
    );

    public NetherTransformerBlock(Properties settings) {
        super(settings.lightLevel(state -> 9));
        var shaperBot = VoxelShaper.forHorizontalAxis(SHAPE_BOTTOM, Direction.Axis.Z);
        var shaperTop = VoxelShaper.forHorizontalAxis(SHAPE_TOP, Direction.Axis.Z);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> {
                    if(state.getValue(PART) >= 2) {
                        return BlockStateTerminalCollection.each(TERMINALS, terminal -> switch (state.getValue(HORIZONTAL_AXIS)) {
                            case Z -> terminal;
                            case X -> terminal.rotateAroundY(90);
                            default -> throw new IllegalStateException();
                        });
                    } else {
                        return new TerminalBoundingBox[2];
                    }
                })
                .withShapeMapper(state -> {
                    if(state.getValue(PART) >= 2) {
                        return shaperTop.get(state.getValue(HORIZONTAL_AXIS));
                    } else {
                        return shaperBot.get(state.getValue(HORIZONTAL_AXIS));
                    }
                })
                .build());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_AXIS, PART);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        if(state.getValue(PART) >= 2)
            return RenderShape.INVISIBLE;
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if(state.getValue(PART) >= 2)
            return;
        Vec3 v = Vec3.atLowerCornerOf(pos).subtract(0.125, 0, 0.125);
        if(state.getValue(PART) == 1) {
            v = v.relative(Direction.fromAxisAndDirection(state.getValue(HORIZONTAL_AXIS), Direction.AxisDirection.NEGATIVE), 1);
        } else {
            v = v.relative(Direction.fromAxisAndDirection(state.getValue(HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE), 1);
        }
        CubeParticleData data = new CubeParticleData(1.0F, random.nextFloat(), 1.0F, 0.0125F + 0.0625F * random.nextFloat(), 30, false);
        level.addParticle(data,
                v.x + random.nextFloat() * 1.25, v.y + 0.5, v.z + random.nextFloat() * 1.25,
                0.0f, 0.04, 0.0f);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int y = switch(state.getValue(PART)) {
            case 0, 1 -> 1;
            case 2, 3 -> -1;
            default -> throw new IllegalStateException();
        };
        int x = switch(state.getValue(PART)) {
            case 0, 2 -> 1;
            case 1, 3 -> -1;
            default -> throw new IllegalStateException();
        };
        var axis = state.getValue(HORIZONTAL_AXIS);
        var offsetPos = pos.relative(axis, x);
        if(!updateShapeVerifyPart(0, y, pos, neighborPos, state, level))
            return Blocks.AIR.defaultBlockState();
        var portalState = level.getBlockState(offsetPos);
        if(!portalState.is(Blocks.NETHER_PORTAL))
            return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ModdedBlocks.TRANSFORMER_CORE.asStack();
    }

    @Override
    public Class<NetherTransformerBlockEntity> getBlockEntityClass() {
        return NetherTransformerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NetherTransformerBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.NETHER_TRANSFORMER.get();
    }

    public static boolean updateShapeVerifyPart(int offsetX, int offsetY, BlockPos pos, BlockPos neighborPos, BlockState state, LevelAccessor level) {
        var axis = state.getValue(HORIZONTAL_AXIS);
        var offsetPos = pos.relative(axis, offsetX).relative(Direction.Axis.Y, offsetY);
        if (!neighborPos.equals(offsetPos))
            return true; // Ignore
        var offsetState = level.getBlockState(offsetPos);
        if (!offsetState.is(state.getBlock()))
            return false;
        int part = state.getValue(PART);
        int expectPart = part;
        if (offsetX > 0)
            expectPart |= 1;
        else if (offsetX < 0)
            expectPart &= ~1;
        if (offsetY > 0)
            expectPart |= 2;
        else if (offsetY < 0)
            expectPart &= ~2;
        return offsetState.getValue(HORIZONTAL_AXIS) == axis && offsetState.getValue(PART) == expectPart;
    }

}
