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

import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.IncompleteCircuitItem;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematicItem;
import org.patryk3211.powergrid.config.CWire;
import org.patryk3211.powergrid.electricity.light.bulb.GrowthLamp;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulb;
import org.patryk3211.powergrid.electricity.light.bulb.LvLightBulb;
import org.patryk3211.powergrid.electricity.sim.DebugItem;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.WireProperties;
import org.patryk3211.powergrid.electricity.wire.powercord.CordItem;
import org.patryk3211.powergrid.electricity.light.string.StringLightCordItem;
import org.patryk3211.powergrid.equipment.ZincArmorMaterial;
import org.patryk3211.powergrid.equipment.baton.ElectroBatonItem;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItem;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItemRenderer;
import org.patryk3211.powergrid.equipment.portablebattery.PortableBatteryItem;
import org.patryk3211.powergrid.equipment.zapper.ElectroZapperItem;
import org.patryk3211.powergrid.equipment.zapper.ElectroZapperItemRenderer;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingItem;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem;

import java.util.function.Supplier;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;
import static org.patryk3211.powergrid.collections.ModdedTags.*;
import static org.patryk3211.powergrid.utility.DataProviderUtility.barrier;
import static org.patryk3211.powergrid.utility.DataProviderUtility.itemWithParent;

public class ModdedItems {
    public static final ItemEntry<WireItem> WIRE = REGISTRATE.item("wire", WireItem::new)
            .transform(CWire.set(0.0015f, 16, 1.0f, 80))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/copper_wire"), 1.01f, 1.2f, 0.0625f))
            .tag(ModdedTags.Item.WIRES.tag, ModdedTags.Item.LIGHT_WIRES.tag, wires("copper"))
            .register();
    public static final ItemEntry<WireItem> IRON_WIRE = REGISTRATE.item("iron_wire", WireItem::new)
            .transform(CWire.set(0.005f, 32, 2.0f, 160))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/iron_wire"), 1.0075f, 1.125f, 0.125f))
            .tag(ModdedTags.Item.WIRES.tag, ModdedTags.Item.FUSE_RESETTING.tag, wires("iron"))
            .register();
    public static final ItemEntry<WireItem> GOLDEN_WIRE = REGISTRATE.item("golden_wire", WireItem::new)
            .transform(CWire.set(0.003f, 8, 0.8f, 160))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/golden_wire"), 1.02f, 1.4f, 0.0625f))
            .tag(ModdedTags.Item.WIRES.tag, ModdedTags.Item.LIGHT_WIRES.tag, wires("gold"))
            .register();
    public static final ItemEntry<WireItem> INSULATED_COPPER_WIRE = REGISTRATE.item("insulated_copper_wire", WireItem::new)
            .transform(CWire.set(0.0015f, 16, 1.2f, 70))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/insulated_wire"), 1.01f, 1.2f, 0.0625f, true))
            .tag(ModdedTags.Item.WIRES.tag, ModdedTags.Item.LIGHT_WIRES.tag)
            .register();
    public static final ItemEntry<CordItem> CORD = REGISTRATE.item("copper_cord", CordItem::new)
            .transform(CWire.set(0.0015f, 8, 2.0f, 60))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/insulated_wire"), 1.005f, 1.005f, 0.125f, true))
            .register();
    public static final ItemEntry<StringLightCordItem> STRING_LIGHT_CORD = REGISTRATE.item("string_light_cord", StringLightCordItem::new)
            .transform(CWire.set(0.0015f, 8, 2.0f, 60))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/insulated_wire"), 1.005f, 1.005f, 0.125f))
            .register();

    public static final ItemEntry<Item> WIRE_CUTTER = REGISTRATE.item("wire_cutter", Item::new)
            .register();

    public static final ItemEntry<Item> EMPTY_CIRCUIT = REGISTRATE.item("empty_circuit", Item::new)
            .register();

    public static final ItemEntry<DebugItem> DEBUG_ITEM = REGISTRATE.item("debug", DebugItem::new).register();

    public static final ItemEntry<LvLightBulb> LV_LIGHT_BULB = REGISTRATE.item("lv_light_bulb", LvLightBulb::new)
            .transform(LightBulb.setModelNameProvider(() -> state -> PowerGrid.asResource(switch(state) {
                case OFF -> "block/lamps/light_bulb";
                case LOW_POWER, ON -> "block/lamps/light_bulb_on";
                case BROKEN -> "block/lamps/light_bulb_broken";
                case LIGHT -> "block/lamps/light_bulb_light";
            })))
            .transform(LightBulb.setDyedModelNameProvider(() -> state -> PowerGrid.asResource(switch(state) {
                case OFF -> "block/lamps/dyed_light_bulb";
                case LOW_POWER, ON -> "block/lamps/dyed_light_bulb_on";
                case BROKEN -> "block/lamps/dyed_light_bulb_broken";
                case LIGHT -> "block/lamps/dyed_light_bulb_light";
                case BULB -> "block/lamps/dyed_light_bulb_bulb";
            })))
            .transform(LightBulb.setProperties(3, 12, 20, 0.001f))
            .model(itemWithParent("block/lamps/light_bulb"))
            .lang("LV Light Bulb")
            .register();

    public static final ItemEntry<LightBulb> LIGHT_BULB = REGISTRATE.item("light_bulb", LightBulb::new)
            .transform(LightBulb.setModelNameProvider(() -> state -> PowerGrid.asResource(switch(state) {
                case OFF -> "block/lamps/light_bulb";
                case LOW_POWER, ON -> "block/lamps/light_bulb_on";
                case BROKEN -> "block/lamps/light_bulb_broken";
                case LIGHT -> "block/lamps/light_bulb_light";
            })))
            .transform(LightBulb.setDyedModelNameProvider(() -> state -> PowerGrid.asResource(switch(state) {
                case OFF -> "block/lamps/dyed_light_bulb";
                case LOW_POWER, ON -> "block/lamps/dyed_light_bulb_on";
                case BROKEN -> "block/lamps/dyed_light_bulb_broken";
                case LIGHT -> "block/lamps/dyed_light_bulb_light";
                case BULB -> "block/lamps/dyed_light_bulb_bulb";
            })))
            .transform(LightBulb.setProperties(30, 120, 120, 0.005f))
            .model(itemWithParent("block/lamps/light_bulb"))
            .register();

    public static final ItemEntry<GrowthLamp> GROWTH_LAMP = REGISTRATE.item("growth_lamp", GrowthLamp::new)
            .transform(LightBulb.setModelNameProvider(() -> state -> PowerGrid.asResource(switch(state) {
                case OFF -> "block/lamps/growth_lamp";
                case LOW_POWER, ON -> "block/lamps/growth_lamp_on";
                case BROKEN -> "block/lamps/growth_lamp_broken";
                case LIGHT -> "block/lamps/growth_lamp_light";
            })))
            .transform(LightBulb.setProperties(120, 240, 120, 0.01f))
            .model(itemWithParent("block/lamps/growth_lamp"))
            .register();

    public static final ItemEntry<Item> RESISTIVE_COIL = ingredient("resistive_coil", forgeItemTag("iron_coils"), ModdedTags.Item.COILS.tag);
    public static final ItemEntry<WindingItem> COPPER_COIL = REGISTRATE.item("copper_coil", WindingItem::new)
            .tag(forgeItemTag("copper_coils"), ModdedTags.Item.COILS.tag)
            .register();
    public static final ItemEntry<Item> MAGNET = ingredient("magnet");

    public static final ItemEntry<Item> INTEGRATED_CIRCUIT = ingredient("integrated_circuit");
    public static final ItemEntry<Item> ELECTRICAL_GIZMO = ingredient("electrical_gizmo");
    public static final ItemEntry<Item> ZINC_SHEET = ingredient("zinc_sheet", ModdedTags.Item.PLATES.tag, plates("zinc"));

    public static final ItemEntry<Item> RELAY = ingredient("relay");
    public static final ItemEntry<Item> RELAY_DPDT = REGISTRATE.item("relay_dpdt", Item::new)
            .lang("Double Pole Relay")
            .register();
    public static final ItemEntry<Item> RESISTOR = ingredient("resistor");
    public static final ItemEntry<Item> REDSTONE_RELAY = ingredient("redstone_relay");
    public static final ItemEntry<Item> DIODE = ingredient("diode");
    public static final ItemEntry<Item> VFET = REGISTRATE.item("vfet", Item::new)
            .lang("Static Induction Transistor")
            .register();
    public static final ItemEntry<Item> BJT_NPN = REGISTRATE.item("bjt_npn", Item::new)
            .lang("NPN BJT")
            .register();
    public static final ItemEntry<Item> BJT_PNP = REGISTRATE.item("bjt_pnp", Item::new)
            .lang("PNP BJT")
            .register();
    public static final ItemEntry<Item> CAPACITOR = ingredient("capacitor");
    public static final ItemEntry<Item> POTENTIOMETER = ingredient("potentiometer");
    public static final ItemEntry<Item> REGULATOR_TUBE = ingredient("regulator_tube");
    public static final ItemEntry<Item> NEON_BULB = ingredient("neon_bulb");
    public static final ItemEntry<Item> BARRETTER_TUBE = ingredient("barretter_tube");
    public static final ItemEntry<Item> VARISTOR = ingredient("varistor");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TRANSFORMER_CORE = sequencedIngredient("incomplete_transformer_core");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_ELECTRICAL_GIZMO = sequencedIngredient("incomplete_electrical_gizmo");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_UNETCHED_CIRCUIT = sequencedIngredientBuilder("incomplete_unetched_circuit")
            .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
            .register();
    public static final ItemEntry<IncompleteCircuitItem> INCOMPLETE_CIRCUIT = REGISTRATE.item("incomplete_circuit", IncompleteCircuitItem::new)
            .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
            .register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_BATTERY = sequencedIngredient("incomplete_battery");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_PUNCH_CARD = sequencedIngredient("incomplete_punch_card");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_BJT_NPN = sequencedIngredientBuilder("incomplete_bjt_npn")
            .lang("Incomplete NPN BJT")
            .register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_BJT_PNP = sequencedIngredientBuilder("incomplete_bjt_pnp")
            .lang("Incomplete PNP BJT")
            .register();

    public static final ItemEntry<ElectroZapperItem> ELECTROZAPPER = REGISTRATE.item("electrozapper", ElectroZapperItem::new)
            .transform(customRenderer(() -> ElectroZapperItemRenderer::new))
            .model(itemWithParent("item/electrozapper/item"))
            .lang("Electro-Zapper")
            .register();

    public static final ItemEntry<ElectroBatonItem> ELECTROBATON = REGISTRATE.item("electrobaton", ElectroBatonItem::new)
            .model(itemWithParent("item/electrobaton/item"))
            .lang("Electro-Baton")
            .register();

    public static final ItemEntry<BacktankItem.BacktankBlockItem> PORTABLE_BATTERY_PLACEABLE = REGISTRATE.item("portable_battery_placeable",
                    p -> new BacktankItem.BacktankBlockItem(ModdedBlocks.PORTABLE_BATTERY.get(), ModdedItems.PORTABLE_BATTERY::get, p))
            .model(barrier())
            .register();

    public static final ItemEntry<PortableBatteryItem> PORTABLE_BATTERY = REGISTRATE.item("portable_battery", p -> new PortableBatteryItem(ZincArmorMaterial.INSTANCE, p, PowerGrid.asResource("zinc"), PORTABLE_BATTERY_PLACEABLE))
            .model(itemWithParent("block/portable_battery/block"))
            .properties(p -> p.durability(-1))
			.tag(forgeItemTag("chestplates"))
            .register();

    public static final ItemEntry<CircuitSchematicItem> CIRCUIT_SCHEMATIC = REGISTRATE.item("circuit_schematic", CircuitSchematicItem::new)
            .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
            .register();

    public static final ItemEntry<Item> UNETCHED_CIRCUIT = ingredient("unetched_circuit", ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag);

    public static final ItemEntry<MultimeterItem> MULTIMETER = REGISTRATE.item("multimeter", MultimeterItem::new)
            .model(itemWithParent("item/multimeter/base"))
            .transform(customRenderer(() -> MultimeterItemRenderer::new))
            .register();

    public static final ItemEntry<PunchCardItem> PUNCH_CARD = REGISTRATE.item("punch_card", PunchCardItem::new)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    private static ItemEntry<SequencedAssemblyItem> sequencedIngredient(String name) {
        return REGISTRATE.item(name, SequencedAssemblyItem::new).register();
    }

    private static ItemBuilder<SequencedAssemblyItem, AbstractPowerGridRegistrate> sequencedIngredientBuilder(String name) {
        return REGISTRATE.item(name, SequencedAssemblyItem::new);
    }

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new).register();
    }

    @SafeVarargs
    private static ItemEntry<Item> ingredient(String name, TagKey<Item>... tags) {
        return REGISTRATE.item(name, Item::new).tag(tags).register();
    }

    @ExpectPlatform
    public static <T extends Item, P> NonNullUnaryOperator<ItemBuilder<T, P>> customRenderer(Supplier<Supplier<CustomRenderedItemModelRenderer>> renderer) {
        throw new AssertionError();
    }
}
