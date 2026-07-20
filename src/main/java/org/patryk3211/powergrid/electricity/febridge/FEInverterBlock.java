package org.patryk3211.powergrid.electricity.febridge;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class FEInverterBlock extends Rotation4ElectricBlock implements IBE<FEInverterBlockEntity>, IHaveElectricProperties {
    private static final VoxelShape SHAPE_DOWN = Shapes.or(
            box(0, 0, 0, 16, 3, 16),
            box(1, 3, 1, 15, 6, 15)
    );

    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 10, 4, 0, 12, 7, 3)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 7, 4, 0, 9, 7, 3)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.CONTROL, 4, 4, 0, 6, 7, 3)
                    .withColor(IDecoratedTerminal.GREEN)
    };

    public FEInverterBlock(Properties settings) {
        super(settings);
        setTerminalCollection(rotation4DownTerminals(this, TERMINALS_DOWN, SHAPE_DOWN));
    }

    @Override
    public Class<FEInverterBlockEntity> getBlockEntityClass() {
        return FEInverterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FEInverterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.FE_INVERTER.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Power.max(FEInverterBlockEntity.energyBufferSize()
                / ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF()
                / ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF(), player, tooltip);
        Lang.translate("tooltip.device_connector.rate").style(ChatFormatting.GRAY).addTo(tooltip);
        LangBuilder valueText = Lang.builder().add(Component.nullToEmpty(" "));
        valueText.add(Lang.number(ModdedConfigs.server().electricity.forgeEnergyPerWatt.getF()))
                .add(Component.nullToEmpty(" "))
                .add(Lang.translateDirect("tooltip.device_connector.fe_w"));
        valueText.style(ChatFormatting.DARK_AQUA).addTo(tooltip);

        Lang.translate("tooltip.fe_inverter.voltage").style(ChatFormatting.GRAY).addTo(tooltip);
        valueText = Lang.builder().add(Component.nullToEmpty(" "));
        valueText.add(Lang.number(ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF()))
                .add(Component.nullToEmpty(" "))
                .add(Lang.translateDirect("tooltip.fe_inverter.fe_v"));
        valueText.style(ChatFormatting.DARK_AQUA).addTo(tooltip);
    }
}
