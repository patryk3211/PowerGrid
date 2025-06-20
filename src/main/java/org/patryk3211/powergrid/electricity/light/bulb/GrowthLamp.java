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

import com.jozufozu.flywheel.core.PartialModel;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock.FACING;

public class GrowthLamp extends LightBulb {
    public GrowthLamp(Settings settings) {
        super(settings);
    }

    @Override
    public LightBulbState createState(LightFixtureBlockEntity fixture) {
        return new State(this, fixture, modelSupplier);
    }

    public static class State extends SimpleState {
        public <T extends Item & ILightBulb> State(T bulb, LightFixtureBlockEntity fixture, Supplier<Function<LightBulb.State, PartialModel>> modelProviderSupplier) {
            super(bulb, fixture, modelProviderSupplier);
        }

        @Override
        public void tick() {
            super.tick();
            if(burned)
                return;

            var world = fixture.getWorld();
            var power = getPowerLevel();
            if(world.isClient || power == 0)
                return;

            var origin = fixture.getPos();
            var facing = fixture.getCachedState().get(FACING).getOpposite();
            final var radius = ModdedConfigs.server().electricity.growthLampRadius.get();
            int xMin = -radius, xMax = radius;
            int yMin = -radius, yMax = radius;
            int zMin = -radius, zMax = radius;

            switch(facing) {
                case EAST -> xMin = 0;
                case WEST -> xMax = 0;
                case UP -> yMin = 0;
                case DOWN -> yMax = 0;
                case SOUTH -> zMin = 0;
                case NORTH -> zMax = 0;
            }

            var serverWorld = (ServerWorld) world;
            var random = serverWorld.random;
            if(random.nextInt(ModdedConfigs.server().electricity.growthLampChance.get() / power) == 0) {
                var x = random.nextBetween(xMin, xMax);
                var y = random.nextBetween(yMin, yMax);
                var z = random.nextBetween(zMin, zMax);
                var pos = origin.add(x, y, z);
                var state = serverWorld.getBlockState(pos);
                if(state.isIn(ModdedTags.Block.AFFECTED_BY_LAMP.tag))
                    state.randomTick(serverWorld, pos, random);
            }
        }
    }
}
