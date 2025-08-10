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

import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Pair;
import org.joml.Vector3f;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

import java.util.ArrayList;
import java.util.List;

public class ZapParticle extends Particle {
    private final Vector3f delta;
    private final Vector3f cross1;
    private final Vector3f cross2;
    private final boolean anchorEnd;
    private final int segmentCount;

    private final List<Pair<Vector3f, Vector3f>> segments = new ArrayList<>();

    public ZapParticle(ZapParticleData data, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        Vector3f end = data.getEnd();
        anchorEnd = data.isAnchored();
        maxAge = data.getLife();
        segmentCount = data.getSegmentCount();
        blue = 0.5f;

        delta = new Vector3f((float) (end.x - this.x), (float) (end.y - this.y), (float) (end.z - this.z));
        var vec = new Vector3f(1 - delta.x, 1 - delta.y, 1 - delta.z);
        cross1 = new Vector3f();
        vec.cross(delta, cross1).normalize().mul(0.02f);
        cross2 = new Vector3f();
        cross1.cross(delta, cross2).normalize().mul(0.02f);

        makeNewSegments();
    }

    public void addSegment(Vector3f pos1, Vector3f pos2) {
        segments.add(new Pair<>(pos1, pos2));
    }

    public void addSegment(float x1, float y1, float z1, float x2, float y2, float z2) {
        segments.add(new Pair<>(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2)));
    }

    public void makeNewSegments() {
        segments.clear();
        var pos = new Vector3f((float) this.x, (float) this.y, (float) this.z);
        var straightPos = new Vector3f(pos);
        var segmentCount = this.segmentCount == -1 ? Math.max((int) (delta.length() / 0.5f), 3) : this.segmentCount;
        var segmentVector = new Vector3f(delta).mul(1.0f / segmentCount);
        var segmentLength = delta.length() / segmentCount;
        float totalLength = delta.length();
        for(int i = 0; i < segmentCount; ++i) {
            var straightEndPos = new Vector3f(segmentVector).add(straightPos);
            var endPos = new Vector3f(segmentVector).add(pos);
            endPos
                    .add(new Vector3f(cross1).mul(random.nextFloat() - 0.5f).mul(totalLength * 50))
                    .add(new Vector3f(cross2).mul(random.nextFloat() - 0.5f).mul(totalLength * 50));
            var middle = segmentCount / 2;
            float factor;
            if(anchorEnd) {
                factor = 1.0f - ((float) Math.abs(middle - i - 1) / (middle + 1));
            } else {
                factor = (float) i / middle;
            }
            float invFactor = 1.0f - factor;
            endPos.mul(factor).add(straightEndPos.x * invFactor, straightEndPos.y * invFactor, straightEndPos.z * invFactor);

            addSegment(pos, endPos);
            if(i != 0 && random.nextFloat() < 0.2f) {
                var x = (random.nextFloat() - 0.5f) * segmentLength * 2;
                var y = (random.nextFloat() - 0.5f) * segmentLength * 2;
                var z = (random.nextFloat() - 0.5f) * segmentLength * 2;
                addSegment(pos.x, pos.y, pos.z, pos.x + x, pos.y + y, pos.z + z);
                if(random.nextFloat() < 0.2f) {
                    var x2 = x + (random.nextFloat() - 0.5f);
                    var y2 = y + (random.nextFloat() - 0.5f);
                    var z2 = z + (random.nextFloat() - 0.5f);
                    addSegment(pos.x + x, pos.y + y, pos.z + z, pos.x + x2, pos.y + y2, pos.z + z2);
                }
            }
            straightPos = straightEndPos;
            pos = endPos;
        }
    }

    @Override
    public void tick() {
        if(age++ >= maxAge) {
            markDead();
        } else {
            makeNewSegments();
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
        var bufferProvider = DefaultSuperRenderTypeBuffer.getInstance();
        var buffer = bufferProvider.getBuffer(ModdedRenderLayers.getColor());

        var camPos = camera.getPos();

        buffer.fixedColor((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));

        for(var segment : segments) {
            var pos1 = new Vector3f(segment.getLeft()).sub(camPos.toVector3f());
            var pos2 = new Vector3f(segment.getRight()).sub(camPos.toVector3f());
            renderSegment(buffer, pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, cross1, cross2);
        }

        buffer.unfixColor();

        bufferProvider.draw();
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }
}
