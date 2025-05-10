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

import com.simibubi.create.foundation.block.IBE;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.Optional;
import java.util.function.BiConsumer;

public class TransformerMediumBlock extends TransformerBlock implements IBE<TransformerMediumBlockEntity> {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = Properties.HORIZONTAL_AXIS;
    public static final IntProperty PART = IntProperty.of("part", 0, 3);

    private static final TerminalBoundingBox TERMINAL_Z_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 9, 6, 5, 16, 10);
    private static final TerminalBoundingBox TERMINAL_Z_2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 9, 6, 16, 16, 10);

    private static final TerminalBoundingBox TERMINAL_X_1 = TERMINAL_Z_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox TERMINAL_X_2 = TERMINAL_Z_2.rotateAroundY(BlockRotation.CLOCKWISE_90);

    private static final VoxelShape SHAPE_Z_BOTTOM = createCuboidShape(2, 0, 0, 14, 16, 16);
    private static final VoxelShape SHAPE_X_BOTTOM = createCuboidShape(0, 0, 2, 16, 16, 14);

    private static final VoxelShape SHAPE_Z_TOP = VoxelShapes.union(
            createCuboidShape(2, 0, 0, 14, 12, 16),
            TERMINAL_Z_1.getShape(),
            TERMINAL_Z_2.getShape()
    );
    private static final VoxelShape SHAPE_X_TOP = VoxelShapes.union(
            createCuboidShape(0, 0, 2, 16, 12, 14),
            TERMINAL_X_1.getShape(),
            TERMINAL_X_2.getShape()
    );

    public TransformerMediumBlock(Settings settings) {
        super(settings, 240);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_AXIS, PART);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case Z -> switch(state.get(PART)) {
                case 0, 1 -> SHAPE_Z_BOTTOM;
                case 2, 3 -> SHAPE_Z_TOP;
                default -> throw new IllegalStateException();
            };
            case X -> switch (state.get(PART)) {
                case 0, 1 -> SHAPE_X_BOTTOM;
                case 2, 3 -> SHAPE_X_TOP;
                default -> throw new IllegalStateException();
            };
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);
        var axis = state.get(HORIZONTAL_AXIS);
        int x = 0;
        int y = 0;
        switch(state.get(PART)) {
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
        world.breakBlock(pos.offset(axis, x), false);
        world.breakBlock(pos.offset(Direction.Axis.Y, y), false);
        world.breakBlock(pos.offset(axis, x).offset(Direction.Axis.Y, y), false);
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        var part = state.get(PART);
        if(part == 0 || part == 1) {
            // Bottom parts have no terminals
            return null;
        }
        return switch(state.get(HORIZONTAL_AXIS)) {
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
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
        var world = context.getWorld();
        var pos = context.getBlockPos();
        var player = context.getPlayer();
        if(world instanceof ServerWorld serverLevel) {
            boolean shouldBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos, world.getBlockState(pos), null);
            if(!shouldBreak) {
                return ActionResult.SUCCESS;
            } else {
                var axis = state.get(HORIZONTAL_AXIS);
                int x = 0;
                int y = 0;
                switch(state.get(PART)) {
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
                    Block.getDroppedStacks(state, serverLevel, pos, world.getBlockEntity(pos), player, context.getStack()).forEach((itemStack) -> player.getInventory().offerOrDrop(itemStack));
                }
                state.onStacksDropped(serverLevel, pos, ItemStack.EMPTY, true);

                BiConsumer<Integer, Integer> processOffset = (offsetX, offsetY) -> {
                    var offsetPos = pos.offset(axis, offsetX).offset(Direction.Axis.Y, offsetY);
                    world.breakBlock(offsetPos, false);
                };
                processOffset.accept(0, 0);
                processOffset.accept(x, 0);
                processOffset.accept(0, y);
                processOffset.accept(x, y);

                this.playRemoveSound(world, pos);
                return ActionResult.SUCCESS;
            }
        } else {
            return ActionResult.SUCCESS;
        }
    }

    @Override
    public int terminalIndexAt(BlockState state, Vec3d pos) {
        var index = super.terminalIndexAt(state, pos);
        if(index >= 0) {
            if(state.get(PART) == 3) {
                // Part 2 gets the default indices but part 3 is offset to
                // allow using a single block entity.
                return index + 2;
            }
        }
        return index;
    }

    @Override
    public Optional<TransformerBlockEntity> getBlockEntity(World world, BlockPos pos, BlockState state) {
        // Block entity is held by part 0
        var axis = state.get(HORIZONTAL_AXIS);
        var bePos = switch(state.get(PART)) {
            case 0 -> pos;
            case 1 -> pos.offset(axis, -1);
            case 2 -> pos.offset(Direction.Axis.Y, -1);
            case 3 -> pos.offset(axis, -1).offset(Direction.Axis.Y, -1);
            default -> throw new IllegalStateException();
        };
        return Optional.ofNullable(getBlockEntity(world, bePos));
    }

    @Override
    protected boolean isInitiator(BlockPos pos, BlockState state, BlockPos initiator) {
        // Initiator can either be part 2 or 3 since they have the terminals.
        int y, x;
        switch(state.get(PART)) {
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
        var p1 = pos.offset(Direction.Axis.Y, y);
        var p2 = p1.offset(state.get(HORIZONTAL_AXIS), x);
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

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if(state.get(PART) == 0)
            return IBE.super.createBlockEntity(pos, state);
        return null;
    }
}
