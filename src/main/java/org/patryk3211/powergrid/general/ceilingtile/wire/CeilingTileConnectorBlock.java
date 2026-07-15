package org.patryk3211.powergrid.general.ceilingtile.wire;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.general.ceilingtile.CeilingBlock;

import java.util.List;

public class CeilingTileConnectorBlock extends ElectricBlock implements IBE<CeilingTileConnectorBlockEntity>, CeilingBlock, SpecialBlockItemRequirement {
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(5, 2, 5, 11, 5, 11),
            box(6, 5, 6, 10, 12,10)
    );

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 5, 6, 10, 12,10)
                    .withOrigin(8, 10, 8)
    };

    public CeilingTileConnectorBlock(Properties settings) {
        super(settings);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> TERMINALS)
                .withShapeMapper(state -> SHAPE)
                .build());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return removeCeilingAttachment(context, ModdedBlocks.WIRE_CONNECTOR.asStack());
    }

    @Override
    public Class<CeilingTileConnectorBlockEntity> getBlockEntityClass() {
        return CeilingTileConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CeilingTileConnectorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CEILING_TILE_CONNECTOR.get();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState blockState, @Nullable BlockEntity blockEntity) {
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, List.of(
                ModdedBlocks.CEILING_TILE.asStack(),
                ModdedBlocks.WIRE_CONNECTOR.asStack()
        ));
    }
}
