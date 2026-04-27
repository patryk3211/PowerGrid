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
package org.patryk3211.powergrid.electricity.resistor;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;

import java.util.List;

public class ResistorBlock extends AbstractResistorBlock implements IBE<ResistorBlockEntity>, IHaveElectricProperties {
    public ResistorBlock(Properties settings) {
        super(settings);
    }

    @Override
    public Class<ResistorBlockEntity> getBlockEntityClass() {
        return ResistorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ResistorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.RESISTOR.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Power.max(stack, player, tooltip);
    }
}
