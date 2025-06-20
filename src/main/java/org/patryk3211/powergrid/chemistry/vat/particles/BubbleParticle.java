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
package org.patryk3211.powergrid.chemistry.vat.particles;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlockEntity;

public class BubbleParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final BlockPos vatPos;

    protected BubbleParticle(BubbleParticleData data, ClientWorld clientWorld, double x, double y, double z, SpriteProvider sprites) {
        super(clientWorld, x, y, z);
        this.sprites = sprites;
        vatPos = data.getVatPos();
        velocityY = 0.05f;
        maxAge = 6;
        age = 0;
        scale = 0.05f;
        collidesWithWorld = false;
        setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if(age >= maxAge) {
            markDead();
        } else {
            var be = world.getBlockEntity(vatPos);
            if(!(be instanceof ChemicalVatBlockEntity vat)) {
                markDead();
                return;
            }

            move(velocityX, velocityY, velocityZ);

            if(age == 0) {
                var fluidLevel = vat.getPos().getY() + vat.getFluidLevel() * ChemicalVatBlock.FLUID_SPAN + ChemicalVatBlock.CORNER + 1 / 64f;
                if (y >= fluidLevel) {
                    y = fluidLevel;
                    velocityY = 0;
//                    velocityX = (world.random.nextFloat() - 0.5f) * 0.1f;
//                    velocityZ = (world.random.nextFloat() - 0.5f) * 0.1f;
                    age = 1;
                    setSpriteForAge(sprites);
                }
            } else {
                age++;
                setSpriteForAge(sprites);
            }
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements ParticleFactory<BubbleParticleData> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(BubbleParticleData data, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BubbleParticle(data, world, x, y, z, this.sprites);
        }
    }
}
