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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import static net.minecraft.state.property.Properties.AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.ALONG_FIRST_AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.PART;

public class WindingItem extends Item {
    public WindingItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return super.hasGlint(stack) || stack.hasNbt();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if(user.isSneaking()) {
            stack.setNbt(null);
            return TypedActionResult.success(stack);
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var pos = context.getBlockPos();
        var world = context.getWorld();
        var stack = context.getStack();
        if(context.getPlayer() != null && context.getPlayer().isSneaking()) {
            stack.setNbt(null);
            return ActionResult.SUCCESS;
        }

        var hitState = world.getBlockState(pos);
        if(hitState.isOf(ModdedBlocks.WINDING.get())) {
            var side = context.getSide();
            if(hitState.get(AXIS) != side.getAxis())
                return ActionResult.FAIL;
            var newPos = pos.offset(side);
            if(!world.getBlockState(newPos).isReplaceable())
                return ActionResult.FAIL;
            if(!world.isClient) {
                stack.decrement(1);
                world.setBlockState(pos, hitState.with(PART, 1));
                world.setBlockState(newPos, hitState);
                world.playSound(null, pos, hitState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1, 1);
            }
            return ActionResult.SUCCESS;
        }

        if(!hitState.isOf(AllBlocks.SHAFT.get()))
            return ActionResult.FAIL;
        
        if(stack.hasNbt()) {
            // Has first point
            var tag = stack.getNbt();
            var posArray = tag.getIntArray("Position");
            var firstPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            if(firstPos.equals(pos))
                return ActionResult.FAIL;

            var firstState = world.getBlockState(firstPos);
            if(!firstState.isOf(AllBlocks.SHAFT.get())) {
                stack.setNbt(null);
                return ActionResult.FAIL;
            }

            var axis = hitState.get(AXIS);
            if(axis != firstState.get(AXIS))
                return ActionResult.FAIL;

            var placementAxis = getPlacementAxis(pos, firstPos);
            if(placementAxis == axis)
                return ActionResult.FAIL;

            var isAlongFirst = isAlongFirst(placementAxis, axis);
            var baseState = ModdedBlocks.WINDING.getDefaultState()
                    .with(AXIS, placementAxis)
                    .with(ALONG_FIRST_AXIS, isAlongFirst)
                    .with(PART, 1);

            var length = getPlacementDelta(pos, firstPos);
            if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, length + 1))
                return ActionResult.FAIL;

            // Verify the winding can be placed
            if(length > 0) {
                for(int i = 1; i < length; ++i) {
                    if(!world.getBlockState(firstPos.offset(placementAxis, i)).isReplaceable()) {
                        return ActionResult.FAIL;
                    }
                }
            } else {
                for(int i = -1; i > length; --i) {
                    if(!world.getBlockState(firstPos.offset(placementAxis, i)).isReplaceable()) {
                        return ActionResult.FAIL;
                    }
                }
            }
            if(world.isClient)
                return ActionResult.SUCCESS;

            // Place the winding
            var offsetDir = Direction.from(placementAxis, Direction.AxisDirection.POSITIVE);
            var start = length > 0 ? firstPos : pos;
            length = Math.abs(length);
            for(int i = 0; i <= length; ++i) {
                if(i == 0) {
                    world.setBlockState(start.offset(offsetDir, i), baseState.with(PART, 0));
                } else if(i == length) {
                    world.setBlockState(start.offset(offsetDir, i), baseState.with(PART, 2));
                } else {
                    world.setBlockState(start.offset(offsetDir, i), baseState);
                }
            }
            stack.setNbt(null);
            PlayerUtilities.removeItems(context.getPlayer(), stack, length + 1);
            world.playSound(null, pos, baseState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1, 1);
        } else {
            if(world.isClient)
                return ActionResult.SUCCESS;
            var tag = new NbtCompound();
            tag.putIntArray("Position", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            stack.setNbt(tag);
        }

        return ActionResult.SUCCESS;
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
}
