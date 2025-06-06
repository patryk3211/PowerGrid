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
package org.patryk3211.powergrid.ponder.base;

import com.simibubi.create.foundation.ponder.PonderWorld;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;

public class InvisibleWireElement extends WireElement {
    public InvisibleWireElement(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, float resistance) {
        super(pos1, terminal1, pos2, terminal2, resistance);
    }

    @Override
    protected void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {
        if(wire == null && isVisible()) {
            var hWire = HangingWireEntity.create(world, new BlockWireEndpoint(pos1, terminal1), new BlockWireEndpoint(pos2, terminal2), ModdedItems.WIRE.asStack(), resistance);
            hWire.updateRenderParams();
            wire = hWire;
        }
    }
}
