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
package org.patryk3211.powergrid.collections;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.battery.BatteryBlock;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlock;
import org.patryk3211.powergrid.electricity.heater.HeaterBlock;
import org.patryk3211.powergrid.electricity.wireconnector.ConnectorBlock;
import org.patryk3211.powergrid.kinetics.basicgenerator.BasicGeneratorBlock;

public class ModdedBlocks {
    public static final Registrate REGISTRATE = Registrate.create(PowerGrid.MOD_ID)
            .defaultCreativeTab(PowerGrid.ITEM_GROUP_KEY);

    public static final BlockEntry<BasicGeneratorBlock> BASIC_GENERATOR = BasicGeneratorBlock.register(REGISTRATE);
    public static final BlockEntry<ConnectorBlock> WIRE_CONNECTOR = ConnectorBlock.register(REGISTRATE);
    public static final BlockEntry<BatteryBlock> BATTERY = BatteryBlock.register(REGISTRATE);
    public static final BlockEntry<VoltageGaugeBlock> VOLTAGE_METER = VoltageGaugeBlock.register(REGISTRATE);
    public static final BlockEntry<HeaterBlock> HEATING_COIL = HeaterBlock.register(REGISTRATE);
}
