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

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class TransformerBlock extends ElectricBlock {
    public static final IntegerProperty COILS = IntegerProperty.create("coils", 0, 2);
    private final int maxTurns;

    public TransformerBlock(Properties settings, int maxTurns) {
        super(settings);
        this.maxTurns = maxTurns;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ModdedBlocks.TRANSFORMER_CORE.get().getCloneItemStack(level, pos, state);
    }

    public abstract Optional<TransformerBlockEntity> getBlockEntity(Level world, BlockPos pos, BlockState state);
    protected abstract boolean isInitiator(BlockPos pos, BlockState state, BlockPos initiator);

    @Override
    public ElectricBehaviour getBehaviour(Level world, BlockPos pos, BlockState state) {
        var be = getBlockEntity(world, pos, state);
        return be.map(ElectricBlockEntity::getElectricBehaviour).orElse(null);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(!(be instanceof TransformerBlockEntity tbe))
            return super.getDrops(state, params);
        var drops = new ArrayList<>(super.getDrops(state, params));
        int turns = 0;
        if(tbe.getSecondary() != null) {
            turns += tbe.getSecondary().getTurns();
        }
        if(tbe.getPrimary() != null) {
            turns += tbe.getPrimary().getTurns();
        }
        for(; turns > 0; turns -= 64) {
            var stack = ModdedItems.WIRE.asStack(Math.min(turns, 64));
            drops.add(stack);
        }
        return drops;
    }

    public InteractionResult onWinding(BlockState state, UseOnContext context) {
        var pos = context.getClickedPos();
        var terminal = terminalIndexAt(state, context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
        var stack = context.getItemInHand();
        var nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        var turns = nbt.getInt("Turns");
        return getBlockEntity(context.getLevel(), context.getClickedPos(), state).map(be -> {
            if(terminal >= 0) {
                // Make coil between selected terminals.
                var firstTerminal = nbt.getInt("Terminal");
                if(terminal == firstTerminal) {
                    IElectric.sendMessage(context, Lang.translate("message.coil_same_terminal").style(ChatFormatting.RED).component());
                    return InteractionResult.FAIL;
                }
                var player = context.getPlayer();
                if(!PlayerUtilities.hasEnoughItems(player, stack, turns)) {
                    IElectric.sendMessage(context, Lang.translate("message.coil_missing_items").style(ChatFormatting.RED).component());
                    return InteractionResult.FAIL;
                }

                // Validate if the given amount of turns can fit on this transformer
                if(be.hasPrimary()) {
                    if(turns + be.getPrimary().getTurns() > maxTurns) {
                        IElectric.sendMessage(context, Lang.translate("message.coil_max_turns").style(ChatFormatting.RED).component());
                        return InteractionResult.FAIL;
                    }
                } else {
                    if(turns > maxTurns) {
                        IElectric.sendMessage(context, Lang.translate("message.coil_max_turns").style(ChatFormatting.RED).component());
                        return InteractionResult.FAIL;
                    }
                }

                if(!context.getLevel().isClientSide) {
                    if(be.hasPrimary()) {
                        be.makeSecondary(firstTerminal, terminal, turns, stack.getItem());
                    } else {
                        be.makePrimary(firstTerminal, terminal, turns, stack.getItem());
                    }
                    PlayerUtilities.removeItems(player, stack, turns);
                    stack.set(DataComponents.CUSTOM_DATA, null);
                }
                return InteractionResult.SUCCESS;
            } else {
                if(context.getLevel().isClientSide) {
                    var cap = be.hasPrimary() ? be.getPrimary().getTurns() : 0;
                    var b = TransformerWindingScreen.beginInteraction(() -> new TransformerWindingScreen(this, context.getHand(), turns, cap));
                    return b ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
                }
                return InteractionResult.SUCCESS;
            }
        }).orElse(InteractionResult.FAIL);
    }

    @Override
    public InteractionResult onWire(BlockState state, UseOnContext context) {
        var stack = context.getItemInHand();
        // Check if wire is in winding mode.
        if(stack.has(DataComponents.CUSTOM_DATA)) {
            var nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if(nbt.contains("Turns")) {
                var posArray = nbt.getIntArray("Initiator");
                var initiatorPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
                if(isInitiator(context.getClickedPos(), state, initiatorPosition)) {
                    return onWinding(state, context);
                }
                return InteractionResult.FAIL;
            }
        }
        // Not in winding mode, regular wire terminal check.
        var result = super.onWire(state, context);
        if(result == InteractionResult.PASS) {
            // Not hit a terminal.
            if(stack.has(DataComponents.CUSTOM_DATA) && stack.get(DataComponents.CUSTOM_DATA).contains("Connection")) {
                if(!stack.is(ModdedItems.WIRE.get())) {
                    // Only copper wire can be used to wind transformers
                    return InteractionResult.FAIL;
                }
                // Has first terminal data.
                return getBlockEntity(context.getLevel(), context.getClickedPos(), state).map(be -> {
                    var nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("Connection");
                    var endpoint = WireEndpointType.deserialize(nbt);
                    if(endpoint.type() != WireEndpointType.BLOCK)
                        return InteractionResult.FAIL;
                    var blockEndpoint = (BlockWireEndpoint) endpoint;
                    if(be.isTerminalUsed(blockEndpoint.getTerminal())) {
                        IElectric.sendMessage(context, Lang.translate("message.coil_exists").style(ChatFormatting.RED).component());
                        return InteractionResult.FAIL;
                    }
                    if(isInitiator(context.getClickedPos(), state, blockEndpoint.getPos())) {
                        // Put into winding mode.
                        if(context.getLevel().isClientSide) {
                            var cap = be.hasPrimary() ? be.getPrimary().getTurns() : 0;
                            var b = TransformerWindingScreen.beginInteraction(() -> new TransformerWindingScreen(this, context.getHand(), 1, cap));
                            return b ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
                        }
                        return InteractionResult.SUCCESS;
                    }
                    return InteractionResult.PASS;
                }).orElse(InteractionResult.FAIL);
            }
        }
        return result;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if(stack.is(ModdedItems.WIRE_CUTTER.get()) && !world.isClientSide) {
            var be = getBlockEntity(world, pos, state);
            if(be.isEmpty())
                return InteractionResult.FAIL;
            if(be.get().hasSecondary()) {
                if(!player.isCreative()) {
                    var coil = be.get().getSecondary();
                    var item = coil.getItem();
                    var count = coil.getTurns();
                    for (int items = count; items > 0; items -= 64) {
                        player.addItem(new ItemStack(item, Math.min(64, items)));
                    }
                }
                be.get().removeSecondary();
                return InteractionResult.SUCCESS;
            } else if(be.get().hasPrimary()) {
                if(!player.isCreative()) {
                    var coil = be.get().getPrimary();
                    var item = coil.getItem();
                    var count = coil.getTurns();
                    for (int items = count; items > 0; items -= 64) {
                        player.addItem(new ItemStack(item, Math.min(64, items)));
                    }
                }
                be.get().removePrimary();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    public int getMaxTurns() {
        return maxTurns;
    }
}
