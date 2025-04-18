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

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Voltage;

import java.util.List;

public class VoltageGaugeBlock extends GaugeBlock<VoltageGaugeBlockEntity> implements IHaveElectricProperties {
    public VoltageGaugeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Class<VoltageGaugeBlockEntity> getBlockEntityClass() {
        return VoltageGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VoltageGaugeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.VOLTAGE_METER.get();
    }

    @Override
    public void appendProperties(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        Voltage.max(maxValue, player, tooltip);
    }
}
