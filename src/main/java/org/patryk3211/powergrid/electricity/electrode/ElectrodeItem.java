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

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.patryk3211.powergrid.chemistry.vat.upgrade.ChemicalVatUpgrade;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.utility.Directions;

import java.util.function.Supplier;

public class ElectrodeItem extends ChemicalVatUpgrade {
    @Environment(EnvType.CLIENT)
    protected Supplier<PartialModel> modelProvider;

    public ElectrodeItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isSided() {
        return true;
    }

    @Override
    public Direction getSide(BlockHitResult hit) {
        if(hit.getSide().getAxis() == Direction.Axis.Y) {
            var localPos = hit.getPos().subtract(hit.getBlockPos().toCenterPos());
            return Direction.getFacing(localPos.x, 0, localPos.z);
//            var x = localPos.x;
//            var z = localPos.z;
//
//            if(Math.abs(x) >= Math.abs(z)) {
//                // X axis
//                if(x >= 0) {
//                    // Positive
//                    return Direction.EAST;
//                } else {
//                    // Negative
//                    return Direction.WEST;
//                }
//            } else {
//                // Z axis
//                if(z >= 0) {
//                    // Positive
//                    return Direction.SOUTH;
//                } else {
//                    // Negative
//                    return Direction.NORTH;
//                }
//            }
        }
        return super.getSide(hit);
    }

    public static <I extends ElectrodeItem, P> NonNullUnaryOperator<ItemBuilder<I, P>> setModel(Supplier<Supplier<PartialModel>> modelProvider) {
        return b -> {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> b.onRegister(item -> item.modelProvider = modelProvider.get()));
            return b;
        };
    }

    private BlockState placementState(Direction dir) {
        return ModdedBlocks.VAT_ELECTRODE.getDefaultState()
                .with(Directions.property(dir), true);
    }

    @Override
    public boolean canApply(ChemicalVatBlockEntity vat, Direction side) {
        if(side == Direction.UP || side == Direction.DOWN)
            return false;

        var world = vat.getWorld();
        var electrodePos = vat.getPos().up();

        var state = world.getBlockState(electrodePos);
        if(state.getBlock() instanceof VatElectrodeBlock)
            return true;

        var newState = placementState(side);
        return world.canPlace(newState, electrodePos, ShapeContext.absent());
    }

    @Override
    public void readUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, @Nullable Direction side) {
    }

    @Override
    public void readRemovedUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, @Nullable Direction side) {
    }

    @Override
    public void applyUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, Direction dir) {
        var world = vat.getWorld();
        var electrodePos = vat.getPos().up();

        var state = world.getBlockState(electrodePos);
        if(state.getBlock() instanceof VatElectrodeBlock) {
            // Add electrode to existing block.
            world.setBlockState(electrodePos, state.with(Directions.property(dir), true));
            var be = world.getBlockEntity(electrodePos);
            if(be instanceof VatElectrodeBlockEntity electrode)
                electrode.getElectricBehaviour().rebuildCircuit();
            return;
        }
        world.setBlockState(electrodePos, placementState(dir));
    }

    @Override
    public void removeUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, Direction dir) {
        var world = vat.getWorld();
        var electrodePos = vat.getPos().up();

        var state = world.getBlockState(electrodePos);
        if(state.getBlock() instanceof VatElectrodeBlock electrodeBlock) {
            // Remove electrode from existing block.
            var newState = state.with(Directions.property(dir), false);
            newState = electrodeBlock.processState(newState);
            world.setBlockState(electrodePos, newState);

            if(newState.isOf(electrodeBlock)) {
                var be = world.getBlockEntity(electrodePos);
                if(be instanceof VatElectrodeBlockEntity electrode)
                    electrode.getElectricBehaviour().rebuildCircuit();
            }
        }
    }

    @Override
    public void render(ChemicalVatBlockEntity vat, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, ItemStack stack, @Nullable Direction side, int light, int overlay) {
        ms.push();
        ms.translate(0, 1, 0);
        var state = vat.getCachedState();
        var model = CachedBufferer.partial(modelProvider.get(), state);

        var world = vat.getWorld();
        var pos = vat.getPos().up();
        var blockLight = world.getLightLevel(LightType.BLOCK, pos);
        var skyLight = world.getLightLevel(LightType.SKY, pos);

        var angle = (180 - side.asRotation()) / 180 * Math.PI;
        model.rotateCentered(Direction.UP, (float) angle)
                .light(LightmapTextureManager.pack(blockLight, skyLight))
                .renderInto(ms, bufferSource.getBuffer(RenderLayer.getSolid()));
        ms.pop();
    }
}
