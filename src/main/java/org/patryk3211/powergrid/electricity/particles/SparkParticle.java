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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class SparkParticle extends SpriteBillboardParticle {
    protected SparkParticle(ClientWorld world, double x, double y, double z, double vX, double vY, double vZ, SpriteProvider sprites) {
        super(world, x, y, z);
        setSpriteForAge(sprites);
        velocityX = vX;
        velocityY = vY;
        velocityZ = vZ;

        var r = world.random;
        var color = r.nextFloat() * 0.3f + 0.3f;
        blue = color;
        red = 1;
        green = 1;

        gravityStrength = 3.0f;
        velocityMultiplier = 0.97f;
        maxAge = r.nextInt(20) + 40;
        scale = r.nextFloat() * 0.1f + 0.1f;
    }

    @Override
    public void tick() {
        super.tick();
//        if(onGround && maxAge - age > 15) {
//            age = maxAge - 15;
//        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    /**
     * TODO:
     * @see ElderGuardianAppearanceParticle
     */
    @Override
    public void buildGeometry(VertexConsumer buffer, Camera camera, float tickDelta) {
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
        super.buildGeometry(buffer, camera, tickDelta);
    }

    public static class Factory implements ParticleFactory<SparkParticleData> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(SparkParticleData data, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SparkParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
