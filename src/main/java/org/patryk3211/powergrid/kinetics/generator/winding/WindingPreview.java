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
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.patryk3211.powergrid.utility.PlacementOverlay;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.Random;

import static net.minecraft.state.property.Properties.AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingItem.getPlacementAxis;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingItem.getPlacementDelta;

@Environment(EnvType.CLIENT)
public class WindingPreview {
    private static final Random r = new Random();

    @Nullable
    public static ItemStack getUsedWireStack(PlayerEntity player) {
        var stack1 = player.getMainHandStack();
        var stack2 = player.getOffHandStack();
        if(stack1 != null && stack1.getItem() instanceof WindingItem && stack1.hasNbt()) {
            return stack1;
        } else if(stack2 != null && stack2.getItem() instanceof WindingItem && stack2.hasNbt()) {
            return stack2;
        } else {
            return null;
        }
    }

    public static void tick() {
        var player = MinecraftClient.getInstance().player;
        var world = MinecraftClient.getInstance().world;
        if(player == null || world == null)
            return;
        var stack = getUsedWireStack(player);
        if(stack == null)
            return;

        var tag = stack.getNbt();
        var posArray = tag.getIntArray("Position");
        if(posArray.length < 3)
            return;
        var firstPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        var firstState = world.getBlockState(firstPos);
        if(!ShaftBlock.isShaft(firstState))
            return;

        var rayTrace = MinecraftClient.getInstance().crosshairTarget;
        if(!(rayTrace instanceof BlockHitResult hit)) {
            if(r.nextInt(50) == 0) {
                world.addParticle(new DustParticleEffect(new Vector3f(.3f, .9f, .5f), 1),
                        firstPos.getX() + .5f + randomOffset(.25f), firstPos.getY() + .5f + randomOffset(.25f),
                        firstPos.getZ() + .5f + randomOffset(.25f), 0, 0, 0);
            }
            return;
        }

        var selected = hit.getBlockPos();
        var selectedState = world.getBlockState(selected);

       if (!ShaftBlock.isShaft(selectedState))
            selected = selected.offset(hit.getSide());

        boolean canConnect = ShaftBlock.isShaft(selectedState) && firstState.get(AXIS) == selectedState.get(AXIS);
        var placementAxis = getPlacementAxis(selected, firstPos);
        if(placementAxis == firstState.get(AXIS))
            canConnect = false;

        var length = getPlacementDelta(selected, firstPos);
        if(canConnect) {
            // Verify the winding can be placed
            if(length > 0) {
                for(int i = 1; i < length; ++i) {
                    if(!world.getBlockState(firstPos.offset(placementAxis, i)).isReplaceable()) {
                        canConnect = false;
                        break;
                    }
                }
            } else {
                for(int i = -1; i > length; --i) {
                    if(!world.getBlockState(firstPos.offset(placementAxis, i)).isReplaceable()) {
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

        var start = Vec3d.of(firstPos);
        var heading = Direction.from(placementAxis, length > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        for (float f = 0; f < Math.abs(length); f += .0625f) {
            Vec3d position = start.offset(heading, f);
            if (r.nextInt(10) == 0) {
                world.addParticle(
                        new DustParticleEffect(new Vector3f(canConnect ? .3f : .9f, canConnect ? .9f : .3f, .5f), 1),
                        position.x + .5f, position.y + .5f, position.z + .5f, 0, 0, 0);
            }
        }
    }

    private static float randomOffset(float range) {
        return (r.nextFloat() - .5f) * 2 * range;
    }
}
