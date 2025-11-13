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
    private static final Component GUN_POSITIVE = Lang.builder()
            .translate("crt.gun.positive")
            .style(ChatFormatting.RED).component();
    private static final Component GUN_NEGATIVE = Lang.builder()
            .translate("crt.gun.negative")
            .style(ChatFormatting.BLUE).component();
    private static final Component X_POSITIVE = Lang.builder()
            .translate("crt.x.positive")
            .style(ChatFormatting.RED).component();
    private static final Component X_NEGATIVE = Lang.builder()
            .translate("crt.x.negative")
            .style(ChatFormatting.BLUE).component();
    private static final Component Y_POSITIVE = Lang.builder()
            .translate("crt.y.positive")
            .style(ChatFormatting.RED).component();
    private static final Component Y_NEGATIVE = Lang.builder()
            .translate("crt.y.negative")
            .style(ChatFormatting.BLUE).component();

    private static final VoxelShape SHAPE = Shapes.or(
            box(4, 2, 0, 12, 10, 12),
            box(3, 1, 6, 13, 11, 9),
            box(5, 3, 12, 11, 9, 15),
            box(4, 0, 1, 12, 2, 10)
    );

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(GUN_POSITIVE, 5, 5, 15, 6, 7, 16)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(GUN_NEGATIVE, 10, 5, 15, 11, 7, 16)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(X_POSITIVE, 2, 2, 7, 3, 4, 8)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(X_NEGATIVE, 13, 2, 7, 14, 4, 8)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(Y_POSITIVE, 7, 11, 7, 9, 12, 8)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(Y_NEGATIVE, 7, 3, 15, 9, 4, 16)
                    .withColor(IDecoratedTerminal.BLUE)
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
