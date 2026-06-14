package org.patryk3211.powergrid.general.ceilingtile;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;

public class CeilingTileBlock extends ElectricBlock implements IBE<CeilingTileBlockEntity> {
    public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);

    private static final VoxelShape SHAPE_EMPTY = box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape SHAPE_LAMP = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(1, 2, 1, 15, 3, 15),
            box(2, 3, 2, 14, 9, 14)
    );

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 3, 9, 3, 6, 11, 6),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 10, 9, 10, 13, 11, 13)
    };

    public CeilingTileBlock(Properties properties) {
        super(properties.lightLevel(state -> switch(state.getValue(STATE)) {
            case LAMP_LOW_POWER -> ILightBulb.LIGHT_LEVEL_LOW_POWER;
            case LAMP_ON -> ILightBulb.LIGHT_LEVEL_FULL_POWER;
            default -> 0;
        }));
        registerDefaultState(defaultBlockState().setValue(STATE, State.EMPTY));
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> {
                    if(state.getValue(STATE) == State.EMPTY)
                        return new TerminalBoundingBox[2];
                    return TERMINALS;
                })
                .withShapeMapper(state -> state.getValue(STATE) == State.EMPTY ? SHAPE_EMPTY : SHAPE_LAMP)
                .build());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        var stack = player.getMainHandItem();
        if(state.getValue(STATE) == State.EMPTY) {
            if(!ModdedBlocks.FACTORY_LIGHT.is(stack.getItem()))
                return InteractionResult.PASS;
            if(!player.isCreative())
                stack.shrink(1);
            if(!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(STATE, State.LAMP));
                level.playSound(null, pos,
                        ModdedBlocks.FACTORY_LIGHT.getDefaultState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if(state.getValue(STATE) != State.EMPTY) {
            var player = context.getPlayer();
            if(player != null && !player.isCreative())
                player.addItem(ModdedBlocks.FACTORY_LIGHT.asStack());
            var level = context.getLevel();
            if(!level.isClientSide) {
                level.setBlockAndUpdate(context.getClickedPos(), state.setValue(STATE, State.EMPTY));
                IWrenchable.playRemoveSound(level, context.getClickedPos());
            }
            return InteractionResult.SUCCESS;
        }
        return super.onWrenched(state, context);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if(state.getBlock() == newState.getBlock() && state.getBlock() == this) {
            if(newState.getValue(STATE) == State.EMPTY) {
                var be = world.getBlockEntity(pos);
                if(be instanceof SmartBlockEntity sbe) {
                    sbe.destroy();
                }
                world.removeBlockEntity(pos);
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATE);
    }

    @Override
    public Class<CeilingTileBlockEntity> getBlockEntityClass() {
        return CeilingTileBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CeilingTileBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CEILING_TILE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if(state.getValue(STATE) == State.EMPTY)
            return null;
        return IBE.super.newBlockEntity(pos, state);
    }

    public enum State implements StringRepresentable {
        EMPTY,
        LAMP,
        LAMP_LOW_POWER,
        LAMP_ON;

        @Override
        public String getSerializedName() {
            return switch(this) {
                case EMPTY -> "empty";
                case LAMP -> "lamp";
                case LAMP_LOW_POWER -> "lamp_low_power";
                case LAMP_ON -> "lamp_on";
            };
        }
    }
}
