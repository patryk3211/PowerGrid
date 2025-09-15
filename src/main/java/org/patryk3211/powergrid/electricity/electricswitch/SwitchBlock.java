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
package org.patryk3211.powergrid.electricity.electricswitch;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.wire.IWire;

import java.util.List;

public abstract class SwitchBlock extends ElectricBlock implements IBE<SwitchBlockEntity>, IHaveElectricProperties {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    protected float maxVoltage = 200f;
    protected boolean isButton = false;

    public SwitchBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    public boolean isButton() {
        return isButton;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!player.isShiftKeyDown()) {
            if(!IWire.holdsWire(player)) {
                var isOpen = !state.getValue(OPEN);
                if(!isButton) {
                    world.setBlockAndUpdate(pos, state.setValue(OPEN, isOpen));
                    withBlockEntityDo(world, pos, be -> be.setState(!isOpen));
                    useSound(world, pos, isOpen);
                } else {
                    if(!isOpen) {
                        world.setBlockAndUpdate(pos, state.setValue(OPEN, false));
                        useSound(world, pos, false);
                    }
                    withBlockEntityDo(world, pos, be -> be.setState(true));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, world, pos, player, hand, hit);
    }

    public void useSound(Level world, BlockPos pos, boolean open) {

    }

    @Override
    public Class<SwitchBlockEntity> getBlockEntityClass() {
        return SwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SwitchBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SWITCH.get();
    }

    public float getMaxVoltage() {
        return maxVoltage;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        Current.max(stack, player, tooltip);
        Voltage.max(maxVoltage, player, tooltip);
    }
}
