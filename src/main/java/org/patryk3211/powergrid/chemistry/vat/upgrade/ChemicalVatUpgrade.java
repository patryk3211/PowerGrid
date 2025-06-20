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
package org.patryk3211.powergrid.chemistry.vat.upgrade;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;

public abstract class ChemicalVatUpgrade extends Item {
    public ChemicalVatUpgrade(Settings settings) {
        super(settings);
    }

    public boolean isSided() {
        return false;
    }

    public Direction getSide(BlockHitResult hit) {
        return hit.getSide();
    }

    public boolean canApply(ChemicalVatBlockEntity vat, Direction side) {
        return true;
    }

    /**
     * Called when upgrade is read from block entity NBT.
     * @param vat Upgrade holder
     * @param stack Upgrade item stack
     * @param side Side of upgrade
     */
    public void readUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, @Nullable Direction side) {
        applyUpgrade(vat, stack, side);
    }

    /**
     * Called when upgrade is removed as a result of block entity NBT update.
     * @param vat Upgrade holder
     * @param stack Upgrade item stack
     * @param side Side of upgrade
     */
    public void readRemovedUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, @Nullable Direction side) {
        removeUpgrade(vat, stack, side);
    }

    public abstract void applyUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, @Nullable Direction side);
    public abstract void removeUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, @Nullable Direction side);

    @Environment(EnvType.CLIENT)
    public abstract void render(ChemicalVatBlockEntity vat, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, ItemStack stack, @Nullable Direction side, int light, int overlay);
}
