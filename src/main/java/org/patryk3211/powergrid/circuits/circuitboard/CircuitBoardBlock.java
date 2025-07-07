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
package org.patryk3211.powergrid.circuits.circuitboard;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.IDynamicComponent;
import org.patryk3211.powergrid.circuits.components.IRedstoneComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class CircuitBoardBlock extends ElectricBlock implements IBE<CircuitBoardBlockEntity> {
    private static final VoxelShape SHAPE_PLATE = createCuboidShape(0, 0, 0, 16, 2, 16);

    public CircuitBoardBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        withBlockEntityDo(world, pos, be -> {
            be.withSchematic(CircuitSchematic.fromStack(stack));
        });
        super.onPlaced(world, pos, state, placer, stack);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final var shape = new VoxelShape[] { SHAPE_PLATE };
        withBlockEntityDo(world, pos, be -> {
            var shapeCopy = shape[0];
            for(int i = 0; i < be.terminalCount(); i++) {
                var terminal = be.terminal(state, i);
                shapeCopy = VoxelShapes.union(((TerminalBoundingBox) terminal).getShape(), shapeCopy);
            }
            for(var placed : be.getComponents(IDynamicComponent.class)) {
                var dynamic = (IDynamicComponent) placed.component;
                shapeCopy = VoxelShapes.union(dynamic.getShape(placed), shapeCopy);
            }
            shape[0] = shapeCopy;
        });
        return shape[0];
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        withBlockEntityDo(world, pos, be -> {
            var tag = new NbtCompound();
            tag.put("Schematic", be.getSchematic().serializeNbt());
            stack.setNbt(tag);
        });
        return stack;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        // This is to make redstone wire connect
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return super.getWeakRedstonePower(state, world, pos, direction);
    }

    @Nullable
    public Orientation getOrientation(BlockState state, Direction side) {
        // TODO: When circuit facing is implemented this needs to be updated.
        return switch(side) {
            case NORTH -> Orientation.UP;
            case EAST -> Orientation.RIGHT;
            case SOUTH -> Orientation.DOWN;
            case WEST -> Orientation.LEFT;
            default -> null;
        };
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        withBlockEntityDo(world, pos, be -> {
            var dirVec = sourcePos.subtract(pos);
            var dir = Direction.fromVector(dirVec.getX(), dirVec.getY(), dirVec.getZ());
            if(dir == null)
                return;
            var power = world.getEmittedRedstonePower(sourcePos, dir.getOpposite());
            for(var placed : be.getComponents(IRedstoneComponent.class)) {
                var redstone = (IRedstoneComponent) placed.component;
                if(!redstone.isReceiver())
                    continue;
                if(placed.has(Orientation.PROPERTY)) {
                    if(placed.get(Orientation.PROPERTY) == getOrientation(state, dir)) {
                        redstone.receiveRedstone(placed, power);
                    }
                } else {
                    redstone.receiveRedstone(placed, power);
                }
            }
        });
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var beResult = onBlockEntityUse(world, pos, be -> {
            var hitLocalPos = hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
            for(var placed : be.getComponents(IDynamicComponent.class)) {
                var dynamic = (IDynamicComponent) placed.component;
                var outline = dynamic.getShape(placed).getBoundingBox().expand(1 / 32f);
                if(!outline.contains(hitLocalPos))
                    continue;
                return dynamic.use(be, placed, player);
            }
            return ActionResult.PASS;
        });
        if(beResult != ActionResult.PASS)
            return beResult;
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        var schematic = CircuitSchematic.fromStack(stack);
        if(schematic != null && schematic.getName() != null) {
            tooltip.add(Lang
                    .translate("circuit_board.tooltip.schematic")
                    .add(Text.literal(schematic.getName()))
                    .style(Formatting.GRAY)
                    .component());
        }
        super.appendTooltip(stack, world, tooltip, options);
    }

    @Override
    public Class<CircuitBoardBlockEntity> getBlockEntityClass() {
        return CircuitBoardBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CircuitBoardBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CIRCUIT_BOARD.get();
    }
}
