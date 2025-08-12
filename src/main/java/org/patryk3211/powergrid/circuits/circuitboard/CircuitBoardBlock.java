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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IRedstoneComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class CircuitBoardBlock extends ElectricBlock implements IBE<CircuitBoardBlockEntity> {
    private static final VoxelShape SHAPE_PLATE = box(0, 0, 0, 16, 2, 16);

    public CircuitBoardBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        withBlockEntityDo(world, pos, be -> {
            be.withSchematic(CircuitSchematic.fromStack(stack));
            be.setAdditionalData(stack.getTag());
        });
        super.setPlacedBy(world, pos, state, placer, stack);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        final var shape = new VoxelShape[] { SHAPE_PLATE };
        withBlockEntityDo(world, pos, be -> {
            var shapeCopy = shape[0];
            for(int i = 0; i < be.terminalCount(); i++) {
                var terminal = be.terminal(state, i);
                shapeCopy = Shapes.or(((TerminalBoundingBox) terminal).getShape(), shapeCopy);
            }
            for(var placed : be.getComponents(IInteractableComponent.class)) {
                var dynamic = (IInteractableComponent) placed.component;
                shapeCopy = Shapes.or(dynamic.getShape(placed), shapeCopy);
            }
            shape[0] = shapeCopy;
        });
        return shape[0];
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        var stack = super.getCloneItemStack(world, pos, state);
        withBlockEntityDo(world, pos, be -> {
            var tag = new CompoundTag();
            tag.put("Schematic", be.getSchematic().serializeNbt());
            stack.setTag(tag);
        });
        return stack;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        // This is to make redstone wire connect
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        var output = new MutableInt();
        withBlockEntityDo(world, pos, be -> {
            for(var placed : be.getComponents(IRedstoneComponent.class)) {
                var redstone = (IRedstoneComponent) placed.component;
                if(!redstone.isEmitter())
                    continue;
                if(placed.has(Orientation.PROPERTY) && placed.get(Orientation.PROPERTY) != getOrientation(state, direction.getOpposite()))
                    continue;
                var level = redstone.getEmittedLevel(placed);
                if(level > output.getValue())
                    output.setValue(level);
            }
        });
        return output.getValue();
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

    public Direction getDirection(BlockState state, Orientation orientation) {
        return switch(orientation) {
            case UP -> Direction.NORTH;
            case RIGHT -> Direction.EAST;
            case DOWN -> Direction.SOUTH;
            case LEFT -> Direction.WEST;
        };
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        withBlockEntityDo(world, pos, be -> {
            var dirVec = sourcePos.subtract(pos);
            var dir = Direction.fromDelta(dirVec.getX(), dirVec.getY(), dirVec.getZ());
            if(dir == null)
                return;
            var power = world.getSignal(sourcePos, dir.getOpposite());
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
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var beResult = onBlockEntityUse(world, pos, be -> {
            var hitLocalPos = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            for(var placed : be.getComponents(IInteractableComponent.class)) {
                var dynamic = (IInteractableComponent) placed.component;
                var outline = dynamic.getShape(placed).bounds().inflate(1 / 32f);
                if(!outline.contains(hitLocalPos))
                    continue;
                return dynamic.use(be, placed, player);
            }
            return InteractionResult.PASS;
        });
        if(beResult != InteractionResult.PASS)
            return beResult;
        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag options) {
        var schematic = CircuitSchematic.fromStack(stack);
        if(schematic != null && schematic.getName() != null) {
            tooltip.add(Lang
                    .translate("circuit_board.tooltip.schematic")
                    .add(Component.literal(schematic.getName()))
                    .style(ChatFormatting.GRAY)
                    .component());
        }
        super.appendHoverText(stack, world, tooltip, options);
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
