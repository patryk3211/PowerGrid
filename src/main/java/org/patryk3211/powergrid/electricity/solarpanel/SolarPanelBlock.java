package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;

import java.util.List;

public class SolarPanelBlock extends Rotation4ElectricBlock implements IBE<SolarPanelBlockEntity>,IHaveElectricProperties {

    private static final VoxelShape SHAPE = Shapes.or(
            box(4, 0, 4, 12, 1, 12),
            box(8, 2, 8, 10, 9, 10),
            box(2, 8, 2, 14, 10, 14)
    );

    private static final TerminalBoundingBox[] NORTH_TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 5, 0, 3, 6, 1, 4),
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 10, 0, 3, 11, 1, 4)
    };

    public SolarPanelBlock(Properties settings) {
        super(settings);
        setTerminalCollection(rotation4DownTerminals(this, NORTH_TERMINALS, SHAPE));
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {

    }

    @Override
    public Class<SolarPanelBlockEntity> getBlockEntityClass() {
        return SolarPanelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SolarPanelBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SOLAR_PANEL.get();
    }
}
