package org.patryk3211.powergrid.general.ceilingtile.junction;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;
import org.patryk3211.powergrid.general.ceilingtile.CeilingBlock;

import java.util.List;

import static org.patryk3211.powergrid.electricity.wireconnector.CordJunctionBlock.pickPoint;

public class CeilingTileJunctionBlock extends ElectricBlock implements IBE<CeilingTileJunctionBlockEntity>, CeilingBlock, SpecialBlockItemRequirement, IAcceptCord {
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return placementHelper(state, level, pos, player, hand, hit);
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

    @Override
    public @Nullable AutoCordEndpoint getEndpoint(UseOnContext context) {
        var pos = context.getClickedPos();

        var center = Vec3.atCenterOf(pos);
        var point = center.add(0, -0.28125, 0);

        var loc = context.getClickLocation();
        var offset = new Vec3(
                pickPoint(loc.x, center.x),
                pickPoint(loc.y, center.y),
                pickPoint(loc.z, center.z)
        );
        if(offset.x == 0 && offset.z == 0)
            point = point.add(0, 0.0625, 0);
        else
            point = point.add(offset.x, 0, offset.z);
        return new AutoCordEndpoint(pos, 0, 1, point, null);
    }

    @Override
    public @Nullable ITerminalPlacement cordTerminal(BlockState state, Level level, BlockHitResult hit) {
        var center = Vec3.atCenterOf(hit.getBlockPos());
        var point = new Vec3(0, -0.28125, 0);

        var loc = hit.getLocation();
        var offset = new Vec3(
                pickPoint(loc.x, center.x),
                pickPoint(loc.y, center.y),
                pickPoint(loc.z, center.z)
        );
        if(offset.x == 0 && offset.z == 0)
            point = point.add(0, 0.0625, 0);
        else
            point = point.add(offset.x, 0, offset.z);
        point = point.scale(16).add(8, 8, 8);
        return new TerminalBoundingBox(IDecoratedTerminal.CORD,
                point.x - 1.5, point.y - 1.5, point.z - 1.5,
                point.x + 1.5, point.y + 1.5, point.z + 1.5);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ModdedBlocks.CORD_JUNCTION.asStack();
    }
}
