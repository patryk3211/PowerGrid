package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.block.IBE;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class EnergyMeterBlock extends HorizontalElectricBlock implements IBE<EnergyMeterBlockEntity> {
    private static final TerminalBoundingBox[] TERMINALS_NORTH = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.INPUT, 2, 16, 0, 5, 18, 2),
            new TerminalBoundingBox(IDecoratedTerminal.OUTPUT, 11, 16, 0, 14, 18, 2),
            new TerminalBoundingBox(IDecoratedTerminal.COMMON, 6.5, 16, 0, 9.5, 18, 2)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE = box(1, 0, 0, 15, 16, 6);

    public EnergyMeterBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, TERMINALS_NORTH, SHAPE));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        withBlockEntityDo(level, pos, be -> {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, be, buf -> be.sendToMenu(new RegistryFriendlyByteBuf(buf, level.registryAccess())));
        });
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(EnergyMeterBlockEntity::pulses).orElse(0);
    }

    @Override
    public Class<EnergyMeterBlockEntity> getBlockEntityClass() {
        return EnergyMeterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EnergyMeterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ENERGY_METER.get();
    }
}
