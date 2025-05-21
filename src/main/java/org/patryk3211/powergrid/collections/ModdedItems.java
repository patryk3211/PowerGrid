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

import com.simibubi.create.AllTags;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGridRegistrate;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulb;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.WireProperties;

import static com.simibubi.create.AllTags.forgeItemTag;
import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class ModdedItems {
    public static final ItemEntry<WireItem> WIRE = REGISTRATE.item("wire", WireItem::new)
            .transform(WireProperties.setAll(0.005f, 16))
            .tag(ModdedTags.Item.COIL_WIRE.tag, ModdedTags.Item.WIRES.tag)
            .register();
    public static final ItemEntry<WireItem> IRON_WIRE = REGISTRATE.item("iron_wire", WireItem::new)
            .transform(WireProperties.setAll(0.015f, 32))
            .tag(ModdedTags.Item.WIRES.tag)
            .register();
    public static final ItemEntry<WireItem> SILVER_WIRE = REGISTRATE.item("silver_wire", WireItem::new)
            .transform(WireProperties.setAll(0.003f, 8))
            .tag(ModdedTags.Item.WIRES.tag)
            .register();

    public static final ItemEntry<Item> WIRE_CUTTER = REGISTRATE.item("wire_cutter", Item::new)
            .register();

    public static final ItemEntry<LightBulb> LIGHT_BULB = REGISTRATE.item("light_bulb", LightBulb::new)
            .transform(LightBulb.setModelProvider(() -> state -> switch(state) {
                case OFF -> ModdedPartialModels.LIGHT_BULB_OFF;
                case ON -> ModdedPartialModels.LIGHT_BULB_ON;
                case BROKEN -> ModdedPartialModels.LIGHT_BULB_BROKEN;
            }))
            .transform(LightBulb.setProperties(30, 60, 30))
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/light_bulb")))
            .register();

    public static final ItemEntry<Item> RESISTIVE_COIL = ingredient("resistive_coil");
    public static final ItemEntry<Item> COPPER_COIL = ingredient("copper_coil");

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
    public static final ItemEntry<Item> SILVER_MESH = REGISTRATE.item("silver_mesh", Item::new)
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
