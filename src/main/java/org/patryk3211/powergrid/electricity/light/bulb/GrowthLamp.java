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

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock.FACING;

public class GrowthLamp extends LightBulb {
    public GrowthLamp(net.minecraft.world.item.Item.Properties settings) {
        super(settings);
    }

    @Override
    public LightBulbState createState(LightFixtureBlockEntity fixture) {
        return new org.patryk3211.powergrid.electricity.light.bulb.GrowthLamp.State(this, fixture, modelSupplier);
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

            var world = fixture.getLevel();
            var power = getPowerLevel();
            if(world.isClientSide || power == 0)
                return;

            var origin = fixture.getBlockPos();
            var facing = fixture.getBlockState().getValue(FACING).getOpposite();
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

            var serverWorld = (ServerLevel) world;
            var random = serverWorld.random;
            if(random.nextInt(ModdedConfigs.server().electricity.growthLampChance.get() / power) == 0) {
                var x = random.nextIntBetweenInclusive(xMin, xMax);
                var y = random.nextIntBetweenInclusive(yMin, yMax);
                var z = random.nextIntBetweenInclusive(zMin, zMax);
                var pos = origin.offset(x, y, z);
                var state = serverWorld.getBlockState(pos);
                if(state.is(ModdedTags.Block.AFFECTED_BY_LAMP.tag))
                    state.randomTick(serverWorld, pos, random);
            }
        }
    }
}
