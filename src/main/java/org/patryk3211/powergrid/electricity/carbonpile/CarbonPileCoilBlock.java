package org.patryk3211.powergrid.electricity.carbonpile;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class CarbonPileCoilBlock extends HorizontalElectricBlock implements IBE<CarbonPileCoilBlockEntity> {
    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 4, 1, 1, 7, 3, 2),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 9, 1, 1, 12, 3, 2),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 0, 13, 6, 1, 15, 10),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 15, 13, 6, 16, 15, 10)
    };

    private static final VoxelShape SHAPE = Shapes.or(
            box(2, 0, 2, 14, 12, 14),
            box(1, 12, 1, 15, 16, 15)
    );

    public CarbonPileCoilBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, TERMINALS, SHAPE));
    }

    @Override
    public Class<CarbonPileCoilBlockEntity> getBlockEntityClass() {
        return CarbonPileCoilBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CarbonPileCoilBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CARBON_PILE_COIL.get();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if(neighborPos.equals(pos.above())) {
            // Only the block above matters
        }
    }
}
