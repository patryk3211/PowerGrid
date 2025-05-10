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
package org.patryk3211.powergrid.electricity.transformer;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.Optional;

public abstract class TransformerBlock extends ElectricBlock {
    public static final IntProperty COILS = IntProperty.of("coils", 0, 2);
    private final int maxTurns;

    public TransformerBlock(Settings settings, int maxTurns) {
        super(settings);
        this.maxTurns = maxTurns;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return ModdedBlocks.TRANSFORMER_CORE.get().getPickStack(world, pos, state);
    }

    public abstract Optional<TransformerBlockEntity> getBlockEntity(World world, BlockPos pos, BlockState state);
    protected abstract boolean isInitiator(BlockPos pos, BlockState state, BlockPos initiator);

    @Override
    public ElectricBehaviour getBehaviour(World world, BlockPos pos, BlockState state) {
        var be = getBlockEntity(world, pos, state);
        return be.map(ElectricBlockEntity::getElectricBehaviour).orElse(null);
    }

    public ActionResult onWinding(BlockState state, ItemUsageContext context) {
        var pos = context.getBlockPos();
        var terminal = terminalIndexAt(state, context.getHitPos().subtract(pos.getX(), pos.getY(), pos.getZ()));
        var stack = context.getStack();
        var nbt = stack.getNbt();
        var turns = nbt.getInt("Turns");
        var be = getBlockEntity(context.getWorld(), context.getBlockPos(), state);
        if(be.isEmpty())
            return ActionResult.FAIL;
        if(terminal >= 0) {
            // Make coil between selected terminals.
            var firstTerminal = nbt.getInt("Terminal");
            if(terminal == firstTerminal) {
                IElectric.sendMessage(context, Lang.translate("message.coil_same_terminal").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
            var player = context.getPlayer();
            if(!PlayerUtilities.hasEnoughItems(player, stack, turns)) {
                IElectric.sendMessage(context, Lang.translate("message.coil_missing_items").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
            if(!context.getWorld().isClient) {
                if(be.get().hasPrimary()) {
                    be.get().makeSecondary(firstTerminal, terminal, turns, stack.getItem());
                } else {
                    be.get().makePrimary(firstTerminal, terminal, turns, stack.getItem());
                }
            }
            PlayerUtilities.removeItems(player, stack, turns);
            stack.setNbt(null);
            return ActionResult.SUCCESS;
        } else {
            var primaryTurns = be.get().hasPrimary() ? be.get().getPrimary().getTurns() : 0;
            if(primaryTurns + turns < maxTurns) {
                // Add turn.
                nbt.putInt("Turns", turns + 1);
                return ActionResult.SUCCESS;
            } else {
                // No more turns fit.
                IElectric.sendMessage(context, Lang.translate("message.coil_max_turns").style(Formatting.RED).component());
                return ActionResult.FAIL;
            }
        }
    }

    @Override
    public ActionResult onWire(BlockState state, ItemUsageContext context) {
        var stack = context.getStack();
        // Check if wire is in winding mode.
        if(stack.hasNbt()) {
            var nbt = stack.getNbt();
            if(nbt.contains("Turns")) {
                var posArray = nbt.getIntArray("Initiator");
                var initiatorPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                if(isInitiator(context.getBlockPos(), state, initiatorPosition)) {
                    return onWinding(state, context);
                }
                return ActionResult.FAIL;
            }
        }
        // Not in winding mode, regular wire terminal check.
        var result = super.onWire(state, context);
        if(result == ActionResult.PASS) {
            // Not hit a terminal.
            if(stack.hasNbt()) {
                // Has first terminal data.
                var be = getBlockEntity(context.getWorld(), context.getBlockPos(), state);
                if(be.isEmpty())
                    return ActionResult.FAIL;
                var nbt = stack.getNbt();
                if(be.get().isTerminalUsed(nbt.getInt("Terminal"))) {
                    IElectric.sendMessage(context, Lang.translate("message.coil_exists").style(Formatting.RED).component());
                    return ActionResult.FAIL;
                }
                var posArray = nbt.getIntArray("Position");
                var firstPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                if(isInitiator(context.getBlockPos(), state, firstPosition)) {
                    // Put into winding mode.
                    nbt.putInt("Turns", 1);
                    nbt.putIntArray("Initiator", posArray);
                    nbt.remove("Position");
                    return ActionResult.SUCCESS;
                }
            }
        }
        return result;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var stack = player.getStackInHand(hand);
        if(stack.isOf(ModdedItems.WIRE_CUTTER.get()) && !world.isClient) {
            var be = getBlockEntity(world, pos, state);
            if(be.isEmpty())
                return ActionResult.FAIL;
            if(be.get().hasSecondary()) {
                var coil = be.get().getSecondary();
                var item = coil.getItem();
                var count = coil.getTurns();
                for(int items = count; items > 0; items -= 64) {
                    player.giveItemStack(new ItemStack(item, Math.min(64, items)));
                }
                be.get().removeSecondary();
                return ActionResult.SUCCESS;
            } else if(be.get().hasPrimary()) {
                var coil = be.get().getPrimary();
                var item = coil.getItem();
                var count = coil.getTurns();
                for(int items = count; items > 0; items -= 64) {
                    player.giveItemStack(new ItemStack(item, Math.min(64, items)));
                }
                be.get().removePrimary();
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
}
