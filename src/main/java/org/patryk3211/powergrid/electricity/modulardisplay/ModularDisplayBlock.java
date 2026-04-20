package org.patryk3211.powergrid.electricity.modulardisplay;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.ArrayList;
import java.util.List;

public class ModularDisplayBlock extends HorizontalElectricBlock implements IBE<ModularDisplayBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTHSHAPE = Shapes.or(
            box(0,0,0 ,16,16,13)
    );

    public ModularDisplayBlock(Properties settings) {
        super(settings);
        setTerminalCollection(horizontalNorthTerminals(this, NORTHTERMINALS, NORTHSHAPE));
    }

    @Override
    public Class<ModularDisplayBlockEntity> getBlockEntityClass() {
        return ModularDisplayBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ModularDisplayBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.MODULAR_DISPLAY.get();
    }


    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        Direction facing = state.getValue(ModularDisplayBlock.HORIZONTAL_FACING);

        if (hit.getDirection() != facing) return InteractionResult.PASS;

        Vec3 hitVec = hit.getLocation();

        double localX = hitVec.x - pos.getX();
        double localY = hitVec.y - pos.getY();
        double localZ = hitVec.z - pos.getZ();

        double faceU, faceV;

        switch (facing) {
            case NORTH -> { faceU = localX;       faceV = 1.0 - localY; }
            case SOUTH -> { faceU = 1.0 - localX; faceV = 1.0 - localY; }
            case WEST  -> { faceU = 1.0 - localZ; faceV = 1.0 - localY; }
            case EAST  -> { faceU = localZ;       faceV = 1.0 - localY; }
            default    -> { return InteractionResult.PASS; }
        }

        int col = Math.max(0, Math.min(3, (int)(faceU * 4)));
        int row = Math.max(0, Math.min(3, (int)(faceV * 4)));
        int slotIndex = row * 4 + col;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);

        if (player.getMainHandItem().getItem() instanceof DyeItem dye) {
            if (be instanceof ModularDisplayBlockEntity display) {
                display.setColor(slotIndex, dye.getDyeColor());
                return InteractionResult.CONSUME;
            }
        }

        if (be instanceof ModularDisplayBlockEntity display) {
            display.interact(slotIndex, player);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        int modulesToDrop = 0;
        if(be instanceof ModularDisplayBlockEntity displayBE) {
            for(int i = 0; i < ModularDisplayBlockEntity.SLOT_COUNT; i++) {
                if(!displayBE.getSlot(i).isEmpty()){
                    modulesToDrop += 1;
                }
            }
            if(modulesToDrop > 0) {
                var drops = new ArrayList<>(super.getDrops(state, params));
                drops.add(new ItemStack(ModdedItems.DISPLAY_MODULE.asItem(), modulesToDrop));
                return drops;
            }
        }
        return super.getDrops(state, params);
    }

    //todo does this take the cake for most terminals on a block in the game?
    private static final TerminalBoundingBox[] NORTHTERMINALS = new TerminalBoundingBox[]{

            new TerminalBoundingBox(IDecoratedTerminal.CASE_GROUND, 8, 0, 13, 9, 2, 14).withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 15, 13, 1, 16, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 2, 13, 13, 3, 14, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 4, 15, 13, 5, 16, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 6, 13, 13, 7, 14, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 8, 15, 13, 9, 16, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 10, 13, 13, 11, 14, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 12, 15, 13, 13, 16, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 14, 13, 13, 15, 14, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 11, 13, 1, 12, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 2, 9, 13, 3, 10, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 4, 11, 13, 5, 12, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 6, 9, 13, 7, 10, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 8, 11, 13, 9, 12, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 10, 9, 13, 11, 10, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 12, 11, 13, 13, 12, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 14, 9, 13, 15, 10, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 7, 13, 1, 8, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 2, 5, 13, 3, 6, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 4, 7, 13, 5, 8, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 6, 5, 13, 7, 6, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 8, 7, 13, 9, 8, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 10, 5, 13, 11, 6, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 12, 7, 13, 13, 8, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 14, 5, 13, 15, 6, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 0, 3, 13, 1, 4, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 2, 1, 13, 3, 2, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 4, 3, 13, 5, 4, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 6, 1, 13, 7, 2, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 8, 3, 13, 9, 4, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 10, 1, 13, 11, 2, 14),

            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 12, 3, 13, 13, 4, 14).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.RESET, 14, 1, 13, 15, 2, 14),

    };
}
