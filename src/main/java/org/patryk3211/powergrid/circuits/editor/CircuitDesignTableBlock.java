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
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class CircuitDesignTableBlock extends Block implements IBE<CircuitDesignTableBlockEntity> {
    public CircuitDesignTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Class<CircuitDesignTableBlockEntity> getBlockEntityClass() {
        return CircuitDesignTableBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CircuitDesignTableBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CIRCUIT_DESIGN_BENCH.get();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient)
            return ActionResult.SUCCESS;
        withBlockEntityDo(world, pos, be -> NetworkHooks.openScreen((ServerPlayerEntity) player, be, be::sendToMenu));
        return ActionResult.SUCCESS;

    }
}
