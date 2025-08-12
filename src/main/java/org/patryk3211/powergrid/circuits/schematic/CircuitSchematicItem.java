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
package org.patryk3211.powergrid.circuits.schematic;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlocks;

import java.util.List;

public class CircuitSchematicItem extends Item {
    public CircuitSchematicItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(user.isCreative() && user.isSneaking()) {
            var block = new ItemStack(ModdedBlocks.CIRCUIT_BOARD, 1);
            block.setNbt(user.getStackInHand(hand).getNbt());
            block.removeCustomName();
            return TypedActionResult.success(block);
        } else {
            return super.use(world, user, hand);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var player = EnvExecutor.getInEnv(Env.CLIENT, () -> () -> MinecraftClient.getInstance().player)
                .map(PlayerEntity::isCreative)
                .orElse(false);
        if(context.isCreative() || player) {
            tooltip.add(Text.translatable(getTranslationKey() + ".tooltip.creative")
                    .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        }
    }
}
