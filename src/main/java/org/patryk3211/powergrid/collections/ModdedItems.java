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
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.PowerGridRegistrate;
import org.patryk3211.powergrid.circuits.circuitboard.IncompleteCircuitItem;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematicItem;
import org.patryk3211.powergrid.electricity.baton.ElectroBatonItem;
import org.patryk3211.powergrid.electricity.light.bulb.GrowthLamp;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulb;
import org.patryk3211.powergrid.electricity.portablebattery.PortableBatteryItem;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.WireProperties;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperItem;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperItemRenderer;
import org.patryk3211.powergrid.equipment.ZincArmorMaterial;

import static com.simibubi.create.AllTags.forgeItemTag;
import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedItems {
    public static final ItemEntry<WireItem> WIRE = REGISTRATE.item("wire", WireItem::new)
            .transform(WireProperties.setAll(0.005f, 16))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/copper_wire"), 1.01f, 1.2f, 0.0625f))
            .tag(ModdedTags.Item.COIL_WIRE.tag, ModdedTags.Item.WIRES.tag, ModdedTags.Item.LIGHT_WIRES.tag)
            .register();
    public static final ItemEntry<WireItem> IRON_WIRE = REGISTRATE.item("iron_wire", WireItem::new)
            .transform(WireProperties.setAll(0.015f, 32))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/iron_wire"), 1.0075f, 1.125f, 0.125f))
            .tag(ModdedTags.Item.WIRES.tag)
            .register();
    public static final ItemEntry<WireItem> GOLDEN_WIRE = REGISTRATE.item("golden_wire", WireItem::new)
            .transform(WireProperties.setAll(0.007f, 8))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/golden_wire"), 1.02f, 1.4f, 0.0625f))
            .tag(ModdedTags.Item.WIRES.tag, ModdedTags.Item.LIGHT_WIRES.tag)
            .register();

    public static final ItemEntry<Item> WIRE_CUTTER = REGISTRATE.item("wire_cutter", Item::new)
            .register();

    public static final ItemEntry<Item> EMPTY_CIRCUIT = REGISTRATE.item("empty_circuit", Item::new)
            .register();

    public static final ItemEntry<LightBulb> LIGHT_BULB = REGISTRATE.item("light_bulb", LightBulb::new)
            .transform(LightBulb.setModelProvider(() -> state -> switch(state) {
                case OFF -> ModdedPartialModels.LIGHT_BULB_OFF;
                case LOW_POWER, ON -> ModdedPartialModels.LIGHT_BULB_ON;
                case BROKEN -> ModdedPartialModels.LIGHT_BULB_BROKEN;
            }))
            .transform(LightBulb.setProperties(30, 60, 30, 1450, 0.005f))
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/lamps/light_bulb")))
            .register();

    public static final ItemEntry<GrowthLamp> GROWTH_LAMP = REGISTRATE.item("growth_lamp", GrowthLamp::new)
            .transform(LightBulb.setModelProvider(() -> state -> switch(state) {
                case OFF -> ModdedPartialModels.GROWTH_LAMP_OFF;
                case LOW_POWER, ON -> ModdedPartialModels.GROWTH_LAMP_ON;
                case BROKEN -> ModdedPartialModels.GROWTH_LAMP_BROKEN;
            }))
            .transform(LightBulb.setProperties(120, 90, 40, 1600, 0.01f))
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/lamps/growth_lamp")))
            .register();

    public static final ItemEntry<Item> RESISTIVE_COIL = ingredient("resistive_coil", forgeItemTag("iron_coils"), ModdedTags.Item.COILS.tag);
    public static final ItemEntry<Item> COPPER_COIL = ingredient("copper_coil", forgeItemTag("copper_coils"), ModdedTags.Item.COILS.tag);
    public static final ItemEntry<Item> MAGNET = ingredient("magnet");

    public static final ItemEntry<Item> INTEGRATED_CIRCUIT = ingredient("integrated_circuit");
    public static final ItemEntry<Item> ELECTRICAL_GIZMO = ingredient("electrical_gizmo");
    public static final ItemEntry<Item> ZINC_SHEET = ingredient("zinc_sheet", ModdedTags.Item.PLATES.tag, forgeItemTag("zinc_plates"));

    public static final ItemEntry<Item> RELAY = ingredient("relay");
    public static final ItemEntry<Item> RESISTOR = ingredient("resistor");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TRANSFORMER_CORE = sequencedIngredient("incomplete_transformer_core");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_ELECTRICAL_GIZMO = sequencedIngredient("incomplete_electrical_gizmo");
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_UNETCHED_CIRCUIT = sequencedIngredientBuilder("incomplete_unetched_circuit")
            .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
            .register();
    public static final ItemEntry<IncompleteCircuitItem> INCOMPLETE_CIRCUIT = REGISTRATE.item("incomplete_circuit", IncompleteCircuitItem::new)
            .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
            .register();

    public static final ItemEntry<ElectroZapperItem> ELECTROZAPPER = REGISTRATE.item("electrozapper", ElectroZapperItem::new)
            .transform(CreateRegistrate.customRenderedItem(() -> ElectroZapperItemRenderer::new))
            .model((ctx, prov) -> prov
                    .withExistingParent(ctx.getName(), PowerGrid.asResource("item/electrozapper/item")))
            .lang("Electro-Zapper")
            .register();

    public static final ItemEntry<ElectroBatonItem> ELECTROBATON = REGISTRATE.item("electrobaton", ElectroBatonItem::new)
            .model((ctx, prov) -> prov
                    .withExistingParent(ctx.getName(), PowerGrid.asResource("item/electrobaton/item")))
            .lang("Electro-Baton")
            .register();

    public static final ItemEntry<BacktankItem.BacktankBlockItem> PORTABLE_BATTERY_PLACEABLE = REGISTRATE.item("portable_battery_placeable",
                    p -> new BacktankItem.BacktankBlockItem(ModdedBlocks.PORTABLE_BATTERY.get(), ModdedItems.PORTABLE_BATTERY::get, p))
            .model((c, p) -> p.withExistingParent(c.getName(), p.mcLoc("item/barrier")))
            .register();

    public static final ItemEntry<PortableBatteryItem> PORTABLE_BATTERY = REGISTRATE.item("portable_battery", p -> new PortableBatteryItem(ZincArmorMaterial.INSTANCE, p, PowerGrid.asResource("zinc"), PORTABLE_BATTERY_PLACEABLE))
            .model((ctx, prov) ->
                    prov.withExistingParent(ctx.getName(), prov.modLoc("block/portable_battery/block")))
            .properties(p -> p.maxDamage(-1))
			.tag(forgeItemTag("chestplates"))
            .register();

    public static final ItemEntry<CircuitSchematicItem> CIRCUIT_SCHEMATIC = REGISTRATE.item("circuit_schematic", CircuitSchematicItem::new)
            .tag(ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag)
            .register();

    public static final ItemEntry<Item> UNETCHED_CIRCUIT = ingredient("unetched_circuit", ModdedTags.Item.CIRCUIT_SCHEMATIC_HOLDER.tag);

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    private static ItemEntry<SequencedAssemblyItem> sequencedIngredient(String name) {
        return REGISTRATE.item(name, SequencedAssemblyItem::new).register();
    }

    private static ItemBuilder<SequencedAssemblyItem, PowerGridRegistrate> sequencedIngredientBuilder(String name) {
        return REGISTRATE.item(name, SequencedAssemblyItem::new);
    }

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new).register();
    }

    @SafeVarargs
    private static ItemEntry<Item> ingredient(String name, TagKey<Item>... tags) {
        return REGISTRATE.item(name, Item::new).tag(tags).register();
    }
}
