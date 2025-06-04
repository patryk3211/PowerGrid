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

import com.simibubi.create.foundation.ponder.ElementLink;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.element.AnimatedSceneElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

public class WireElement extends AnimatedSceneElement {
    protected WireEntity wire;

    protected BlockPos pos1, pos2;
    protected int terminal1, terminal2;
    protected float resistance;

    public WireElement(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, float resistance) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.terminal1 = terminal1;
        this.terminal2 = terminal2;
        this.resistance = resistance;
    }

    @Override
    public void reset(PonderScene scene) {
        super.reset(scene);
        if(wire != null) {
            wire.discard();
            wire = null;
        }
    }

    @Override
    protected void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float fade, float pt) {
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        if(wire == null && isVisible()) {
            var hWire = HangingWireEntity.create(world, new BlockWireEndpoint(pos1, terminal1), new BlockWireEndpoint(pos2, terminal2), ModdedItems.WIRE.asStack(), resistance);
            hWire.updateRenderParams();
            wire = hWire;
        }

        ms.push();
        ms.translate(
                MathHelper.lerp(pt, wire.prevX, wire.getX()),
                MathHelper.lerp(pt, wire.prevY, wire.getY()),
                MathHelper.lerp(pt, wire.prevZ, wire.getZ())
        );

        dispatcher.render(wire, 0, 0, 0, 0, pt, ms, buffer, lightCoordsFromFade(fade));
        ms.pop();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(!visible) {
            if(wire != null) {
                wire.discard();
                wire = null;
            }
        }
    }
}
