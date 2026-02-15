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
package org.patryk3211.powergrid.electricity.grounding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.ArrayList;
import java.util.HashSet;

public class GroundingRodBlockEntity extends ElectricBlockEntity {
    private static final float DANGER_POTENTIAL = 48;
    private SwitchedWire wire;
    private int groundCount;
    private int conductiveCount;
    private int damageTickCounter;

    public GroundingRodBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
    }

    private static int minBlocks() {
        return ModdedConfigs.server().electricity.groundingMinimumBlocks.get();
    }

    private void scan(BlockPos pos) {
        assert level != null;
        var scanned = new HashSet<BlockPos>();
        var queue = new ArrayList<BlockPos>();
        queue.add(pos);
        while(!queue.isEmpty()) {
            var checkPos = queue.remove(0);
            if(!scanned.add(checkPos))
                continue;
            var state = level.getBlockState(checkPos);
            if(state.is(ModdedTags.Block.CONDUCTIVE_GROUND.tag)) {
                ++conductiveCount;
            } else if(groundCount >= minBlocks()) {
                // Avoid running the test below
                continue;
            } else if(state.isRedstoneConductor(level, checkPos)) {
                // Stop scanning normal blocks if the threshold has been reached
                if(++groundCount >= minBlocks())
                    continue;
            }
            for(var dir : Direction.values()) {
                var nextPos = checkPos.relative(dir);
                if(scanned.contains(nextPos))
                    continue;
                // Limit search to 10 blocks away from starting position,
                // this should result in about 1000 blocks checked.
                if(nextPos.distManhattan(pos) < 10 && !level.isEmptyBlock(nextPos))
                    queue.add(nextPos);
            }
            if(conductiveCount >= ModdedConfigs.server().electricity.groundingMaximumBlocks.get() &&
                groundCount >= minBlocks())
                break;
        }
    }

    @Override
    public void lazyTick() {
        assert level != null;
        super.lazyTick();
        if(!level.isClientSide) {
            var prevConductiveCount = conductiveCount;
            groundCount = 0;
            conductiveCount = 0;
            scan(getBlockPos().below());
            var prevState = wire.getState();
            if(groundCount < minBlocks()) {
                wire.setState(false);
            } else {
                var highR = ModdedConfigs.server().electricity.groundingHighestResistance.getF();
                var lowR = ModdedConfigs.server().electricity.groundingLowestResistance.getF();
                int maxBlocks = ModdedConfigs.server().electricity.groundingMaximumBlocks.get();
                var delta = (highR - lowR) / maxBlocks;
                var resistance = highR - delta * Math.min(conductiveCount, maxBlocks);
                wire.setResistance(resistance);
                wire.setState(true);
            }
            if(prevState != wire.getState() || prevConductiveCount != conductiveCount)
                notifyUpdate();
        }
    }

    @Override
    public void tick() {
        assert level != null;
        super.tick();
        if(!level.isClientSide && damageTickCounter++ >= 10) {
            damageTickCounter = 0;
            // Limit radius to 10 block to avoid having to scan the whole world
            var dangerRadius = Math.min(Math.abs(wire.getResistance() * wire.current() / (2 * Math.PI * DANGER_POTENTIAL)), 10);
            var blockRadius = (int) Math.round(dangerRadius);
            var bb = new AABB(worldPosition.offset(-blockRadius, -blockRadius, -blockRadius), worldPosition.offset(blockRadius, blockRadius, blockRadius));
            var sqrDist = dangerRadius * dangerRadius;
            var center = worldPosition.getCenter();
            var entities = level.getEntitiesOfClass(LivingEntity.class, bb, e -> e.position().distanceToSqr(center) <= sqrDist && e.onGround());

            Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            var source = new DamageSource(registry.getHolder(ModdedDamageTypes.ZAP).get());
            for(var entity : entities) {
                // TODO: Scale damage with potential.
                entity.hurt(source, 1);
            }
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(1);
        wire = builder.connectSwitch(1, builder.terminalNode(0), null, false);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if(tag.contains("Resistance")) {
            var R = tag.getFloat("Resistance");
            wire.setState(false);
            wire.setResistance(R);
            wire.setState(R > 0);
        } else {
            wire.setState(false);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if(wire.getState())
            tag.putFloat("Resistance", (float) wire.getResistance());
    }
}
