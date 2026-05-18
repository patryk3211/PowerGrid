package org.patryk3211.powergrid.electricity.redstoneconverter;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.Current;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Resistance;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class RedstoneConverterBlock extends Rotation4ElectricBlock implements IBE<RedstoneConverterBlockEntity>, IHaveElectricProperties {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final Component NINV = Lang.builder()
            .translate("redconv.noninverting")
            .style(ChatFormatting.GRAY)
            .component();
    private static final Component INV = Lang.builder()
            .translate("redconv.inverting")
            .style(ChatFormatting.GRAY)
            .component();

    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(NINV, 9, 0, 12, 11, 2, 14)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(INV, 5, 0, 12, 7, 2, 14)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.TAP, 7, 0, 2, 9, 2, 4)
                    .withColor(IDecoratedTerminal.GRAY)
    };

    private static final VoxelShape SHAPE_DOWN = box(4, 0, 4, 12, 3, 12);

    public RedstoneConverterBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
        setTerminalCollection(rotation4DownTerminals(this, TERMINALS_DOWN, SHAPE_DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public Class<RedstoneConverterBlockEntity> getBlockEntityClass() {
        return RedstoneConverterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneConverterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.REDSTONE_CONVERTER.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Resistance.minimum(resistance("min"), player, tooltip);
        var power = ThermalValues.getPower(this);
        Current.max(resistance("min"), power, player, tooltip);
    }
}
