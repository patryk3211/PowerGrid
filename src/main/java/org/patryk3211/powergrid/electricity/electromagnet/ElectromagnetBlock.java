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
package org.patryk3211.powergrid.electricity.electromagnet;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;

import java.util.List;

public class ElectromagnetBlock extends ElectricBlock implements IBE<ElectromagnetBlockEntity>, IHaveElectricProperties, IAcceptConnector {
    private static final VoxelShape DOWN_SHAPE = Shapes.or(
            box(0, 1, 0, 16, 16, 16),
            box(3.5, -6, 3.5, 12.5, 1, 12.5)
    );

    public ElectromagnetBlock(Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return DOWN_SHAPE;
    }

    @Override
    public Class<ElectromagnetBlockEntity> getBlockEntityClass() {
        return ElectromagnetBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ElectromagnetBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ELECTROMAGNET.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        var minCurrent = 0.25f / 0.1f;
        Voltage.min(minCurrent * resistance(), player, tooltip);
        Power.max(stack, player, tooltip);
    }

    @Override
    public boolean canConnect(LevelReader world, BlockPos pos, BlockState state, Direction side) {
        return side != Direction.DOWN;
    }
}
