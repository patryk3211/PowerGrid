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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedTags;

import java.util.function.Function;
import java.util.function.Supplier;

public class GrowthLamp extends LightBulb {

    public GrowthLamp(Item.Properties settings) {
        super(settings);
    }

    @Override
    public <F extends SmartBlockEntity & IFixtureEntity> LightBulbState createState(F fixture) {
        return new GrowthLamp.State(this, fixture, modelSupplier, dyedModelSupplier);
    }

    public static class State extends SimpleState {

        public <T extends Item & ILightBulb,
                F extends SmartBlockEntity & IFixtureEntity> State(T bulb, F fixture,
                                                                   Supplier<Function<LightBulb.State, PartialModel>> modelProviderSupplier,
                                                                   Supplier<Function<DyedState, PartialModel>> dyedModelProviderSupplier) {
            super(bulb, fixture, modelProviderSupplier, dyedModelProviderSupplier);
        }

        @Override
        protected void specialEffects(BlockPos origin, @Nullable Direction facing) {
            final var radius = ModdedConfigs.server().electricity.growthLampRadius.get();
            int xMin = -radius, xMax = radius;
            int yMin = -radius, yMax = radius;
            int zMin = -radius, zMax = radius;

            if(facing != null) {
                switch(facing) {
                    case EAST -> xMin = 0;
                    case WEST -> xMax = 0;
                    case UP -> yMin = 0;
                    case DOWN -> yMax = 0;
                    case SOUTH -> zMin = 0;
                    case NORTH -> zMax = 0;
                }
            }

            int chanceValue = ModdedConfigs.server().electricity.growthLampChance.get();
            var serverWorld = (ServerLevel) fixtureBE.getLevel();
            var random = serverWorld.random;
            // Iterate over every block in the cube and boost all valid crops
            for(int x = xMin; x <= xMax; x++) {
                for(int y = yMin; y <= yMax; y++) {
                    for(int z = zMin; z <= zMax; z++) {
                        // Per-block chance check - keeps performance reasonable
                        if(chanceValue > 1 && random.nextInt(chanceValue) != 0)
                            continue;

                        var pos = origin.offset(x, y, z);
                        var state = serverWorld.getBlockState(pos);

                        if(state.is(ModdedTags.Block.AFFECTED_BY_LAMP.tag))
                            state.randomTick(serverWorld, pos, random);
                    }
                }
            }
        }
    }
}