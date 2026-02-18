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
package org.patryk3211.powergrid.kinetics.punchcard;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedMenus;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class PunchCardItem extends Item implements MenuProvider {
    public PunchCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() == null)
            return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if(!player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if(!world.isClientSide && player instanceof ServerPlayer serverPlayer)
                MenuRegistry.openExtendedMenu(serverPlayer, this, buf -> buf.writeItem(stack));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Punch Card");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if(!stack.has(DataComponents.CUSTOM_DATA))
            return;
        if(stack.get(DataComponents.CUSTOM_DATA).copyTag().getBoolean("Locked")) {
            var author = stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("Author");
            if(author.isEmpty())
                return;
            var line = Lang.translate("gui.punch_card.author")
                    .add(Component.literal(author)).style(ChatFormatting.GRAY);
            tooltipComponents.add(line.component());
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        ItemStack heldItem = player.getMainHandItem();
        return PunchCardMenu.CONSTRUCTORS.create(ModdedMenus.PUNCH_CARD.get(), i, inventory, heldItem);
    }
}
