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

import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.patryk3211.powergrid.utility.PlacementOverlay;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.Random;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingItem.*;

@Environment(EnvType.CLIENT)
public class WindingPreview {
    private static final Random r = new Random();

    @Nullable
    public static ItemStack getUsedWireStack(Player player) {
        var stack1 = player.getMainHandItem();
        var stack2 = player.getOffhandItem();
        if(stack1 != null && stack1.getItem() instanceof WindingItem && stack1.has(DataComponents.CUSTOM_DATA)) {
            return stack1;
        } else if(stack2 != null && stack2.getItem() instanceof WindingItem && stack2.has(DataComponents.CUSTOM_DATA)) {
            return stack2;
        } else {
            return null;
        }
    }

    public static void tick() {
        var player = Minecraft.getInstance().player;
        var world = Minecraft.getInstance().level;
        if(player == null || world == null)
            return;
        var stack = getUsedWireStack(player);
        if(stack == null)
            return;

        var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        var posArray = tag.getIntArray("Position");
        if(posArray.length < 3)
            return;
        var firstPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        var firstState = world.getBlockState(firstPos);
        if(!ShaftBlock.isShaft(firstState))
            return;

        var rayTrace = Minecraft.getInstance().hitResult;
        if(!(rayTrace instanceof BlockHitResult hit)) {
            if(r.nextInt(50) == 0) {
                world.addParticle(new DustParticleOptions(new Vector3f(.3f, .9f, .5f), 1),
                        firstPos.getX() + .5f + randomOffset(.25f), firstPos.getY() + .5f + randomOffset(.25f),
                        firstPos.getZ() + .5f + randomOffset(.25f), 0, 0, 0);
            }
            return;
        }

        var selected = hit.getBlockPos();
        var selectedState = world.getBlockState(selected);

       if (!ShaftBlock.isShaft(selectedState))
            selected = selected.relative(hit.getDirection());

        boolean canConnect = ShaftBlock.isShaft(selectedState)
                && isPlacementAxisAligned(selected, firstPos)
                && firstState.getValue(AXIS) == selectedState.getValue(AXIS);
        var placementAxis = getPlacementAxis(selected, firstPos);
        if(placementAxis == firstState.getValue(AXIS))
            canConnect = false;

        var length = getPlacementDelta(selected, firstPos);
        if(Math.abs(length) > 64)
            return;
        if(canConnect) {
            // Verify the winding can be placed
            if(length > 0) {
                for(int i = 1; i < length; ++i) {
                    if(!world.getBlockState(firstPos.relative(placementAxis, i)).canBeReplaced()) {
                        canConnect = false;
                        break;
                    }
                }
            } else {
                for(int i = -1; i > length; --i) {
                    if(!world.getBlockState(firstPos.relative(placementAxis, i)).canBeReplaced()) {
                        canConnect = false;
                        break;
                    }
                }
            }
        }

        if(!player.isCreative()) {
            var item = stack.getItem();
            var count = Math.abs(length) + 1;
            var hasItems = PlayerUtilities.hasEnoughItems(player, item, count);
            PlacementOverlay.setItemRequirement(item, count, hasItems);
        }

        var start = Vec3.atLowerCornerOf(firstPos);
        var heading = Direction.fromAxisAndDirection(placementAxis, length > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        for (float f = 0; f < Math.abs(length); f += .0625f) {
            Vec3 position = start.relative(heading, f);
            if (r.nextInt(10) == 0) {
                world.addParticle(
                        new DustParticleOptions(new Vector3f(canConnect ? .3f : .9f, canConnect ? .9f : .3f, .5f), 1),
                        position.x + .5f, position.y + .5f, position.z + .5f, 0, 0, 0);
            }
        }
    }

    private static float randomOffset(float range) {
        return (r.nextFloat() - .5f) * 2 * range;
    }
}
