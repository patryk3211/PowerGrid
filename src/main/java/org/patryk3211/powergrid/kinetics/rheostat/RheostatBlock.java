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
package org.patryk3211.powergrid.kinetics.rheostat;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.kinetics.base.TunedBlock;

import java.util.List;

public class RheostatBlock extends TunedBlock implements IBE<RheostatBlockEntity>, IHaveElectricProperties {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(2, 2, 2, 14, 10, 14),
            box(5, 10, 5, 11, 16, 11)
    );
    private static final VoxelShape SHAPE_NORTH_BASELESS = Shapes.or(
            box(5, 0, 5, 11, 2, 11),
            box(2, 2, 2, 14, 10, 14),
            box(5, 10, 5, 11, 16, 11)
    );

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 10, 2, 0, 12, 4, 1),
            new TerminalBoundingBox(IDecoratedTerminal.TAP, 7, 2, 0, 9, 4, 1),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4, 2, 0, 6, 4, 1)
    };

    public RheostatBlock(Properties properties) {
        super(properties);
        setTerminalCollection(tunedNorthTerminals(this, TERMINALS_NORTH, SHAPE_NORTH, SHAPE_NORTH_BASELESS));
    }

    @Override
    public Class<RheostatBlockEntity> getBlockEntityClass() {
        return RheostatBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RheostatBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.RHEOSTAT.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Power.max(stack, player, tooltip);
    }
}
