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
package org.patryk3211.powergrid.electricity.light.bulb;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

public class LvLightBulb extends LightBulb {
    public LvLightBulb(Item.Properties settings) {
        super(settings);
    }

    @Override
    public <F extends SmartBlockEntity & IFixtureEntity> LightBulbState createState(F fixture) {
        return new State(this, fixture, modelSupplier, dyedModelSupplier);
    }

    public static class State extends LightBulb.SimpleState {
        public <T extends Item & ILightBulb,
                F extends SmartBlockEntity & IFixtureEntity> State(T bulb, F fixture,
                                                                   Supplier<Function<LightBulb.State, PartialModel>> modelProviderSupplier,
                                                                   Supplier<Function<DyedState, PartialModel>> dyedModelProviderSupplier) {
            super(bulb, fixture, modelProviderSupplier, dyedModelProviderSupplier);
        }

        @Override
        protected void updatePowerLevel(int newLevel) {
            super.updatePowerLevel(Math.min(newLevel, 1));
        }
    }
}
