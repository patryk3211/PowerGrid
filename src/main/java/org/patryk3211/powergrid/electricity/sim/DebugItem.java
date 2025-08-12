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
package org.patryk3211.powergrid.electricity.sim;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;

public class DebugItem extends Item {
    public DebugItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var opt = world.getBlockEntity(context.getClickedPos(), ModdedBlockEntities.WIRE_CONNECTOR.get());
        return opt.map(be -> {
            GlobalElectricNetworks.inspect(be.getElectricBehaviour().getTerminal(0), context.getPlayer());
            return InteractionResult.SUCCESS;
        }).orElse(InteractionResult.FAIL);
    }
}
