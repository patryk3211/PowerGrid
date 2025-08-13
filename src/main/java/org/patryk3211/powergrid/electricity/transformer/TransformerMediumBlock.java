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
package org.patryk3211.powergrid.electricity.transformer;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.Optional;
import java.util.function.BiConsumer;

public class TransformerMediumBlock extends TransformerBlock implements IBE<TransformerMediumBlockEntity> {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

    private static final TerminalBoundingBox TERMINAL_Z_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 9, 6, 5, 16, 10);
    private static final TerminalBoundingBox TERMINAL_Z_2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 9, 6, 16, 16, 10);

    private static final TerminalBoundingBox TERMINAL_X_1 = TERMINAL_Z_1.rotateAroundY(Rotation.CLOCKWISE_90);
    private static final TerminalBoundingBox TERMINAL_X_2 = TERMINAL_Z_2.rotateAroundY(Rotation.CLOCKWISE_90);

    private static final VoxelShape SHAPE_Z_BOTTOM = box(2, 0, 0, 14, 16, 16);
    private static final VoxelShape SHAPE_X_BOTTOM = box(0, 0, 2, 16, 16, 14);

    private static final VoxelShape SHAPE_Z_TOP = Shapes.or(
            box(2, 0, 0, 14, 12, 16),
            TERMINAL_Z_1.getShape(),
            TERMINAL_Z_2.getShape()
    );
    private static final VoxelShape SHAPE_X_TOP = Shapes.or(
            box(0, 0, 2, 16, 12, 14),
            TERMINAL_X_1.getShape(),
            TERMINAL_X_2.getShape()
    );

    public TransformerMediumBlock(Properties settings) {
        super(settings, 240);
        registerDefaultState(defaultBlockState().setValue(COILS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_AXIS, PART, COILS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch(state.getValue(HORIZONTAL_AXIS)) {
            case Z -> switch(state.getValue(PART)) {
                case 0, 1 -> SHAPE_Z_BOTTOM;
                case 2, 3 -> SHAPE_Z_TOP;
                default -> throw new IllegalStateException();
            };
            case X -> switch (state.getValue(PART)) {
                case 0, 1 -> SHAPE_X_BOTTOM;
                case 2, 3 -> SHAPE_X_TOP;
                default -> throw new IllegalStateException();
            };
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
        super.destroy(world, pos, state);
        var axis = state.getValue(HORIZONTAL_AXIS);
        int x = 0;
        int y = 0;
        switch(state.getValue(PART)) {
            case 0:
                x = 1;
                y = 1;
                break;
            case 1:
                x = -1;
                y = 1;
                break;
            case 2:
                x = 1;
                y = -1;
                break;
            case 3:
                x = -1;
                y = -1;
                break;
        }
        world.destroyBlock(pos.relative(axis, x), false);
        world.destroyBlock(pos.relative(Direction.Axis.Y, y), false);
        world.destroyBlock(pos.relative(axis, x).relative(Direction.Axis.Y, y), false);
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        var part = state.getValue(PART);
        if(part == 0 || part == 1) {
            // Bottom parts have no terminals
            return null;
        }
        return switch(state.getValue(HORIZONTAL_AXIS)) {
            case Z -> switch(index) {
                case 0, 2 -> TERMINAL_Z_1;
                case 1, 3 -> TERMINAL_Z_2;
                default -> null;
            };
            case X -> switch(index) {
                case 0, 2 -> TERMINAL_X_1;
                case 1, 3 -> TERMINAL_X_2;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if(world instanceof ServerLevel serverLevel) {
            boolean shouldBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
            if(!shouldBreak) {
                return InteractionResult.SUCCESS;
            } else {
                var axis = state.getValue(HORIZONTAL_AXIS);
                int x = 0;
                int y = 0;
                switch(state.getValue(PART)) {
                    case 0:
                        x = 1;
                        y = 1;
                        break;
                    case 1:
                        x = -1;
                        y = 1;
                        break;
                    case 2:
                        x = 1;
                        y = -1;
                        break;
                    case 3:
                        x = -1;
                        y = -1;
                        break;
                }
                if (player != null && !player.isCreative()) {
                    Block.getDrops(state, serverLevel, pos, world.getBlockEntity(pos), player, context.getItemInHand()).forEach((itemStack) -> player.getInventory().placeItemBackInInventory(itemStack));
                }
                state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);

                BiConsumer<Integer, Integer> processOffset = (offsetX, offsetY) -> {
                    var offsetPos = pos.relative(axis, offsetX).relative(Direction.Axis.Y, offsetY);
                    world.destroyBlock(offsetPos, false);
                };
                processOffset.accept(0, 0);
                processOffset.accept(x, 0);
                processOffset.accept(0, y);
                processOffset.accept(x, y);

                IWrenchable.playRemoveSound(world, pos);
                return InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public int terminalIndexAt(BlockState state, Vec3 pos) {
        var index = super.terminalIndexAt(state, pos);
        if(index >= 0) {
            if(state.getValue(PART) == 3) {
                // Part 2 gets the default indices but part 3 is offset to
                // allow using a single block entity.
                return index + 2;
            }
        }
        return index;
    }

    @Override
    public Optional<TransformerBlockEntity> getBlockEntity(Level world, BlockPos pos, BlockState state) {
        // Block entity is held by part 0
        var axis = state.getValue(HORIZONTAL_AXIS);
        var bePos = switch(state.getValue(PART)) {
            case 0 -> pos;
            case 1 -> pos.relative(axis, -1);
            case 2 -> pos.relative(Direction.Axis.Y, -1);
            case 3 -> pos.relative(axis, -1).relative(Direction.Axis.Y, -1);
            default -> throw new IllegalStateException();
        };
        return Optional.ofNullable(getBlockEntity(world, bePos));
    }

    @Override
    protected boolean isInitiator(BlockPos pos, BlockState state, BlockPos initiator) {
        // Initiator can either be part 2 or 3 since they have the terminals.
        int y, x;
        switch(state.getValue(PART)) {
            case 0 -> {
                x = 1;
                y = 1;
            }
            case 1 -> {
                x = -1;
                y = 1;
            }
            case 2 -> {
                x = 1;
                y = 0;
            }
            case 3 -> {
                x = -1;
                y = 0;
            }
            default -> throw new IllegalStateException();
        }
        var p1 = pos.relative(Direction.Axis.Y, y);
        var p2 = p1.relative(state.getValue(HORIZONTAL_AXIS), x);
        return initiator.equals(p1) || initiator.equals(p2);
    }

    @Override
    public Class<TransformerMediumBlockEntity> getBlockEntityClass() {
        return TransformerMediumBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransformerMediumBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.TRANSFORMER_MEDIUM.get();
    }
}
