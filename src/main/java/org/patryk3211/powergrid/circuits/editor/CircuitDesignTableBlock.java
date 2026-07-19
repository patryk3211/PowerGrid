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
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class CircuitDesignTableBlock extends HorizontalElectricBlock implements IBE<CircuitDesignTableBlockEntity>, ISocketElectric, IHaveElectricProperties {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShaper SHAPER = VoxelShaper.forHorizontal(Shapes.or(
            box(3, 0, 3, 13, 12, 13),
            box(0, 11, 2, 16, 14, 14),
            box(4, 3, 12, 12, 11, 16)
    ), Direction.NORTH);

    private static final TerminalBoundingBox SOCKET_NORTH = new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 6, 5, 14, 10, 9, 18);
    private static final TerminalBoundingBox SOCKET_SOUTH = SOCKET_NORTH.rotateAroundY(180);
    private static final TerminalBoundingBox SOCKET_EAST = SOCKET_NORTH.rotateAroundY(90);
    private static final TerminalBoundingBox SOCKET_WEST = SOCKET_NORTH.rotateAroundY(-90);

    public CircuitDesignTableBlock(Properties settings) {
        super(settings.lightLevel(state -> state.getValue(POWERED) ? 7 : 0));
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPER.get(state.getValue(HORIZONTAL_FACING));
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
        withBlockEntityDo(world, pos, be -> {
            if(!player.isCreative() && !be.isPowered()) {
                player.displayClientMessage(Lang.translate("message.circuit_table.no_power")
                        .style(ChatFormatting.RED)
                        .component(), true);
                return;
            }
            MenuRegistry.openExtendedMenu((ServerPlayer) player, be, be::sendToMenu);
        });
        return InteractionResult.SUCCESS;
    }

    @Override
    public ITerminalPlacement socket(BlockState state) {
        return switch(state.getValue(HORIZONTAL_FACING)) {
            case NORTH -> SOCKET_NORTH;
            case SOUTH -> SOCKET_SOUTH;
            case EAST -> SOCKET_EAST;
            case WEST -> SOCKET_WEST;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        double R = ResistanceValues.get(this);
        float P = 30;
        Voltage.rated(Math.round(Math.sqrt(P * R)), player, tooltip);
        Power.rated(P, player, tooltip);
    }
}
