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
package org.patryk3211.powergrid.electricity.wire;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import static org.patryk3211.powergrid.electricity.base.IElectric.sendMessage;

public class WireItem extends Item implements IWire {
    protected float resistance;
    protected float maxLength;

    protected Identifier wireTexture;
    protected float horizontalCoefficient = 1.01f;
    protected float verticalCoefficient = 1.2f;
    protected float wireThickness = 1 / 16f;

    public WireItem(Settings settings) {
        super(settings);
        resistance = 0.1f;
        maxLength = 16f;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getPlayer() != null && context.getPlayer().isSneaking())
            return super.useOnBlock(context);

        var blockState = context.getWorld().getBlockState(context.getBlockPos());
        if(blockState.getBlock() instanceof IElectric electric) {
            var result = electric.onWire(blockState, context);
            if(result != ActionResult.PASS)
                return result;
        }
        var tag = context.getStack().getNbt();
        if(tag != null) {
            // This will result in the connection being a block wire (instead of a hanging wire)
            var world = context.getWorld();
            var stack = context.getStack();
            var endpoint = WireEndpointType.deserialize(tag);
            if(endpoint == null)
                return ActionResult.FAIL;
            var lastPoint = endpoint.getExactPosition(world);

            var hitPoint = context.getHitPos();
            var result = BlockTrace.findPath(context.getWorld(), lastPoint, hitPoint, null);
            if(result != null && result.reachedTarget()) {
                float addedLength = 0;
                for(var point : result.points())
                    addedLength += point.length();

                if(endpoint.type() != WireEndpointType.BLOCK_WIRE) {
                    // New entity must be created.
                    var newItems = (int) Math.ceil(addedLength);
                    if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, newItems)) {
                        sendMessage(context, Lang.translate("message.connection_missing_items").style(Formatting.RED).component());
                        return ActionResult.FAIL;
                    }
                    if(!world.isClient) {
                        var entity = BlockWireEntity.create(world, endpoint, stack.copyWithCount(newItems), result.points());
                        if(!((ServerWorld) world).spawnNewEntityAndPassengers(entity)) {
                            PowerGrid.LOGGER.error("Failed to spawn new block wire entity.");
                            sendMessage(context, Lang.translate("message.connection_failed").style(Formatting.RED).component());
                            return ActionResult.FAIL;
                        }
                        PlayerUtilities.removeItems(context.getPlayer(), stack, newItems);
                        endpoint = new BlockWireEntityEndpoint(entity, true);
                        stack.setNbt(endpoint.serialize());
                        context.getPlayer().setStackInHand(context.getHand(), stack);
                    }
                } else {
                    // Entity exists, we just need to extend it.
                    var bwEndpoint = (BlockWireEntityEndpoint) endpoint;
                    var wire = bwEndpoint.getEntity(world);
                    var newItems = (int) Math.ceil(wire.getTotalLength() + addedLength - wire.getWireCount());
                    if(!PlayerUtilities.hasEnoughItems(context.getPlayer(), stack, newItems)) {
                        IElectric.sendMessage(context, Lang.translate("message.connection_missing_items").style(Formatting.RED).component());
                        return ActionResult.FAIL;
                    }

                    if(!context.getWorld().isClient) {
                        if(!bwEndpoint.getEnd()) {
                            PowerGrid.LOGGER.error("Cannot extend wire at start (must be flipped beforehand)");
                            return ActionResult.FAIL;
                        }
                        wire.extend(result.points(), newItems);
                        PlayerUtilities.removeItems(context.getPlayer(), stack, newItems);

                        endpoint = new BlockWireEntityEndpoint(wire, true);
                        stack.setNbt(endpoint.serialize());
                        context.getPlayer().setStackInHand(context.getHand(), stack);
                    }
                }

                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return super.hasGlint(stack) || stack.hasNbt();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if(stack.hasNbt() && user.isSneaking()) {
            stack.setNbt(null);
            if(!world.isClient)
                user.sendMessage(Lang.translate("message.connection_reset").style(Formatting.GRAY).component(), true);
            return TypedActionResult.success(stack, true);
        }
        return super.use(world, user, hand);
    }

    @Override
    public float getResistance() {
        return resistance;
    }

    @Override
    public float getMaximumLength() {
        return maxLength;
    }

    @Environment(EnvType.CLIENT)
    public Identifier getWireTexture() {
        return wireTexture;
    }

    @Environment(EnvType.CLIENT)
    public float getHorizontalCoefficient() {
        return horizontalCoefficient;
    }

    @Environment(EnvType.CLIENT)
    public float getVerticalCoefficient() {
        return verticalCoefficient;
    }

    @Environment(EnvType.CLIENT)
    public float getWireThickness() {
        return wireThickness;
    }
}
