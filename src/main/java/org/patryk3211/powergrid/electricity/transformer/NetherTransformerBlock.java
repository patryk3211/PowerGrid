package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
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
        super(settings);
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
    public Class<NetherTransformerBlockEntity> getBlockEntityClass() {
        return NetherTransformerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NetherTransformerBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.NETHER_TRANSFORMER.get();
    }
}
