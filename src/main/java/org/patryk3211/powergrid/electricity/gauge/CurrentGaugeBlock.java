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
package org.patryk3211.powergrid.electricity.gauge;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;

import java.util.List;

public class CurrentGaugeBlock extends GaugeBlock<CurrentGaugeBlockEntity> implements IHaveElectricProperties {

    public CurrentGaugeBlock(Properties settings) {
        super(settings);
    }

    @Override
    public Class<CurrentGaugeBlockEntity> getBlockEntityClass() {
        return CurrentGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CurrentGaugeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CURRENT_METER.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Current.max(20, player, tooltip);
    }
}
