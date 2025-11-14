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
package org.patryk3211.powergrid.electricity.crt;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.utility.Lang;

public class CRTBlock extends HorizontalElectricBlock implements IBE<CRTBlockEntity> {
    private static final Component HEATER = Lang.builder()
            .translate("crt.heater")
            .style(ChatFormatting.RED).component();
    private static final Component CATHODE = Lang.builder()
            .translate("crt.cathode")
            .style(ChatFormatting.BLUE).component();
    private static final Component GRID = Lang.builder()
            .translate("crt.grid")
            .style(ChatFormatting.GRAY).component();
    private static final Component ANODE = Lang.builder()
            .translate("crt.anode")
            .style(ChatFormatting.RED).component();

    private static final Component X_COIL = Lang.builder()
            .translate("crt.coil.x")
            .style(ChatFormatting.RED).component();
    private static final Component Y_COIL = Lang.builder()
            .translate("crt.coil.y")
            .style(ChatFormatting.RED).component();
    private static final Component COMMON_COIL = Lang.builder()
            .translate("crt.coil.common")
            .style(ChatFormatting.BLUE).component();

    private static final VoxelShape SHAPE = Shapes.or(
            box(4, 2, 0, 12, 10, 12),
            box(3, 1, 6, 13, 11, 9),
            box(5, 3, 12, 11, 9, 15),
            box(4, 0, 1, 12, 2, 10)
    );

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(CATHODE, 5, 5, 15, 6, 7, 16)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(HEATER, 10, 5, 15, 11, 7, 16)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(GRID, 7, 3, 15, 9, 4, 16),
            new TerminalBoundingBox(ANODE, 3, 6, 2, 4, 7, 3)
                    .withColor(IDecoratedTerminal.RED),

            new TerminalBoundingBox(X_COIL, 2, 2, 7, 3, 4, 8)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(Y_COIL, 13, 2, 7, 14, 4, 8)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(COMMON_COIL, 7, 11, 7, 9, 12, 8)
                    .withColor(IDecoratedTerminal.BLUE),
    };

    public CRTBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, TERMINALS, SHAPE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ? ctx.getHorizontalDirection().getOpposite() : ctx.getHorizontalDirection();
        return defaultBlockState().setValue(HORIZONTAL_FACING, player);
    }

    @Override
    public Class<CRTBlockEntity> getBlockEntityClass() {
        return CRTBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CRTBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CRT.get();
    }
}
