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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;

@Environment(EnvType.CLIENT)
public class SparkParticle extends TextureSheetParticle {
    protected SparkParticle(SparkParticleData data, ClientLevel world, double x, double y, double z, double vX, double vY, double vZ, SpriteSet sprites) {
        super(world, x, y, z);
        setSpriteFromAge(sprites);
        xd = vX;
        yd = vY;
        zd = vZ;

        var r = world.random;
        var color = r.nextFloat() * 0.3f + 0.3f;
        bCol = color;
        rCol = 1;
        gCol = 1;

        gravity = data.getGravity() ? 3.0f : 0;
        friction = 0.97f;
        lifetime = data.getLife() < 0 ? r.nextInt(20) + 40 : data.getLife();
        quadSize = r.nextFloat() * 0.1f + 0.1f;
        hasPhysics = data.getCollision();
    }

    @Override
    protected int getLightColor(float tint) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        super.tick();
//        if(onGround && maxAge - age > 15) {
//            age = maxAge - 15;
//        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    /**
     * TODO:
     * @see MobAppearanceParticle
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float tickDelta) {
//        var camPos = camera.getPos();
//        var x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - camPos.getX());
//        var y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - camPos.getY());
//        var z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - camPos.getZ());
//
//        var vertices = new Vector3f[] {
//                new Vector3f(-1, -1, -1),
//                new Vector3f(-1,  1, -1),
//                new Vector3f( 1,  1, -1),
//                new Vector3f( 1, -1, -1),
//                new Vector3f(-1, -1,  1),
//                new Vector3f(-1,  1,  1),
//                new Vector3f( 1,  1,  1),
//                new Vector3f( 1, -1,  1)
//        };
//        var indices = new int[] {
//                0, 1, 2, 3,
//                4, 5, 6, 7,
//                0, 1, 4, 5,
//                2, 3, 6, 7,
//                0, 3, 4, 7,
//                1, 2, 5, 6
//        };
//
//        var size = 1;//getSize(tickDelta);
//        for(int i = 0; i < vertices.length; ++i) {
//            var vec = vertices[i];
//            vec.rotate(camera.getRotation());
//            vec.mul(size);
//            vec.add(x, y, z);
//        }
//
//        int light = getBrightness(tickDelta);
//        for(int i = 0; i < indices.length; ++i) {
//            var pos = vertices[indices[i]];
//            buffer.vertex(pos.x, pos.y, pos.z)
//                    .texture(0.5f, 0.5f)
//                    .color(red, green, blue, alpha)
//                    .light(light)
//                    .next();
//        }
        super.render(buffer, camera, tickDelta);
    }

    public static class Factory implements ParticleProvider<SparkParticleData> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(SparkParticleData data, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SparkParticle(data, world, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
