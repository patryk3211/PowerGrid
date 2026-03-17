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
import dev.architectury.registry.menu.MenuRegistry;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IRedstoneComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.editor.CircuitBoardEditMenu;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.utility.Lang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CircuitBoardBlock extends ElectricBlock implements IBE<CircuitBoardBlockEntity> {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 2);

    private static final VoxelShape SHAPE_PLATE = box(0, 0, 0, 16, 2, 16);

    public CircuitBoardBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var face = ctx.getClickedFace();
        if(face.getAxis() == Direction.Axis.Y) {
            var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ?
                    ctx.getHorizontalDirection() : ctx.getHorizontalDirection().getOpposite();
            return defaultBlockState()
                    .setValue(HORIZONTAL_FACING, player)
                    .setValue(ROTATION, face == Direction.UP ? 0 : 2);
        } else {
            return defaultBlockState()
                    .setValue(ROTATION, 1)
                    .setValue(HORIZONTAL_FACING, face.getOpposite());
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        withBlockEntityDo(world, pos, be -> {
            be.setSchematic(CircuitSchematic.fromStack(stack));
            be.setAdditionalData(stack.getTag());
        });
        super.setPlacedBy(world, pos, state, placer, stack);
    }

    public static VoxelShape rotate(VoxelShape shapeIn, float x, float y) {
        if(y == 0 && x == 0)
            return shapeIn;

        var result = new MutableObject<>(Shapes.empty());
        var center = new Vec3(8, 8, 8);
        shapeIn.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            var v1 = new Vec3(x1, y1, z1).scale(16)
                    .subtract(center);
            var v2 = new Vec3(x2, y2, z2).scale(16)
                    .subtract(center);

            v1 = VecHelper.rotate(v1, x, Direction.Axis.X);
            v1 = VecHelper.rotate(v1, y, Direction.Axis.Y)
                    .add(center);
            v2 = VecHelper.rotate(v2, x, Direction.Axis.X);
            v2 = VecHelper.rotate(v2, y, Direction.Axis.Y)
                    .add(center);

            var rotated = box(
                    Math.min(v1.x, v2.x),
                    Math.min(v1.y, v2.y),
                    Math.min(v1.z, v2.z),
                    Math.max(v1.x, v2.x),
                    Math.max(v1.y, v2.y),
                    Math.max(v1.z, v2.z)
            );
            result.setValue(Shapes.or(result.getValue(), rotated));
        });
        return result.getValue();
    }

    public static int getAngleX(BlockState state) {
        if(!(state.getBlock() instanceof CircuitBoardBlock))
            return 0;
        return state.getValue(ROTATION) * 90;
    }

    public static int getAngleY(BlockState state) {
        if(!(state.getBlock() instanceof CircuitBoardBlock))
            return 0;
        int angle = switch(state.getValue(HORIZONTAL_FACING)) {
            case NORTH -> 0;
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> throw new IllegalStateException();
        };
        if(state.getValue(ROTATION) == 2)
            angle -= 180;
        return angle;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int x = getAngleX(state);
        int y = getAngleY(state);
        final VoxelShape[] shape = {rotate(SHAPE_PLATE, x, y)};

        withBlockEntityDo(world, pos, be -> {
            shape[0] = be.getShape(state, shape[0]);
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
    public static Orientation getOrientation(BlockState state, Direction side) {
        var facing = state.getValue(HORIZONTAL_FACING);
        if(state.getValue(ROTATION) == 1) {
            if(side == Direction.UP)
                return Orientation.UP;
            if(side == Direction.DOWN)
                return Orientation.DOWN;
            if(side == facing.getCounterClockWise())
                return Orientation.LEFT;
            if(side == facing.getClockWise())
                return Orientation.RIGHT;
            return null;
        }
        Orientation orientation;
        if(state.getValue(ROTATION) == 0) {
            orientation = switch (side) {
                case NORTH -> Orientation.UP;
                case EAST -> Orientation.RIGHT;
                case SOUTH -> Orientation.DOWN;
                case WEST -> Orientation.LEFT;
                default -> null;
            };
        } else {
            orientation = switch (side) {
                case NORTH -> Orientation.UP;
                case EAST -> Orientation.LEFT;
                case SOUTH -> Orientation.DOWN;
                case WEST -> Orientation.RIGHT;
                default -> null;
            };
        }
        if(orientation == null)
            return null;
        if(state.getValue(ROTATION) == 0) {
            orientation = switch (facing) {
                case NORTH -> orientation;
                case SOUTH -> orientation.getOpposite();
                case EAST -> orientation.getCounterClockwise();
                case WEST -> orientation.getClockwise();
                default -> throw new IllegalStateException();
            };
        } else {
            orientation = switch (facing) {
                case NORTH -> orientation;
                case SOUTH -> orientation.getOpposite();
                case WEST -> orientation.getCounterClockwise();
                case EAST -> orientation.getClockwise();
                default -> throw new IllegalStateException();
            };
        }
        return orientation;
    }

    public static Direction getDirection(BlockState state, Orientation orientation) {
        var facing = state.getValue(HORIZONTAL_FACING);
        if(state.getValue(ROTATION) == 1) {
            if(orientation == Orientation.UP)
                return Direction.UP;
            if(orientation == Orientation.DOWN)
                return Direction.DOWN;
            if(orientation == Orientation.LEFT)
                return facing.getCounterClockWise();
            if(orientation == Orientation.RIGHT)
                return facing.getClockWise();
            throw new IllegalStateException();
        }
        Direction dir;
        if(state.getValue(ROTATION) == 0) {
            dir = switch (orientation) {
                case UP -> Direction.NORTH;
                case RIGHT -> Direction.EAST;
                case DOWN -> Direction.SOUTH;
                case LEFT -> Direction.WEST;
            };
            dir = switch (state.getValue(HORIZONTAL_FACING)) {
                case NORTH -> dir;
                case SOUTH -> dir.getOpposite();
                case EAST -> dir.getClockWise();
                case WEST -> dir.getCounterClockWise();
                default -> throw new IllegalStateException();
            };
        } else {
            dir = switch (orientation) {
                case UP -> Direction.SOUTH;
                case RIGHT -> Direction.EAST;
                case DOWN -> Direction.NORTH;
                case LEFT -> Direction.WEST;
            };
            dir = switch (state.getValue(HORIZONTAL_FACING)) {
                case NORTH -> dir.getOpposite();
                case SOUTH -> dir;
                case EAST -> dir.getCounterClockWise();
                case WEST -> dir.getClockWise();
                default -> throw new IllegalStateException();
            };
        }
        return dir;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        withBlockEntityDo(world, pos, be -> {
            var dirVec = sourcePos.subtract(pos);
            var dir = Direction.fromDelta(dirVec.getX(), dirVec.getY(), dirVec.getZ());
            if(dir == null)
                return;
            var power = world.getSignal(sourcePos, dir);
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
            hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -getAngleY(state), Direction.Axis.Y);
            hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -getAngleX(state), Direction.Axis.X);
            for(var placed : be.getComponents(IInteractableComponent.class)) {
                var dynamic = (IInteractableComponent) placed.component;
                var outline = dynamic.getShape(placed).bounds().inflate(1 / 32f);
                if(!outline.contains(hitLocalPos))
                    continue;
                return dynamic.use(be, placed, player);
            }
            if(player.isCreative() && player.isShiftKeyDown()) {
                be.repairBroken();
                return InteractionResult.SUCCESS;
            } else if(player.isCreative() && player instanceof ServerPlayer sp
                    && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
                MenuRegistry.openExtendedMenu(sp, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Circuit");
                    }

                    @Override
                    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                        if(!player.isCreative())
                            return null;
                        return CircuitBoardEditMenu.create(i, inventory, be);
                    }
                }, be::sendToMenu);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        if(beResult != InteractionResult.PASS)
            return beResult;
        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        var result = super.onWrenched(state, context);
        if(result.consumesAction() && context.getLevel().isClientSide)
            withBlockEntityDo(context.getLevel(), context.getClickedPos(), CircuitBoardBlockEntity::refreshModel);
        return result;
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
