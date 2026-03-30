package org.patryk3211.powergrid.electricity.gpu;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;

import java.util.List;

public class GPUBlock extends HorizontalElectricBlock implements IBE<GPUBlockEntity>, IHaveElectricProperties {
    private static final VoxelShape SHAPE = box(2, 0, 1, 13, 5, 23);

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 1.5, 2, 2, 2.5, 3)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 0, 1.5, 3, 2, 2.5, 4)
                    .withColor(IDecoratedTerminal.BLUE),

            new TerminalBoundingBox(IDecoratedTerminal.CONTROL, 0, 1.5, 4, 2, 2.5, 5)
                    .withColor(IDecoratedTerminal.GREEN),
            new TerminalBoundingBox(IDecoratedTerminal.CONTROL, 0, 1.5, 5, 2, 2.5, 6)
                    .withColor(IDecoratedTerminal.GREEN),
            new TerminalBoundingBox(IDecoratedTerminal.CONTROL, 0, 1.5, 6, 2, 2.5, 7)
                    .withColor(IDecoratedTerminal.GREEN),
            new TerminalBoundingBox(IDecoratedTerminal.CONTROL, 0, 1.5, 7, 2, 2.5, 8)
                    .withColor(IDecoratedTerminal.GREEN),
    };

    public GPUBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, TERMINALS, SHAPE));
    }

    @Override
    public Class<GPUBlockEntity> getBlockEntityClass() {
        return GPUBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GPUBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GPU.get();
    }

    @Override
    public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
        Voltage.rated(12, player, tooltip);
        Power.max(stack, player, tooltip);
    }
}
