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
package org.patryk3211.powergrid.electricity.battery;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.block.entity.BlockEntityType;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;

public class BatteryBlock extends ElectricBlock implements IBE<BatteryBlockEntity>, IAcceptConnector {
    protected BatterySpec spec;

    public BatteryBlock(Settings settings) {
        super(settings);
    }

    public static <T extends BatteryBlock, P> NonNullUnaryOperator<BlockBuilder<T, P>> setSpec(BatterySpec spec) {
        return b -> b.onRegister(block -> block.spec = spec);
    }

    public BatterySpec getSpec() {
        return spec;
    }

    @Override
    public Class<BatteryBlockEntity> getBlockEntityClass() {
        return BatteryBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BatteryBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.BATTERY.get();
    }

    @Override
    public boolean isPolarized() {
        return true;
    }
}
