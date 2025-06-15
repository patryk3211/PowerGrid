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
package org.patryk3211.powergrid.electricity.particles;

import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector3f;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class ZapParticle extends Particle {
    private Vector3f end;
    private Vector3f delta;
    private Vector3f cross1;
    private Vector3f cross2;

    public ZapParticle(ZapParticleData data, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        end = data.getEnd();
        maxAge = 1;
        blue = 0.5f;

        delta = new Vector3f((float) (end.x - this.x), (float) (end.y - this.y), (float) (end.z - this.z));
        var vec = new Vector3f(1 - delta.x, 1 - delta.y, 1 - delta.z);
        cross1 = new Vector3f();
        vec.cross(delta, cross1).normalize().mul(0.02f);
        cross2 = new Vector3f();
        cross1.cross(delta, cross2).normalize().mul(0.02f);
    }

    @Override
    public void tick() {
        if(age++ >= maxAge) {
            markDead();
        } else {

        }
    }

    public void renderSegment(VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, Vector3f cross1, Vector3f cross2) {
        buffer.vertex(x1 + cross1.x, y1 + cross1.y, z1 + cross1.z).next();
        buffer.vertex(x1 - cross1.x, y1 - cross1.y, z1 - cross1.z).next();
        buffer.vertex(x2 - cross1.x, y2 - cross1.y, z2 - cross1.z).next();
        buffer.vertex(x2 + cross1.x, y2 + cross1.y, z2 + cross1.z).next();

        buffer.vertex(x1 + cross2.x, y1 + cross2.y, z1 + cross2.z).next();
        buffer.vertex(x1 - cross2.x, y1 - cross2.y, z1 - cross2.z).next();
        buffer.vertex(x2 - cross2.x, y2 - cross2.y, z2 - cross2.z).next();
        buffer.vertex(x2 + cross2.x, y2 + cross2.y, z2 + cross2.z).next();
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        var bufferProvider = SuperRenderTypeBuffer.getInstance();
        var buffer = bufferProvider.getBuffer(ModdedRenderLayers.getColor());

        var camPos = camera.getPos();

        buffer.fixedColor((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));

        var pos = new Vector3f((float) this.x, (float) this.y, (float) this.z).sub(camPos.toVector3f());
        var straightPos = new Vector3f(pos);
        var segmentCount = Math.max((int) (delta.length() / 0.5f), 3);
        var segmentVector = new Vector3f(delta).mul(1.0f / segmentCount);
        var segmentLength = delta.length() / segmentCount;
        for(int i = 0; i < segmentCount; ++i) {
            var straightEndPos = new Vector3f(segmentVector).add(straightPos);
            var endPos = new Vector3f(segmentVector).add(pos);
            endPos
                    .add(new Vector3f(cross1).mul(random.nextFloat() - 0.5f).mul(50))
                    .add(new Vector3f(cross2).mul(random.nextFloat() - 0.5f).mul(50));
            var middle = segmentCount / 2;
            float factor = 1.0f - ((float) Math.abs(middle - i - 1) / (middle + 1));
            float invFactor = 1.0f - factor;
            endPos.mul(factor).add(straightEndPos.x * invFactor, straightEndPos.y * invFactor, straightEndPos.z * invFactor);
            renderSegment(buffer,
                    pos.x, pos.y, pos.z,
                    endPos.x, endPos.y, endPos.z,
                    cross1, cross2);
            if(i != 0 && random.nextFloat() < 0.2f) {
                var x = (random.nextFloat() - 0.5f) * segmentLength * 2;
                var y = (random.nextFloat() - 0.5f) * segmentLength * 2;
                var z = (random.nextFloat() - 0.5f) * segmentLength * 2;
                renderSegment(buffer, pos.x, pos.y, pos.z, pos.x + x, pos.y + y, pos.z + z, cross1, cross2);
                if(random.nextFloat() < 0.2f) {
                    var x2 = x + (random.nextFloat() - 0.5f);
                    var y2 = y + (random.nextFloat() - 0.5f);
                    var z2 = z + (random.nextFloat() - 0.5f);
                    renderSegment(buffer, pos.x + x, pos.y + y, pos.z + z, pos.x + x2, pos.y + y2, pos.z + z2, cross1, cross2);
                }
            }
            straightPos = straightEndPos;
            pos = endPos;
        }
        buffer.unfixColor();

        bufferProvider.draw();
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }
}
