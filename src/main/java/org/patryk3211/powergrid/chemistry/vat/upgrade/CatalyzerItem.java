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

import com.simibubi.create.foundation.render.CachedBufferer;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class CatalyzerItem extends ChemicalVatUpgrade {
    protected float strength = 1.0f;

    public CatalyzerItem(Settings settings) {
        super(settings);
    }

    public static <T extends CatalyzerItem, P> NonNullUnaryOperator<ItemBuilder<T, P>> setStrength(float strength) {
        return b -> b.onRegister(item -> item.strength = strength);
    }

    @Override
    public void applyUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, Direction side) {
        vat.getReagentMixture().setCatalyzer(strength);
    }

    @Override
    public void removeUpgrade(ChemicalVatBlockEntity vat, ItemStack stack, Direction side) {
        vat.getReagentMixture().setCatalyzer(0.0f);
    }

    @Override
    public void render(ChemicalVatBlockEntity vat, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, ItemStack stack, @Nullable Direction side, int light, int overlay) {
        if(!vat.getCachedState().get(ChemicalVatBlock.OPEN))
            return;

        var model = CachedBufferer.partial(ModdedPartialModels.VAT_SILVER_MESH, vat.getCachedState());
        model.light(light)
                .renderInto(ms, bufferSource.getBuffer(RenderLayer.getCutoutMipped()));
    }
}
