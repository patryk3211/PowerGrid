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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.utility.Lang;

public class TransformerSmallBlock extends ElectricBlock implements IBE<TransformerSmallBlockEntity> {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = Properties.HORIZONTAL_AXIS;
    public static final IntProperty COILS = IntProperty.of("coils", 0, 2);

    private static final TerminalBoundingBox Z_TERMINAL_1 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 12, 2, 4, 17, 5);
    private static final TerminalBoundingBox Z_TERMINAL_2 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 12, 11, 4, 17, 14);
    private static final TerminalBoundingBox Z_TERMINAL_3 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 12, 2, 15, 17, 5);
    private static final TerminalBoundingBox Z_TERMINAL_4 = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 11, 12, 11, 15, 17, 14);

    private static final TerminalBoundingBox X_TERMINAL_1 = Z_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_2 = Z_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_3 = Z_TERMINAL_3.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox X_TERMINAL_4 = Z_TERMINAL_4.rotateAroundY(BlockRotation.CLOCKWISE_90);

    private static final VoxelShape SHAPE_Z = VoxelShapes.union(
            createCuboidShape(2, 0, 0, 14, 14, 16),
            Z_TERMINAL_1.getShape(),
            Z_TERMINAL_2.getShape(),
            Z_TERMINAL_3.getShape(),
            Z_TERMINAL_4.getShape()
    );

    private static final VoxelShape SHAPE_X = VoxelShapes.union(
            createCuboidShape(0, 0, 2, 16, 14, 14),
            X_TERMINAL_1.getShape(),
            X_TERMINAL_2.getShape(),
            X_TERMINAL_3.getShape(),
            X_TERMINAL_4.getShape()
    );

    public TransformerSmallBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(COILS, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_AXIS, COILS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ModdedBlocks.TRANSFORMER_CORE.get().getPickStack(world, pos, state);
    }

    public ActionResult onWinding(BlockState state, ItemUsageContext context) {
        var pos = context.getBlockPos();
        var terminal = terminalIndexAt(state, context.getHitPos().subtract(pos.getX(), pos.getY(), pos.getZ()));
        var stack = context.getStack();
        var nbt = stack.getNbt();
        if(terminal >= 0) {
            // Make coil between selected terminals.
            var firstTerminal = nbt.getInt("Terminal");
            if(terminal == firstTerminal) {
                IElectric.sendMessage(context, Lang.translate("message.coil_same_terminal").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
            var be = context.getWorld().getBlockEntity(context.getBlockPos(), ModdedBlockEntities.TRANSFORMER_SMALL.get());
            if(be.isEmpty())
                return ActionResult.FAIL;
            var turns = nbt.getInt("Turns");
            // TODO: Use the whole player inventory
            var player = context.getPlayer();
            boolean ignoreItemCount = player != null && player.isCreative();
            if(stack.getCount() < turns && !ignoreItemCount) {
                IElectric.sendMessage(context, Lang.translate("message.coil_missing_items").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
            if(!context.getWorld().isClient) {
                if (be.get().hasPrimary()) {
                    be.get().makeSecondary(firstTerminal, terminal, turns, stack.getItem());
                } else {
                    be.get().makePrimary(firstTerminal, terminal, turns, stack.getItem());
                }
            }
            if(!ignoreItemCount) {
                stack.decrement(turns);
            }
            stack.setNbt(null);
            return ActionResult.SUCCESS;
        } else {
            // Add turn.
            nbt.putInt("Turns", nbt.getInt("Turns") + 1);
            return ActionResult.SUCCESS;
        }
    }

    @Override
    public ActionResult onWire(BlockState state, ItemUsageContext context) {
        var stack = context.getStack();
        // Check if wire is in winding mode.
        if(stack.hasNbt()) {
            var nbt = stack.getNbt();
            if(nbt.contains("Turns")) {
                return onWinding(state, context);
            }
        }
        // Not in winding mode, regular wire terminal check.
        var result = super.onWire(state, context);
        if(result == ActionResult.PASS) {
            // Not hit a terminal.
            if(stack.hasNbt()) {
                // Has first terminal data.
                var be = context.getWorld().getBlockEntity(context.getBlockPos(), ModdedBlockEntities.TRANSFORMER_SMALL.get());
                if(be.isEmpty())
                    return ActionResult.FAIL;
                var nbt = stack.getNbt();
                if(be.get().isTerminalUsed(nbt.getInt("Terminal"))) {
                    IElectric.sendMessage(context, Lang.translate("message.coil_exists").style(Formatting.RED).component());
                    return ActionResult.FAIL;
                }
                var posArray = nbt.getIntArray("Position");
                var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                if(firstPosition.equals(context.getBlockPos())) {
                    // Put into winding mode.
                    nbt.putInt("Turns", 1);
                    nbt.remove("Position");
                    return ActionResult.SUCCESS;
                }
            }
        }
        return result;
    }

    @Override
    public int terminalCount() {
        return 4;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(HORIZONTAL_AXIS)) {
            case X -> switch(index) {
                case 0 -> X_TERMINAL_1;
                case 1 -> X_TERMINAL_2;
                case 2 -> X_TERMINAL_3;
                case 3 -> X_TERMINAL_4;
                default -> null;
            };
            case Z -> switch(index) {
                case 0 -> Z_TERMINAL_1;
                case 1 -> Z_TERMINAL_2;
                case 2 -> Z_TERMINAL_3;
                case 3 -> Z_TERMINAL_4;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public Class<TransformerSmallBlockEntity> getBlockEntityClass() {
        return TransformerSmallBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransformerSmallBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.TRANSFORMER_SMALL.get();
    }
}
