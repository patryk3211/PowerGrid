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

import com.google.gson.JsonObject;
import com.simibubi.create.AllSoundEvents;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ModdedSoundEvents {
    public static final Map<Identifier, AllSoundEvents.SoundEntry> ALL = new HashMap<>();

    public static final AllSoundEvents.SoundEntry

    LV_SWITCH_CLICK = create("lv_switch_click").subtitle("Switch clicks")
            .playExisting(SoundEvents.BLOCK_LEVER_CLICK)
            .category(SoundCategory.BLOCKS)
            .build(),

    MV_SWITCH_CLICK = create("mv_switch_click").subtitle("Switch clicks")
            .playExisting(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE)
            .category(SoundCategory.BLOCKS)
            .build(),

    HV_SWITCH_CONNECT = create("hv_switch_connect").subtitle("Switch connected")
            .playExisting(AllSoundEvents.CONTRAPTION_DISASSEMBLE::getMainEvent, .5f, .75f)
            .category(SoundCategory.BLOCKS)
			.build(),

    HV_SWITCH_DISCONNECT = create("hv_switch_disconnect").subtitle("Switch disconnected")
            .playExisting(AllSoundEvents.CONTRAPTION_ASSEMBLE::getMainEvent, .5f, .75f)
            .category(SoundCategory.BLOCKS)
            .build(),

    WIRE_CUT = create("wire_cut").subtitle("Wire cut")
            .playExisting(SoundEvents.ENTITY_SHEEP_SHEAR, 0.75f, 1.25f)
            .category(SoundCategory.BLOCKS)
            .build(),

    MAGNETIZING = create("magnetizing").subtitle("Magnetizing")
            .addVariant("magnetizing")
            .category(SoundCategory.BLOCKS)
            .build(),

    ELECTROZAPPER_SHOOT = create("electrozapper_shoot").subtitle("Electro-Zapper bzzzts")
            .category(SoundCategory.PLAYERS)
            .build(),

    UI_CLICK = create("ui.click")
            .playExisting(SoundEvents.UI_BUTTON_CLICK)
            .build(),

    UI_COMPONENT_ROTATE = create("ui.component_rotate")
            .playExisting(SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, 0.75f, 0.75f)
            .build(),

    UI_PLACE_TRACE = create("ui.place_trace")
            .playExisting(SoundEvents.BLOCK_METAL_PLACE, 0.75f, 1.75f)
            .build(),

    UI_DELETE_AREA = create("ui.delete_area")
            .playExisting(SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, 0.5f, 1.0f)
            .build(),

    UI_PLACE_COMPONENT = create("ui.place_component")
            .playExisting(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, 0.5f, 1.0f)
            .build(),

    UI_FAIL = create("ui.action.fail")
            .playExisting(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0f, 0.5f)
            .build(),

    UI_SELECT_COMPONENT = create("ui.select_component")
            .playExisting(SoundEvents.UI_BUTTON_CLICK, 1.0f, 1.0f)
            .build(),

    RELAY_CLICK = create("relay_click").subtitle("Relay clicks")
            .playExisting(SoundEvents.BLOCK_LEVER_CLICK)
            .category(SoundCategory.BLOCKS)
            .build(),

    COMPONENT_EXPLODE = create("component_explode").subtitle("Component pops")
            .playExisting(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.25f, 2.0f)
            .category(SoundCategory.BLOCKS)
            .build(),

    MICROSWITCH_ON = create("uswitch_on").subtitle("Switch clicks")
            .playExisting(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 2.0f)
            .category(SoundCategory.BLOCKS)
            .build(),

    MICROSWITCH_OFF = create("uswitch_off").subtitle("Switch clicks")
            .playExisting(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, 1.0f, 2.0f)
            .category(SoundCategory.BLOCKS)
            .build(),

    MICROBUTTON_ON = create("ubutton_on").subtitle("Button clicks")
            .playExisting(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.75f, 2.0f)
            .category(SoundCategory.BLOCKS)
            .build(),

    MICROBUTTON_OFF = create("ubutton_off").subtitle("Button clicks")
            .playExisting(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.75f, 2.0f)
            .category(SoundCategory.BLOCKS)
            .build()
            ;

    private static SoundEntryBuilder create(String name) {
        return create(PowerGrid.asResource(name));
    }

    public static SoundEntryBuilder create(Identifier id) {
        return new SoundEntryBuilder(id);
    }

    public static void prepare() {
        for(AllSoundEvents.SoundEntry entry : ALL.values())
            entry.prepare();
    }

    public static void register() {
        for(AllSoundEvents.SoundEntry entry : ALL.values())
            entry.register();
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for(AllSoundEvents.SoundEntry entry : ALL.values())
            if(entry.hasSubtitle())
                consumer.accept(entry.getSubtitleKey(), entry.getSubtitle());
    }

    public static DataProvider provider(FabricDataOutput output) {
        return new SoundEntryProvider(output);
    }

    private static class SoundEntryProvider implements DataProvider {
        private DataOutput output;

        public SoundEntryProvider(DataOutput output) {
            this.output = output;
        }

        @Override
        public CompletableFuture<?> run(DataWriter cache) {
            return generate(output.getPath(), cache);
        }

        @Override
        public String getName() {
            return "Power Grid's Custom Sounds";
        }

        public CompletableFuture<?> generate(Path path, DataWriter cache) {
            path = path.resolve("assets/powergrid");
            JsonObject json = new JsonObject();
            ALL.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        entry.getValue()
                                .write(json);
                    });
            return DataProvider.writeToPath(cache, json, path.resolve("sounds.json"));
        }
    }

    public static class SoundEntryBuilder extends AllSoundEvents.SoundEntryBuilder {
        public SoundEntryBuilder(Identifier id) {
            super(id);
        }

        @Override
        public AllSoundEvents.SoundEntryBuilder addVariant(String name) {
            return addVariant(PowerGrid.asResource(name));
        }

        @Override
        public AllSoundEvents.SoundEntry build() {
            var entry = super.build();
            // Put entry into our map.
            AllSoundEvents.ALL.remove(entry.getId());
            ALL.put(entry.getId(), entry);
            return entry;
        }
    }
}
