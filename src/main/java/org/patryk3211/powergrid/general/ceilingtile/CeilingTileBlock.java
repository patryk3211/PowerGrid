package org.patryk3211.powergrid.general.ceilingtile;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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

import java.util.List;
import java.util.function.Predicate;

public class CeilingTileBlock extends ElectricBlock implements IBE<CeilingTileBlockEntity> {
    public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);
    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    private static final VoxelShape SHAPE_EMPTY = box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape SHAPE_SOLAR_PANEL = box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape SHAPE_LAMP = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(1, 2, 1, 15, 3, 15),
            box(2, 3, 2, 14, 9, 14)
    );
    private static final VoxelShape SHAPE_WIRE_CONNECTOR = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(5, 2, 5, 11, 5, 11),
            box(6, 5, 6, 10, 12,10)
    );
    private static final VoxelShape SHAPE_CORD_JUNCTION = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(4, 2, 4, 12, 5, 12)
    );

    private static final TerminalBoundingBox[] LAMP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 3, 9, 3, 6, 11, 6),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 10, 9, 10, 13, 11, 13)
    };
    private static final TerminalBoundingBox[] WIRE_CONNECTOR_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 5, 6, 10, 12,10),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 6, 7, 8, 7, 8) //second one hidden
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
                    switch(state.getValue(STATE)) {
                        case LAMP, LAMP_LOW_POWER, LAMP_ON -> {return LAMP_TERMINALS;}
                        case WIRE_CONNECTOR -> {return WIRE_CONNECTOR_TERMINALS;}
                        case CORD_JUNCTION -> {return new TerminalBoundingBox[2];}
                        case SOLAR_PANEL -> {return new TerminalBoundingBox[2];}
                        default -> {return new TerminalBoundingBox[2];}
                    }
                })
                .withShapeMapper(state -> {
                    switch(state.getValue(STATE)) {
                        case LAMP, LAMP_LOW_POWER, LAMP_ON -> {return SHAPE_LAMP;}
                        case WIRE_CONNECTOR -> {return SHAPE_WIRE_CONNECTOR;}
                        case CORD_JUNCTION -> {return SHAPE_CORD_JUNCTION;}
                        case SOLAR_PANEL -> {return SHAPE_SOLAR_PANEL;}
                        default -> {return SHAPE_EMPTY;}
                    }
                }).build());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        var stack = player.getMainHandItem();
        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(stack)) {
                placementHelper.getOffset(player, level, state, pos, hit)
                        .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hit);
                return InteractionResult.SUCCESS;
            }
        }
        if(state.getValue(STATE) == State.EMPTY) {
            if (ModdedBlocks.FACTORY_LIGHT.is(stack.getItem())) {
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
            if (ModdedBlocks.WIRE_CONNECTOR.is(stack.getItem())) {
                if(!player.isCreative())
                    stack.shrink(1);
                if(!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(STATE, State.WIRE_CONNECTOR));
                    level.playSound(null, pos,
                            ModdedBlocks.WIRE_CONNECTOR.getDefaultState().getSoundType().getPlaceSound(),
                            SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            if (ModdedBlocks.CORD_JUNCTION.is(stack.getItem())) {
                if(!player.isCreative())
                    stack.shrink(1);
                if(!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(STATE, State.CORD_JUNCTION));
                    level.playSound(null, pos,
                            ModdedBlocks.CORD_JUNCTION.getDefaultState().getSoundType().getPlaceSound(),
                            SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            if (ModdedBlocks.SOLAR_PANEL.is(stack.getItem())) {
                if(!player.isCreative())
                    stack.shrink(1);
                if(!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(STATE, State.SOLAR_PANEL));
                    level.playSound(null, pos,
                            ModdedBlocks.SOLAR_PANEL.getDefaultState().getSoundType().getPlaceSound(),
                            SoundSource.BLOCKS, 1.0f, level.random.nextFloat() * 0.25f + 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        } else {
            if(stack.isEmpty() || stack.getItem() instanceof ILightBulb) {
                return onBlockEntityUse(level, pos, be ->
                        be.replaceBulb(player, hand, stack)
                                ? InteractionResult.SUCCESS
                                : InteractionResult.FAIL);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if(state.getValue(STATE) != State.EMPTY) {
            var player = context.getPlayer();
            if(player != null && !player.isCreative())
                switch (state.getValue(STATE)) {
                    case LAMP_LOW_POWER, LAMP_ON, LAMP -> player.addItem(ModdedBlocks.FACTORY_LIGHT.asStack());
                    case SOLAR_PANEL ->  player.addItem(ModdedBlocks.SOLAR_PANEL.asStack());
                    case WIRE_CONNECTOR ->  player.addItem(ModdedBlocks.WIRE_CONNECTOR.asStack());
                    case CORD_JUNCTION ->  player.addItem(ModdedBlocks.CORD_JUNCTION.asStack());
                }

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
        LAMP_ON,
        WIRE_CONNECTOR,
        CORD_JUNCTION,
        SOLAR_PANEL;

        @Override
        public String getSerializedName() {
            return switch(this) {
                case EMPTY -> "empty";
                case LAMP -> "lamp";
                case LAMP_LOW_POWER -> "lamp_low_power";
                case LAMP_ON -> "lamp_on";
                case WIRE_CONNECTOR -> "wire_connector";
                case CORD_JUNCTION -> "cord_junction";
                case SOLAR_PANEL -> "solar_panel";
            };
        }
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return ModdedBlocks.CEILING_TILE::isIn;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof CeilingTileBlock;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
            List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
                    ray.getDirection().getAxis(), dir -> world.getBlockState(pos.relative(dir)).canBeReplaced());

            if (directions.isEmpty())
                return PlacementOffset.fail();
            else {
                return PlacementOffset.success(pos.relative(directions.get(0)), s -> s.getBlock().defaultBlockState());
            }
        }
    }
}
