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
package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;

import java.util.List;

/**
 * Electric gauge block entity base class.
 * PowerGrid's electric equivalent of the kinetic gauge from Create.
 * @see com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity
 */
public abstract class GaugeBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    protected final float maxValue;

    public float dialTarget;
    public float prevDialState;
    public float dialState;

    public GaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        assert state.getBlock() instanceof IGaugeBlock;
        maxValue = ((IGaugeBlock) state.getBlock()).getMaxValue();
    }

    @Override
    public void tick() {
        super.tick();
        if(!Float.isNaN(dialTarget)) {
            prevDialState = dialState;
            dialState += (dialTarget - dialState) * .125f;
            if (dialState > 1 && world.random.nextFloat() < 1 / 2f)
                dialState -= (dialState - 1) * world.random.nextFloat();
        }
    }

    public abstract float getValue();

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        // Use default Create header here.
        Lang.translate("gui.gauge.info_header").forGoggles(tooltip);
        return true;
    }
}
