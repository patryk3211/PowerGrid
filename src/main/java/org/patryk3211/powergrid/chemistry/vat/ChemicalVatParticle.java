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
package org.patryk3211.powergrid.chemistry.vat;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;

public class ChemicalVatParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;

    public ChemicalVatParticle(ChemicalVatParticleData parameters, ClientWorld world, double x, double y, double z, double vX, double vY, double vZ, SpriteProvider sprites) {
        super(world, x, y, z, vX, vY, vZ);

        var random = world.getRandom();
        this.velocityX = vX + random.nextFloat() * 0.05f - 0.025f;
        this.velocityY = vY + random.nextFloat() * 0.05f - 0.025f;
        this.velocityZ = vZ + random.nextFloat() * 0.05f - 0.025f;
        this.sprites = sprites;

        var color = parameters.getColor();
        var darken = random.nextFloat() * 0.3f + 0.7f;
        setColor(color.x * darken, color.y * darken, color.z * darken);

        maxAge = 8;
        age = (int) (random.nextFloat() * 4.0f);
        scale = 0.1f;
        setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteForAge(sprites);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements ParticleFactory<ChemicalVatParticleData> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(ChemicalVatParticleData data, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ChemicalVatParticle(data, world, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
