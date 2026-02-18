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
package org.patryk3211.powergrid.kinetics.generator.winding;

import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.ALONG_FIRST_AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.PART;



public class WindingItem extends Item {
    public WindingItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.has(DataComponents.CUSTOM_DATA);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if(user.isShiftKeyDown()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return InteractionResultHolder.success(stack);
        }
        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var pos = context.getClickedPos();
        var world = context.getLevel();
        var stack = context.getItemInHand();
        if(context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return InteractionResult.SUCCESS;
        }

        var hitState = world.getBlockState(pos);
        if(hitState.is(ModdedBlocks.WINDING.get())) {
            var side = context.getClickedFace();
            if(hitState.getValue(AXIS) != side.getAxis())
                return InteractionResult.FAIL;
            var newPos = pos.relative(side);
            if(!world.getBlockState(newPos).canBeReplaced())
                return InteractionResult.FAIL;
            if(!world.isClientSide) {
                stack.shrink(1);
                world.setBlockAndUpdate(pos, hitState.setValue(PART, 1));
                world.setBlockAndUpdate(newPos, hitState);
                world.playSound(null, pos, hitState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1, 1);
            }
            return InteractionResult.SUCCESS;
        }

        if(!hitState.is(AllBlocks.SHAFT.get()))
            return InteractionResult.FAIL;
        
        if(stack.has(DataComponents.CUSTOM_DATA)) {
            // Has first point
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            var posArray = tag.getIntArray("Position");
            var firstPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            if(firstPos.equals(pos))
                return InteractionResult.FAIL;

            var firstState = world.getBlockState(firstPos);
            if(!firstState.is(AllBlocks.SHAFT.get())) {
                stack.remove(DataComponents.CUSTOM_DATA);
                return InteractionResult.FAIL;
            }

            var axis = hitState.getValue(AXIS);
            if(axis != firstState.getValue(AXIS))
                return InteractionResult.FAIL;

            var placementAxis = getPlacementAxis(pos, firstPos);
            if(placementAxis == axis || !isPlacementAxisAligned(pos, firstPos))
                return InteractionResult.FAIL;

            var isAlongFirst = isAlongFirst(placementAxis, axis);
            var baseState = ModdedBlocks.WINDING.getDefaultState()
                    .setValue(AXIS, placementAxis)
                    .setValue(ALONG_FIRST_AXIS, isAlongFirst)
                    .setValue(PART, 1);

            var length = getPlacementDelta(pos, firstPos);
            if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, Math.abs(length) + 1))
                return InteractionResult.FAIL;

            // Verify the winding can be placed
            if(length > 0) {
                for(int i = 1; i < length; ++i) {
                    if(!world.getBlockState(firstPos.relative(placementAxis, i)).canBeReplaced()) {
                        return InteractionResult.FAIL;
                    }
                }
            } else {
                for(int i = -1; i > length; --i) {
                    if(!world.getBlockState(firstPos.relative(placementAxis, i)).canBeReplaced()) {
                        return InteractionResult.FAIL;
                    }
                }
            }
            if(world.isClientSide)
                return InteractionResult.SUCCESS;

            // Place the winding
            var offsetDir = Direction.fromAxisAndDirection(placementAxis, Direction.AxisDirection.POSITIVE);
            var start = length > 0 ? firstPos : pos;
            length = Math.abs(length);
            for(int i = 0; i <= length; ++i) {
                if(i == 0) {
                    world.setBlockAndUpdate(start.relative(offsetDir, i), baseState.setValue(PART, 0));
                } else if(i == length) {
                    world.setBlockAndUpdate(start.relative(offsetDir, i), baseState.setValue(PART, 2));
                } else {
                    world.setBlockAndUpdate(start.relative(offsetDir, i), baseState);
                }
            }
            stack.remove(DataComponents.CUSTOM_DATA);
            PlayerUtilities.removeItems(context.getPlayer(), stack, length + 1);
            world.playSound(null, pos, baseState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1, 1);
        } else {
            if(world.isClientSide)
                return InteractionResult.SUCCESS;
            var tag = new CompoundTag();
            tag.putIntArray("Position", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean isAlongFirst(Direction.Axis placementAxis, Direction.Axis shaftAxis) {
        if(placementAxis.isHorizontal() && shaftAxis.isHorizontal())
            return false;
        if(placementAxis.isHorizontal() && shaftAxis.isVertical())
            return true;
        return shaftAxis == Direction.Axis.Z;
    }

    @NotNull
    public static Direction.Axis getPlacementAxis(BlockPos pos, BlockPos firstPos) {
        var dX = pos.getX() - firstPos.getX();
        var dY = pos.getY() - firstPos.getY();
        var dZ = pos.getZ() - firstPos.getZ();
        var lenX = Math.abs(dX);
        var lenY = Math.abs(dY);
        var lenZ = Math.abs(dZ);

        if(lenX > lenY && lenX > lenZ) {
            return Direction.Axis.X;
        } else if(lenY > lenZ) {
            return Direction.Axis.Y;
        } else {
            return Direction.Axis.Z;
        }
    }

    public static int getPlacementDelta(BlockPos pos, BlockPos firstPos) {
        var dX = pos.getX() - firstPos.getX();
        var dY = pos.getY() - firstPos.getY();
        var dZ = pos.getZ() - firstPos.getZ();
        var lenX = Math.abs(dX);
        var lenY = Math.abs(dY);
        var lenZ = Math.abs(dZ);

        if(lenX > lenY && lenX > lenZ) {
            return dX;
        } else if(lenY > lenZ) {
            return dY;
        } else {
            return dZ;
        }
    }

    public static boolean isPlacementAxisAligned(BlockPos pos, BlockPos firstPos) {
        var hasX = pos.getX() != firstPos.getX() ? 1 : 0;
        var hasY = pos.getY() != firstPos.getY() ? 1 : 0;
        var hasZ = pos.getZ() != firstPos.getZ() ? 1 : 0;

        return hasX + hasY + hasZ < 2;
    }
}
