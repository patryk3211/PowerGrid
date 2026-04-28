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
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.utility.ClientSideAccess;

import java.util.List;

public class CircuitSchematicItem extends Item {
    public CircuitSchematicItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if(user.isCreative() && user.isShiftKeyDown()) {
            var block = new ItemStack(ModdedBlocks.CIRCUIT_BOARD, 1);
            block.set(DataComponents.CUSTOM_DATA, user.getItemInHand(hand).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
            block.remove(DataComponents.CUSTOM_NAME);
            return InteractionResultHolder.success(block);
        } else {
            return super.use(world, user, hand);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var player = EnvExecutor.getInEnv(Env.CLIENT, () -> ClientSideAccess::player)
                .map(Player::isCreative)
                .orElse(false);
        if(tooltipFlag.isCreative() || player) {
            tooltipComponents.add(Component.translatable(getDescriptionId() + ".tooltip.creative")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }
}
