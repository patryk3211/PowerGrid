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
package org.patryk3211.powergrid.electricity.basinheater;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;

import java.util.List;

public class BasinHeaterBlock extends ElectricBlock implements IBE<BasinHeaterBlockEntity>, IAcceptConnector, IHaveElectricProperties {
    public static final EnumProperty<BlazeBurnerBlock.HeatLevel> HEAT_LEVEL = BlazeBurnerBlock.HEAT_LEVEL;

    public BasinHeaterBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HEAT_LEVEL);
    }

    @Override
    public boolean canConnect(WorldView world, BlockPos pos, BlockState state, Direction side) {
        return side != Direction.UP;
    }

    @Override
    public Class<BasinHeaterBlockEntity> getBlockEntityClass() {
        return BasinHeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BasinHeaterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.BASIN_HEATER.get();
    }

    public static float resistance() {
        return 10f;
    }

    public static float resistanceWorking() {
        // Slightly lower resistance when mixer is running.
        return 9.0f;
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Resistance.series(resistance(), player, tooltip);
    }
}
