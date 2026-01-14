/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.light.fixture;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LightFixtureBlock extends DirectionalElectricBlock implements IBE<LightFixtureBlockEntity>, IAcceptCord {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 2);
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    private static final TerminalBoundingBox[] UP_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 2.5, 1.5, 6.5, 4.5, 3.5, 9.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11.5, 1.5, 6.5, 13.5, 3.5, 9.5)
    };

    private static final VoxelShape SHAPE_UP = box(3.5, 0, 3.5, 12.5, 3, 12.5);

    Vec3 modelOffset;

    public LightFixtureBlock(Properties settings) {
        super(settings.lightLevel(state -> switch(state.getValue(POWER)) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 0;
        }));
        modelOffset = Vec3.ZERO;
        registerDefaultState(defaultBlockState().setValue(POWER, 0));

        var shaper = VoxelShaper.forDirectional(SHAPE_UP, Direction.UP);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> BlockStateTerminalCollection.each(UP_TERMINALS, terminal -> {
                    var facing = state.getValue(FACING);
                    terminal = switch(facing) {
                        case UP -> terminal;
                        case DOWN -> terminal.rotateAroundX(Rotation.CLOCKWISE_180);
                        case NORTH -> terminal.rotateAroundX(Rotation.CLOCKWISE_90);
                        case SOUTH -> terminal.rotateAroundX(Rotation.COUNTERCLOCKWISE_90);
                        case EAST -> terminal.rotateAroundX(Rotation.CLOCKWISE_90).rotateAroundY(Rotation.CLOCKWISE_90);
                        case WEST -> terminal.rotateAroundX(Rotation.CLOCKWISE_90).rotateAroundY(Rotation.COUNTERCLOCKWISE_90);
                    };
                    if(!state.getValue(ALONG_FIRST_AXIS)) {
                        terminal = terminal.rotate(facing.getAxis(), Rotation.CLOCKWISE_90);
                    }
                    return terminal;
                }), POWER)
                .withShapeMapper(state -> shaper.get(state.getValue(FACING)))
                .build());
    }

    public static <B extends LightFixtureBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setBulbModelOffset(Vec3 modelOffset) {
        return b -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> b.onRegister(block -> block.modelOffset = modelOffset));
            return b;
        };
    }

    public static <B extends LightFixtureBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setBulbModelOffset(float x, float y, float z) {
        return b -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> b.onRegister(block -> block.modelOffset = new Vec3(x, y, z)));
            return b;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER, ALONG_FIRST_AXIS);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(FACING);
        return canSupportCenter(world, pos.relative(facing, -1), facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        var facing = state.getValue(FACING);
        return direction == facing.getOpposite() && !canSurvive(state, world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace();
        boolean along = true;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            if(player.getAxis() == Direction.Axis.X)
                along = false;
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ALONG_FIRST_AXIS, along);
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        var stack = player.getItemInHand(hand);
        if(stack.isEmpty() || stack.getItem() instanceof ILightBulb) {
            return onBlockEntityUse(world, pos, be ->
                    be.replaceBulb(player, hand, stack)
                            ? InteractionResult.SUCCESS
                            : InteractionResult.FAIL);
        } else if(stack.getItem() instanceof DyeItem dye) {
            return onBlockEntityUse(world, pos, be -> be.setColor(dye.getDyeColor()));
        } else {
            // Holding something else.
            return InteractionResult.PASS;
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(be instanceof LightFixtureBlockEntity fixture) {
            var bulb = fixture.getBulbState();
            if(bulb != null && !bulb.isBurned()) {
                var drops = new ArrayList<>(super.getDrops(state, params));
                drops.add(new ItemStack(fixture.getBulbState().getItem(), 1));
                return drops;
            }
        }
        return super.getDrops(state, params);
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
        super.destroy(world, pos, state);
    }

    @Override
    public Class<LightFixtureBlockEntity> getBlockEntityClass() {
        return LightFixtureBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LightFixtureBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.LIGHT_FIXTURE.get();
    }

    @Override
    public @Nullable AutoCordEndpoint getEndpoint(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var facing = state.getValue(FACING);

        var center = Vec3.atCenterOf(pos);
        var normal = facing.getNormal();
        var point = center.add(normal.getX() * -0.40625, normal.getY() * -0.40625, normal.getZ() * -0.40625);

        return new AutoCordEndpoint(context.getClickedPos(), 0, 1, point,
                renderPlug() ? context.getClickedFace() : null);
    }
}
