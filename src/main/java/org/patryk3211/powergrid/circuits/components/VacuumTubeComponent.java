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
package org.patryk3211.powergrid.circuits.components;

import com.simibubi.create.AllItems;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;

public class VacuumTubeComponent extends Component {
    public static final ComponentFootprint FOOTPRINT = new ComponentFootprint(3, 3)
            .addPad(0, 1)
            .addPad(0, 4)
            .addPad(5, 1)
            .addPad(5, 4)
            .withItem(AllItems.ELECTRON_TUBE)
            .withOutline();

    public VacuumTubeComponent() {
        super(AllItems.ELECTRON_TUBE, FOOTPRINT);
    }
}
