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
package org.patryk3211.powergrid.electricity.base;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface ISocketElectric {
    default int socketIndexAt(BlockState state, Vec3 pos) {
        for(int i = 0; i < socketCount(); ++i) {
            var terminal = socket(state, i);
            if(terminal == null)
                continue;
            if(terminal.check(pos))
                return i;
        }
        return -1;
    }

    default ITerminalPlacement socketAt(BlockState state, Vec3 pos) {
        for(int i = 0; i < socketCount(); ++i) {
            var terminal = socket(state, i);
            if(terminal == null)
                continue;
            if(terminal.check(pos))
                return terminal;
        }
        return null;
    }

    int socketCount();
    ITerminalPlacement socket(BlockState state, int index);
}
