/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.electrode;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.utility.Directions;

public class VatElectrodeBlock extends ElectricBlock implements IBE<VatElectrodeBlockEntity> {
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;

    private static final TerminalBoundingBox TERMINAL_NORTH = new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6.5, 2, 3, 9.5, 4, 6);

    public VatElectrodeBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(EAST, false)
                .with(WEST, false));

        var shaper = VoxelShaper.forDirectional(createCuboidShape(5, 0, 0, 11, 2, 7), Direction.NORTH);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> {
                    var terminals = new TerminalBoundingBox[4];
                    if(state.get(NORTH)) terminals[0] = TERMINAL_NORTH;
                    if(state.get(SOUTH)) terminals[1] = TERMINAL_NORTH.rotateAroundY(180);
                    if(state.get(EAST)) terminals[2] = TERMINAL_NORTH.rotateAroundY(90);
                    if(state.get(WEST)) terminals[3] = TERMINAL_NORTH.rotateAroundY(-90);
                    return terminals;
                })
                .withShapeMapper(state -> {
                    var shape = VoxelShapes.empty();
                    if(state.get(NORTH)) shape = VoxelShapes.union(shape, shaper.get(Direction.NORTH));
                    if(state.get(SOUTH)) shape = VoxelShapes.union(shape, shaper.get(Direction.SOUTH));
                    if(state.get(EAST)) shape = VoxelShapes.union(shape, shaper.get(Direction.EAST));
                    if(state.get(WEST)) shape = VoxelShapes.union(shape, shaper.get(Direction.WEST));
                    return shape;
                })
                .build());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @NotNull
    public BlockState processState(@NotNull BlockState state) {
        if(!state.get(NORTH) && !state.get(SOUTH) && !state.get(EAST) && !state.get(WEST))
            return Blocks.AIR.getDefaultState();
        return state;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if(sourceBlock instanceof ChemicalVatBlock) {
            // Owner updated
            if(!world.getBlockState(sourcePos).isOf(sourceBlock)) {
                // Owner removed.
                world.breakBlock(pos, false);
            }
        }
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        return ActionResult.FAIL;
    }

    @Override
    public ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
        var localHit = context.getHitPos().subtract(context.getBlockPos().toCenterPos());
        var dir = Direction.getFacing(localHit.x, 0, localHit.z);

        var world = context.getWorld();
        if(state.get(Directions.property(dir))) {
            // Electrode exists.
            var vat = world.getBlockEntity(context.getBlockPos().down(), ModdedBlockEntities.CHEMICAL_VAT.get());
            if(vat.isEmpty()) {
                world.breakBlock(context.getBlockPos(), true);
                return ActionResult.SUCCESS;
            }
            return vat.get().removeUpgrade(context.getPlayer(), dir);
        }
        return ActionResult.FAIL;
    }

    @Override
    public Class<VatElectrodeBlockEntity> getBlockEntityClass() {
        return VatElectrodeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VatElectrodeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.VAT_ELECTRODE.get();
    }
}
