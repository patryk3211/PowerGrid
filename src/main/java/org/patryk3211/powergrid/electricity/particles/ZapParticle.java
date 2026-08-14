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

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

import java.util.ArrayList;
import java.util.List;

public class ZapParticle extends Particle {
    private final Vec3 delta;
    private final Vec3 cross1;
    private final Vec3 cross2;
    private final boolean anchorEnd;
    private final int segmentCount;
    private final float factorMultiplier;

    private final List<Tuple<Vec3, Vec3>> segments = new ArrayList<>();

    @Nullable
    private final ClientSubLevelAccess sublevel;

    public ZapParticle(ZapParticleData data, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        sublevel = SableCompanion.INSTANCE.getContainingClient(new Vec3(x, y, z));
        Vec3 end = data.getEnd();
        anchorEnd = data.isAnchored();
        lifetime = data.getLife();
        segmentCount = data.getSegmentCount();
        bCol = 0.5f;
        factorMultiplier = data.getFactor();

        delta = new Vec3(end.x - this.x, end.y - this.y, end.z - this.z);
        var vec = new Vec3(1 - delta.x, 1 - delta.y, 1 - delta.z);
        cross1 = vec.cross(delta).normalize().scale(0.02);
        cross2 = cross1.cross(delta).normalize().scale(0.02);

        double radius = delta.length();
        setBoundingBox(AABB.ofSize(new Vec3(x, y, z), radius, radius, radius));

        makeNewSegments();
    }

    public void addSegment(Vec3 pos1, Vec3 pos2) {
        segments.add(new Tuple<>(pos1, pos2));
    }

    public void addSegment(double x1, double y1, double z1, double x2, double y2, double z2) {
        segments.add(new Tuple<>(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2)));
    }

    public void makeNewSegments() {
        segments.clear();
        var pos = new Vec3(this.x, this.y, this.z);
        var straightPos = pos;
        var segmentCount = this.segmentCount == -1 ? Math.max((int) (delta.length() / 0.5f), 3) : this.segmentCount;
        var segmentVector = delta.scale(1.0 / segmentCount);
        var segmentLength = delta.length() / segmentCount;
        double totalLength = delta.length();
        for(int i = 0; i < segmentCount; ++i) {
            var straightEndPos = segmentVector.add(straightPos);
            var endPos = segmentVector.add(pos);
            endPos = endPos
                    .add(cross1.scale((random.nextFloat() - 0.5f) * totalLength * 50))
                    .add(cross2.scale((random.nextFloat() - 0.5f) * totalLength * 50));
            var middle = segmentCount / 2;
            float factor;
            if(anchorEnd) {
                factor = 1.0f - ((float) Math.abs(middle - i - 1) / (middle + 1));
            } else {
                factor = (float) i / middle;
            }
            factor *= factorMultiplier;
            float invFactor = 1.0f - factor;
            endPos = endPos.scale(factor).add(straightEndPos.x * invFactor, straightEndPos.y * invFactor, straightEndPos.z * invFactor);

            addSegment(pos, endPos);
            if(i != 0 && random.nextFloat() < 0.2f) {
                var x = (random.nextFloat() - 0.5) * segmentLength * 2;
                var y = (random.nextFloat() - 0.5) * segmentLength * 2;
                var z = (random.nextFloat() - 0.5) * segmentLength * 2;
                addSegment(pos.x, pos.y, pos.z, pos.x + x, pos.y + y, pos.z + z);
                if(random.nextFloat() < 0.2f) {
                    var x2 = x + (random.nextFloat() - 0.5);
                    var y2 = y + (random.nextFloat() - 0.5);
                    var z2 = z + (random.nextFloat() - 0.5);
                    addSegment(pos.x + x, pos.y + y, pos.z + z, pos.x + x2, pos.y + y2, pos.z + z2);
                }
            }
            straightPos = straightEndPos;
            pos = endPos;
        }
    }

    @Override
    public void tick() {
        if(age++ >= lifetime) {
            remove();
        } else {
            makeNewSegments();
        }
    }

    public void renderSegment(VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2, Vec3 cross1, Vec3 cross2, int color) {
        buffer.addVertex((float) (x1 + cross1.x), (float) (y1 + cross1.y), (float) (z1 + cross1.z)).setColor(color);
        buffer.addVertex((float) (x1 - cross1.x), (float) (y1 - cross1.y), (float) (z1 - cross1.z)).setColor(color);
        buffer.addVertex((float) (x2 - cross1.x), (float) (y2 - cross1.y), (float) (z2 - cross1.z)).setColor(color);
        buffer.addVertex((float) (x2 + cross1.x), (float) (y2 + cross1.y), (float) (z2 + cross1.z)).setColor(color);

        buffer.addVertex((float) (x1 + cross2.x), (float) (y1 + cross2.y), (float) (z1 + cross2.z)).setColor(color);
        buffer.addVertex((float) (x1 - cross2.x), (float) (y1 - cross2.y), (float) (z1 - cross2.z)).setColor(color);
        buffer.addVertex((float) (x2 - cross2.x), (float) (y2 - cross2.y), (float) (z2 - cross2.z)).setColor(color);
        buffer.addVertex((float) (x2 + cross2.x), (float) (y2 + cross2.y), (float) (z2 + cross2.z)).setColor(color);
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        var bufferProvider = DefaultSuperRenderTypeBuffer.getInstance();
        var buffer = bufferProvider.getBuffer(ModdedRenderLayers.getColor());

        var camPos = camera.getPosition();

        int argbColor = ((int) (alpha * 255) << 24) | ((int) (rCol * 255) << 16) | ((int) (gCol * 255) << 8) | (int) (bCol * 255);

        Pose3dc pose = null;
        if(sublevel != null)
            pose = sublevel.renderPose(tickDelta);
        for(var segment : segments) {
            var a = segment.getA();
            var b = segment.getB();
            if(pose != null) {
                a = pose.transformPosition(a);
                b = pose.transformPosition(b);
            }
            renderSegment(buffer,
                    a.x - camPos.x, a.y - camPos.y, a.z - camPos.z,
                    b.x - camPos.x, b.y - camPos.y, b.z - camPos.z,
                    cross1, cross2, argbColor);
        }

        bufferProvider.draw();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
