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

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.item.Item;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.CatalyzerItem;
import org.patryk3211.powergrid.electricity.light.bulb.GrowthLamp;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulb;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.WireProperties;

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
    public static final ItemEntry<WireItem> SILVER_WIRE = REGISTRATE.item("silver_wire", WireItem::new)
            .transform(WireProperties.setAll(0.003f, 8))
            .transform(WireProperties.setRenderingParams(PowerGrid.texture("special/copper_wire"), 1.01f, 1.2f, 0.0625f))
            .tag(ModdedTags.Item.WIRES.tag, ModdedTags.Item.LIGHT_WIRES.tag)
            .register();

    public static final ItemEntry<Item> WIRE_CUTTER = REGISTRATE.item("wire_cutter", Item::new)
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

    public static final ItemEntry<Item> RESISTIVE_COIL = ingredient("resistive_coil");
    public static final ItemEntry<Item> COPPER_COIL = ingredient("copper_coil");
    public static final ItemEntry<Item> MAGNET = ingredient("magnet");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TRANSFORMER_CORE = sequencedIngredient("incomplete_transformer_core");

    public static final ItemEntry<Item> SULFUR = REGISTRATE.item("sulfur", Item::new)
            .register();
    public static final ItemEntry<Item> RAW_SILVER = REGISTRATE.item("raw_silver", Item::new)
            .tag(forgeItemTag("raw_silver_ores"), ModdedTags.Item.RAW_ORES.tag)
            .register();
    public static final ItemEntry<Item> SILVER_INGOT = REGISTRATE.item("silver_ingot", Item::new)
            .tag(ModdedTags.Item.SILVER_INGOTS.tag)
            .register();
    public static final ItemEntry<Item> SILVER_SHEET = REGISTRATE.item("silver_sheet", Item::new)
            .tag(ModdedTags.Item.PLATES.tag, forgeItemTag("silver_plates"))
            .register();
    public static final ItemEntry<CatalyzerItem> SILVER_MESH = REGISTRATE.item("silver_mesh", CatalyzerItem::new)
            .transform(CatalyzerItem.setStrength(1.0f))
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    private static ItemEntry<SequencedAssemblyItem> sequencedIngredient(String name) {
        return REGISTRATE.item(name, SequencedAssemblyItem::new).register();
    }

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new).register();
    }
}
