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
package org.patryk3211.powergrid.electricity.heater;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;

import java.util.List;

@MethodsReturnNonnullByDefault
public class HeaterBlock extends HorizontalElectricBlock implements IBE<HeaterBlockEntity>, IHaveElectricProperties, IAcceptCord {
    private static final TerminalBoundingBox[] NORTH_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 12, 12, 7, 15, 15, 10),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 1, 12, 7, 4, 15, 10)
    };

    private static final VoxelShape SHAPE_NORTH = box(0, 0, 5, 16, 12, 11);

    public HeaterBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, NORTH_TERMINALS, SHAPE_NORTH));
    }

    @Override
    public Class<HeaterBlockEntity> getBlockEntityClass() {
        return HeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeaterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.HEATING_COIL.get();
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.series(resistance(), player, tooltip);
        Power.max(stack, player, tooltip);
    }

    @Override
    public boolean renderPlug() {
        return true;
    }
}
