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
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock;

import java.util.List;

public class InductionRotorBlock extends AbstractRotorBlock implements IBE<InductionRotorBlockEntity>, IHaveElectricProperties {
    public InductionRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<InductionRotorBlockEntity> getBlockEntityClass() {
        return InductionRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends InductionRotorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR_INDUCTION_ROTOR.get();
    }

    @Override
    public float getInertia() {
        return ModdedConfigs.server().kinetics.generatorControls.generatorInductionRotorInertia.getF();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        withBlockEntityDo(level, pos, InductionRotorBlockEntity::neighborsChanged);
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean alwaysDisplay() {
        return true;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(ResistanceValues.get(this), player, tooltip);
    }
}
