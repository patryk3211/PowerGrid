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
package org.patryk3211.powergrid.circuits.editor;

import com.simibubi.create.foundation.block.IBE;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class CircuitDesignTableBlock extends Block implements IBE<CircuitDesignTableBlockEntity> {
    public CircuitDesignTableBlock(Properties settings) {
        super(settings);
    }

    @Override
    public Class<CircuitDesignTableBlockEntity> getBlockEntityClass() {
        return CircuitDesignTableBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CircuitDesignTableBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CIRCUIT_DESIGN_TABLE.get();
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(world.isClientSide)
            return InteractionResult.SUCCESS;
        withBlockEntityDo(world, pos, be -> MenuRegistry.openExtendedMenu((ServerPlayer) player, be, be::sendToMenu));
        return InteractionResult.SUCCESS;

    }
}
