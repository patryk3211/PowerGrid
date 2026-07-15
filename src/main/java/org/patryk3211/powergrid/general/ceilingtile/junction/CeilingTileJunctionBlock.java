package org.patryk3211.powergrid.general.ceilingtile.junction;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.general.ceilingtile.CeilingBlock;

import java.util.List;

public class CeilingTileJunctionBlock extends ElectricBlock implements IBE<CeilingTileJunctionBlockEntity>, CeilingBlock, SpecialBlockItemRequirement {
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16),
            box(4, 2, 4, 12, 5, 12)
    );

    public CeilingTileJunctionBlock(Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return removeCeilingAttachment(context, ModdedBlocks.CORD_JUNCTION.asStack());
    }

    @Override
    public Class<CeilingTileJunctionBlockEntity> getBlockEntityClass() {
        return CeilingTileJunctionBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CeilingTileJunctionBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CEILING_TILE_JUNCTION.get();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState blockState, @Nullable BlockEntity blockEntity) {
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, List.of(
                ModdedBlocks.CEILING_TILE.asStack(),
                ModdedBlocks.CORD_JUNCTION.asStack()
        ));
    }
}
