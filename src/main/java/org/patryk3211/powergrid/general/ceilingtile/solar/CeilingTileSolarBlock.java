package org.patryk3211.powergrid.general.ceilingtile.solar;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.general.ceilingtile.CeilingBlock;

import java.util.List;

public class CeilingTileSolarBlock extends ElectricBlock implements CeilingBlock, SpecialBlockItemRequirement, IAcceptConnector {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 4, 16);

    public CeilingTileSolarBlock(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return removeCeilingAttachment(context, ModdedBlocks.SOLAR_PANEL.asStack());
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState blockState, @Nullable BlockEntity blockEntity) {
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, List.of(
                ModdedBlocks.CEILING_TILE.asStack(),
                ModdedBlocks.SOLAR_PANEL.asStack()
        ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
