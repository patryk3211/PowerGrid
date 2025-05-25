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
package org.patryk3211.powergrid.ponder.base;

import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.instruction.PonderInstruction;
import org.patryk3211.powergrid.electricity.PonderElectricNetwork;

public class UnloadElectricityWorldInstruction extends PonderInstruction {
    private boolean executed = false;

    @Override
    public boolean isComplete() {
        return executed;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void reset(PonderScene scene) {
        executed = false;
    }

    @Override
    public void tick(PonderScene scene) {
        if(!executed) {
            executed = true;
            PonderElectricNetwork.removeWorldEntry(scene.getWorld());
        }
    }
}
