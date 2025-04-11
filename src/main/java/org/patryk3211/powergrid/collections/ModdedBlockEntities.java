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
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.GaugeRenderer;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.heater.HeaterBlockEntity;
import org.patryk3211.powergrid.electricity.wireconnector.ConnectorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.GeneratorBlockEntity;

public class ModdedBlockEntities {
    public static final Registrate REGISTRATE = Registrate.create(PowerGrid.MOD_ID);

    public static final BlockEntityEntry<GeneratorBlockEntity> GENERATOR =
            REGISTRATE.blockEntity("generator", GeneratorBlockEntity::new)
                    .validBlock(ModdedBlocks.GENERATOR)
                    .register();

    public static final BlockEntityEntry<ConnectorBlockEntity> WIRE_CONNECTOR =
            REGISTRATE.blockEntity("wire_connector", ConnectorBlockEntity::new)
                    .validBlock(ModdedBlocks.WIRE_CONNECTOR)
                    .register();

    public static final BlockEntityEntry<BatteryBlockEntity> BATTERY =
            REGISTRATE.blockEntity("battery", BatteryBlockEntity::new)
                    .validBlock(ModdedBlocks.BATTERY)
                    .register();

    public static final BlockEntityEntry<VoltageGaugeBlockEntity> VOLTAGE_METER =
            REGISTRATE.blockEntity("voltage_meter", VoltageGaugeBlockEntity::new)
                    .validBlock(ModdedBlocks.VOLTAGE_METER)
                    .renderer(() -> GaugeRenderer::new)
                    .register();

    public static final BlockEntityEntry<HeaterBlockEntity> HEATING_COIL =
            REGISTRATE.blockEntity("heating_coil", HeaterBlockEntity::new)
                    .validBlock(ModdedBlocks.HEATING_COIL)
                    .register();
}
