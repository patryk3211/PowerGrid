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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.Lang;

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
        if(context.getStack().hasNbt()) {
            // This will result in the connection being a block wire (instead of a hanging wire)
            var tag = context.getStack().getNbt();
            NbtList list;
            Vec3d lastPoint;
            if(tag.contains("Segments")) {
                list = tag.getList("Segments", NbtElement.COMPOUND_TYPE);

                var lastPointList = tag.getList("LastPoint", NbtElement.FLOAT_TYPE);
                lastPoint = new Vec3d(
                        lastPointList.getFloat(0),
                        lastPointList.getFloat(1),
                        lastPointList.getFloat(2)
                );
            } else {
                list = new NbtList();
                tag.put("Segments", list);

                var posArray = tag.getIntArray("Position");
                var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                var firstTerminal = tag.getInt("Terminal");
                lastPoint = IElectric.getTerminalPos(firstPosition, context.getWorld().getBlockState(firstPosition), firstTerminal);
                lastPoint = BlockTrace.alignPosition(lastPoint);
            }

            var hitPoint = context.getHitPos();
            var newPoints = BlockTrace.findPath(context.getWorld(), lastPoint, hitPoint, null);
            if(newPoints != null) {
                newPoints.forEach(point -> list.add(point.serialize()));

                var lastPointList = new NbtList();
                lastPointList.add(NbtFloat.of((float) hitPoint.x));
                lastPointList.add(NbtFloat.of((float) hitPoint.y));
                lastPointList.add(NbtFloat.of((float) hitPoint.z));
                tag.put("LastPoint", lastPointList);

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
