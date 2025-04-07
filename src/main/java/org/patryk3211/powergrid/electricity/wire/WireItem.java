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

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.patryk3211.powergrid.electricity.base.IElectric;

public class WireItem extends Item implements IWire {
    public WireItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var blockState = context.getWorld().getBlockState(context.getBlockPos());
        if(blockState.getBlock() instanceof IElectric electric) {
            return electric.onWire(blockState, context);
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
            return TypedActionResult.success(stack, true);
        }
        return super.use(world, user, hand);
    }

    public static ItemEntry<WireItem> register(Registrate registrate) {
        return registrate.item("wire", WireItem::new)
                .register();
    }

    @Override
    public float getResistance() {
        return 0.01f;
    }
}
