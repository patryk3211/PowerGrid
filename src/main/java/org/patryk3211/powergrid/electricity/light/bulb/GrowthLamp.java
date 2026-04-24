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

    public GrowthLamp(Item.Properties settings) {
        super(settings);
    }

    @Override
    public LightBulbState createState(LightFixtureBlockEntity fixture) {
        return new GrowthLamp.State(this, fixture, modelSupplier, dyedModelSupplier);
    }

    public static class State extends SimpleState {

        public <T extends Item & ILightBulb> State(T bulb, LightFixtureBlockEntity fixture,
                                                   Supplier<Function<LightBulb.State, PartialModel>> modelProviderSupplier,
                                                   Supplier<Function<LightBulb.DyedState, PartialModel>> dyedModelProviderSupplier) {
            super(bulb, fixture, modelProviderSupplier, dyedModelProviderSupplier);
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
            var facing = fixture.getBlockState().getValue(FACING);
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

            int chanceValue = ModdedConfigs.server().electricity.growthLampChance.get();
            var serverWorld = (ServerLevel) world;
            var random = serverWorld.random;
            // Iterate over every block in the cube and boost all valid crops
            for (int x = xMin; x <= xMax; x++) {
                for (int y = yMin; y <= yMax; y++) {
                    for (int z = zMin; z <= zMax; z++) {
                        // Per-block chance check - keeps performance reasonable
                        if (chanceValue > 1 && random.nextInt(chanceValue) != 0)
                            continue;

                        var pos = origin.offset(x, y, z);
                        var state = serverWorld.getBlockState(pos);

                        if (state.is(ModdedTags.Block.AFFECTED_BY_LAMP.tag))
                            state.randomTick(serverWorld, pos, random);
                    }
                }
            }
        }
    }
}