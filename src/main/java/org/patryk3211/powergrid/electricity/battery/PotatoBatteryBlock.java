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
package org.patryk3211.powergrid.electricity.battery;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class PotatoBatteryBlock extends AbstractBatteryBlock<PotatoBatteryBlockEntity> {
    public static final BatterySpec BATTERY_SPEC = new SimpleBatterySpec(
            0.216f,
            0.216f,
            0.5f,
            0.01f,
            e -> 0.4f * e + 0.5f,
            e -> (float) Math.exp(-6.15619f * e + 9.28731f) + 430
    );

    public static final DirectionProperty HORIZONTAL_FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty BAKED = BooleanProperty.of("baked");

    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 7, 3, 4.5, 9, 5.5, 6)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 7, 3, 10, 9, 5.5, 11.5)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_NORTH = createCuboidShape(6, 0, 5, 10, 3, 11);

    public PotatoBatteryBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(BAKED, false));
        setTerminalCollection(HorizontalElectricBlock.horizontalNorthTerminals(this, TERMINALS_NORTH, SHAPE_NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_FACING, BAKED);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isSneaking() ? ctx.getHorizontalPlayerFacing() : ctx.getHorizontalPlayerFacing().getOpposite();
        return getDefaultState().with(HORIZONTAL_FACING, player.rotateYClockwise());
    }

    @Override
    public BatterySpec getSpec() {
        return BATTERY_SPEC;
    }

    @Override
    public Class<PotatoBatteryBlockEntity> getBlockEntityClass() {
        return PotatoBatteryBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PotatoBatteryBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.POTATO_BATTERY.get();
    }
}
