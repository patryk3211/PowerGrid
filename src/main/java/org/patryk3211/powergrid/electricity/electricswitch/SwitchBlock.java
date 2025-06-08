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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.wire.IWire;

import java.util.List;

public abstract class SwitchBlock extends ElectricBlock implements IBE<SwitchBlockEntity>, IHaveElectricProperties {
    public static final BooleanProperty OPEN = Properties.OPEN;

    protected float resistance = 0.01f;
    protected float maxVoltage = 200f;

    public SwitchBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OPEN, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPEN);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!player.isSneaking()) {
            if(!IWire.holdsWire(player)) {
                var isOpen = !state.get(OPEN);
                world.setBlockState(pos, state.with(OPEN, isOpen));
                if(world.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
                    entity.setState(!isOpen);
                }
                useSound(world, pos, isOpen);
                return ActionResult.SUCCESS;
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    public void useSound(World world, BlockPos pos, boolean open) {

    }

    @Override
    public Class<SwitchBlockEntity> getBlockEntityClass() {
        return SwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SwitchBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SWITCH.get();
    }

    public float getResistance() {
        return resistance;
    }

    public float getMaxVoltage() {
        return maxVoltage;
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(getResistance(), player, tooltip);
    }
}
