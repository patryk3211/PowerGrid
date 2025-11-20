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
package org.patryk3211.powergrid.utility.sound;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Kindly taken from Create
 * @see com.simibubi.create.foundation.sound.SoundScapes
 */
public class SoundScapes {
    static final int MAX_AMBIENT_SOURCE_DISTANCE = 16;
    static final int UPDATE_INTERVAL = 4;
    static final int SOUND_VOLUME_ARG_MAX = 15;

    public enum AmbienceGroup {
        HUM(SoundScapes::hum),
        GENERATOR(SoundScapes::generator);

        private BiFunction<Float, AmbienceGroup, SoundScape> factory;

        AmbienceGroup(BiFunction<Float, AmbienceGroup, SoundScape> factory) {
            this.factory = factory;
        }

        public SoundScape instantiate(float pitch) {
            return factory.apply(pitch, this);
        }
    }

    private static SoundScape hum(float pitch, AmbienceGroup group) {
        return new SoundScape(pitch, group)
                .continuous(ModdedSoundEvents.TRANSFORMER_HUM.getMainEvent(), 2, 1);
    }

    private static SoundScape generator(float pitch, AmbienceGroup group) {
        return new SoundScape(pitch, group)
                .continuous(ModdedSoundEvents.GENERATOR.getMainEvent(), 1, 1);
    }

    enum RangeGroup {
        VERY_LOW, LOW, NORMAL, HIGH, VERY_HIGH
    }

    private static Map<AmbienceGroup, Map<RangeGroup, Set<BlockPos>>> counter = new IdentityHashMap<>();
    private static Map<Pair<AmbienceGroup, RangeGroup>, SoundScape> activeSounds = new HashMap<>();

    public static void play(AmbienceGroup group, BlockPos pos, float pitch, float volume) {
//        if (!AllConfigs.client().enableAmbientSounds.get())
//            return;
        if (!outOfRange(pos) && volume > 0.05f)
            addSound(group, pos, pitch, volume);
    }

    public static void tick() {
        activeSounds.values()
                .forEach(SoundScape::tick);

        if (AnimationTickHolder.getTicks() % UPDATE_INTERVAL != 0)
            return;

        boolean disable = false;//!AllConfigs.client().enableAmbientSounds.get();
        for (Iterator<Map.Entry<Pair<AmbienceGroup, RangeGroup>, SoundScape>> iterator = activeSounds.entrySet()
                .iterator(); iterator.hasNext();) {

            Map.Entry<Pair<AmbienceGroup, RangeGroup>, SoundScape> entry = iterator.next();
            Pair<AmbienceGroup, RangeGroup> key = entry.getKey();
            SoundScape value = entry.getValue();

            if (disable || getSoundCount(key.getFirst(), key.getSecond()) == 0) {
                value.remove();
                iterator.remove();
            }
        }

        counter.values()
                .forEach(m -> m.values()
                        .forEach(Set::clear));
    }

    private static void addSound(AmbienceGroup group, BlockPos pos, float pitch, float volume) {
        RangeGroup groupFromPitch = getGroupFromPitch(pitch);
        Set<BlockPos> set = counter.computeIfAbsent(group, ag -> new IdentityHashMap<>())
                .computeIfAbsent(groupFromPitch, pg -> new HashSet<>());
        set.add(pos);

        Pair<AmbienceGroup, RangeGroup> pair = Pair.of(group, groupFromPitch);
        activeSounds.computeIfAbsent(pair, $ -> {
            SoundScape soundScape = group.instantiate(pitch);
            soundScape.play();
            return soundScape;
        }).addVolume(volume);
    }

    public static void invalidateAll() {
        counter.clear();
        activeSounds.forEach(($, sound) -> sound.remove());
        activeSounds.clear();
    }

    protected static boolean outOfRange(BlockPos pos) {
        return !getCameraPos().closerThan(pos, MAX_AMBIENT_SOURCE_DISTANCE);
    }

    protected static BlockPos getCameraPos() {
        Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
        if (renderViewEntity == null)
            return BlockPos.ZERO;
        BlockPos playerLocation = renderViewEntity.blockPosition();
        return playerLocation;
    }

    public static int getSoundCount(AmbienceGroup group, RangeGroup pitchGroup) {
        return getAllLocations(group, pitchGroup).size();
    }

    public static Set<BlockPos> getAllLocations(AmbienceGroup group, RangeGroup pitchGroup) {
        return counter.getOrDefault(group, Collections.emptyMap())
                .getOrDefault(pitchGroup, Collections.emptySet());
    }

    public static RangeGroup getGroupFromPitch(float pitch) {
        if (pitch < .70)
            return RangeGroup.VERY_LOW;
        if (pitch < .90)
            return RangeGroup.LOW;
        if (pitch < 1.10)
            return RangeGroup.NORMAL;
        if (pitch < 1.30)
            return RangeGroup.HIGH;
        return RangeGroup.VERY_HIGH;
    }
}
