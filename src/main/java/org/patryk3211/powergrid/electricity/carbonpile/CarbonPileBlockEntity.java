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
package org.patryk3211.powergrid.electricity.carbonpile;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class CarbonPileBlockEntity extends SmartBlockEntity {
    protected AThermalBehaviour thermal;
    protected TrimValueBehaviour trim;
    private CarbonPileCoilBlockEntity coil;
    private int size;

    public CarbonPileBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {
        thermal = ThermalBehaviour.fromConfig(this);
        if(thermal != null && thermal instanceof ThermalBehaviour thermalBehaviour) {
            thermalBehaviour.particleGenerator((consumer, random) -> {
                for (int i = 0; i < Math.ceil(size / 2.0f); ++i) {
                    double x = worldPosition.getX() + random.nextDouble();
                    double z = worldPosition.getZ() + random.nextDouble();
                    double y = worldPosition.getY() - size + 1 + random.nextDouble() * size;
                    consumer.accept(x, y, z);
                }
            });
            list.add(thermal);
        }
        trim = new TrimValueBehaviour(Lang.translateDirect("gui.carbon_pile.trim"), this, new Box());
        trim.withCallback(i -> {
            assert coil != null;
            coil.setTrim(1.0f + i / 200f);
        });
        list.add(trim);
    }

    @Override
    public void initialize() {
        assert level != null;
        super.initialize();
        var pos = worldPosition.below();
        size = 1;
        while(ModdedBlocks.CARBON_PILE.has(level.getBlockState(pos))) {
            pos = pos.below();
            ++size;
        }
        var coil = level.getBlockEntity(pos, ModdedBlockEntities.CARBON_PILE_COIL.get());
        if(coil.isEmpty()) {
            level.destroyBlock(worldPosition, true);
            return;
        }
        this.coil = coil.get();
        trim.setValue((int) ((this.coil.getTrim() - 1.0f) * 200));
        if(thermal != null && thermal instanceof ThermalBehaviour thermalBehaviour) {
            thermalBehaviour.setThermalMass(ThermalValues.getMass(getBlockState().getBlock()) * size);
            thermalBehaviour.setDissipationFactor(
                    ThermalBehaviour.dissipationFactor(ThermalValues.getPower(getBlockState().getBlock()) * size,
                            175.0f));
        }
    }

    @Override
    public void tick() {
        if(thermal != null && coil != null && !level.isClientSide)
            thermal.applyWirePower(coil.getPileWire());
        super.tick();
    }

    private static class Box extends CenteredSideValueBoxTransform {
        public Box() {
            super((state, dir) -> dir == Direction.UP);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 12.5f);
        }
    }
}
