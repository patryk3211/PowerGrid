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
package org.patryk3211.powergrid.electricity.electromagnet;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class MagnetizationParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final BlockPos controller;
    private boolean triggered;

    protected MagnetizationParticle(MagnetizationParticleData data, ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z);
        this.sprites = sprites;
        this.controller = data.getControllerPos();
        this.collidesWithWorld = false;

        scale = MathHelper.lerp(world.random.nextFloat(), 0.125f, 0.25f);// 0.25f;
        maxAge = 20;
        triggered = false;
        setSprite(sprites.getSprite(world.random));
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        MagnetizingBehaviour behaviour = null;
        var behaviourGeneric = controller == null ? null : BlockEntityBehaviour.get(world, controller, BeltProcessingBehaviour.TYPE);
        if(behaviourGeneric instanceof MagnetizingBehaviour magnetizingBehaviour)
            behaviour = magnetizingBehaviour;

        if(behaviour == null) {
            if(age++ >= maxAge) {
                markDead();
                return;
            }
            jiggle();
        } else {
            if(!behaviour.running) {
                markDead();
                return;
            }

            if(triggered && age++ >= maxAge) {
                markDead();
                return;
            }

            if(!triggered)
                jiggle();

            if(behaviour.runningTicks >= MagnetizingBehaviour.COLLAPSE_TIME - 5 && !triggered) {
                age = 0;
                maxAge = 5;
                final float SPEED_CONST = 0.3f;
                var dX = behaviour.target.x - x;
                var dY = behaviour.target.y - y;
                var dZ = behaviour.target.z - z;
                velocityX = dX * SPEED_CONST;
                velocityY = dY * SPEED_CONST;
                velocityZ = dZ * SPEED_CONST;
                collidesWithWorld = true;
                triggered = true;
            }
        }

        move(this.velocityX, this.velocityY, this.velocityZ);
        scale = 0.25f * (1.0f - (float) age / maxAge);
    }

    private void jiggle() {
        velocityX = (random.nextFloat() - 0.5f) * 0.05f;
        velocityY = (random.nextFloat() - 0.5f) * 0.05f;
        velocityZ = (random.nextFloat() - 0.5f) * 0.05f;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements ParticleFactory<MagnetizationParticleData> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(MagnetizationParticleData data, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new MagnetizationParticle(data, world, x, y, z, this.sprites);
        }
    }
}
