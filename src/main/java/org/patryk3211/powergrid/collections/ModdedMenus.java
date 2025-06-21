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

import com.tterrag.registrate.builders.MenuBuilder;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.screen.ScreenHandler;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableEditMenu;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableEditScreen;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableMenu;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableScreen;

public class ModdedMenus {
    public static final MenuEntry<CircuitDesignTableMenu> CIRCUIT_DESIGN_BENCH =
            register("circuit_design_table", CircuitDesignTableMenu::new, () -> CircuitDesignTableScreen::new);
    public static final MenuEntry<CircuitDesignTableEditMenu> CIRCUIT_DESIGN_BENCH_EDIT =
            register("circuit_design_table_edit", CircuitDesignTableEditMenu::new, () -> CircuitDesignTableEditScreen::new);

    private static <C extends ScreenHandler, S extends Screen & ScreenHandlerProvider<C>> MenuEntry<C> register(String name, MenuBuilder.ForgeMenuFactory<C> factory, NonNullSupplier<MenuBuilder.ScreenFactory<C, S>> screenFactory) {
        return PowerGrid.REGISTRATE.menu(name, factory, screenFactory).register();
    }

    public static void register() {}
}
