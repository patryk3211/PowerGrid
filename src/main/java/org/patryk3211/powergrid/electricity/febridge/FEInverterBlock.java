package org.patryk3211.powergrid.electricity.febridge;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class FEInverterBlock extends Rotation4ElectricBlock implements IBE<FEInverterBlockEntity> {
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
}
