package org.patryk3211.powergrid.general.ceilingtile.solar;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlock;
import org.patryk3211.powergrid.general.ceilingtile.CeilingBlock;

import java.util.List;

public class CeilingTileSolarBlock extends ElectricBlock implements IBE<CeilingTileSolarBlockEntity>, CeilingBlock, SpecialBlockItemRequirement, IAcceptConnector {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 4, 16);

    public CeilingTileSolarBlock(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return placementHelper(state, level, pos, player, hand, hit);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if(context.getClickedFace().getAxis() != Direction.Axis.Y)
            return removeCeilingAttachment(context, ModdedBlocks.SOLAR_PANEL.asStack());
        var pos = context.getClickedPos();
        Direction maxDir = SolarPanelBlock.getInteractionDirection(Direction.UP, context);
        if(maxDir == null)
            return removeCeilingAttachment(context, ModdedBlocks.SOLAR_PANEL.asStack());
        var level = context.getLevel();
        if(!(level.getBlockEntity(pos) instanceof CeilingTileSolarBlockEntity thisSolarBE))
            return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos.relative(maxDir)) instanceof CeilingTileSolarBlockEntity neighborBE))
            return InteractionResult.FAIL;
        if(CeilingTileSolarBlockEntity.areConnected(thisSolarBE, neighborBE)) {
            var controller = thisSolarBE.getController();
            if(controller.isEmpty())
                return InteractionResult.FAIL;
            CeilingTileSolarBlockEntity.splitMultiblock(controller.get(), pos.get(maxDir.getAxis()), maxDir);
            if(level.isClientSide)
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        } else {
            var controller1 = thisSolarBE.getController();
            var controller2 = neighborBE.getController();
            if(controller1.isEmpty() || controller2.isEmpty())
                return InteractionResult.FAIL;
            CeilingTileSolarBlockEntity.mergeMultiblock(controller1.get(), controller2.get());
            if(level.isClientSide)
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
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

    @Override
    public Class<CeilingTileSolarBlockEntity> getBlockEntityClass() {
        return CeilingTileSolarBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CeilingTileSolarBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CEILING_TILE_SOLAR.get();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ModdedBlocks.SOLAR_PANEL.asStack();
    }
}
