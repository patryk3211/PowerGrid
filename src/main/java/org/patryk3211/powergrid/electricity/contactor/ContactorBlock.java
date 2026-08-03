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
package org.patryk3211.powergrid.electricity.contactor;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.base.IHelperTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class ContactorBlock extends HorizontalAxisElectricBlock implements IBE<ContactorBlockEntity>, IHaveElectricProperties, IAcceptConnector, IHelperTerminal {
    public static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(0, 0, 0, 16, 12, 16),
            box(0, 12, 4, 16, 14, 12),
            box(6, 12, 0, 10, 13, 4),
            box(6, 12, 12, 10, 13, 16)
    );

    public static final Component SWITCH1 = Lang.builder()
            .translate("contactor.switch1")
            .style(ChatFormatting.GRAY)
            .component();
    public static final Component SWITCH2 = Lang.builder()
            .translate("contactor.switch2")
            .style(ChatFormatting.GRAY)
            .component();

    public static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            null, null,
            new TerminalBoundingBox(SWITCH1, 2, 10, 0, 6, 12, 4),
            new TerminalBoundingBox(SWITCH1, 2, 10, 12, 6, 12, 16),
            new TerminalBoundingBox(SWITCH2, 10, 10, 0, 14, 12, 4),
            new TerminalBoundingBox(SWITCH2, 10, 10, 12, 14, 12, 16)
    };

    public ContactorBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalZTerminals(this, TERMINALS_NORTH, SHAPE_NORTH));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean accepts(ItemStack wireStack) {
        return true;
    }

    @Override
    public Class<ContactorBlockEntity> getBlockEntityClass() {
        return ContactorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ContactorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CONTACTOR.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.coil(resistance("coil"), player, tooltip);
        Voltage.rated(24, player, tooltip);
        Resistance.switchResistance(resistance("switch"), player, tooltip);
        // Subtract the coil's rated power.
        Current.max(resistance("switch"), ThermalValues.getPower(this) - 48, player, tooltip);
    }

    @Override
    public boolean canConnect(LevelReader world, BlockPos pos, BlockState state, Direction side) {
        return side.getAxis() != Direction.Axis.Y && state.getValue(HORIZONTAL_AXIS) != side.getAxis();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(ContactorBlockEntity::getSignal).orElse(0);
    }

    @Override
    public @Nullable ITerminalPlacement connectedTerminal(BlockState state, int index) {
        return switch(index) {
            case 2 -> terminal(state, 3);
            case 3 -> terminal(state, 2);
            case 4 -> terminal(state, 5);
            case 5 -> terminal(state, 4);
            default -> null;
        };
    }
}
