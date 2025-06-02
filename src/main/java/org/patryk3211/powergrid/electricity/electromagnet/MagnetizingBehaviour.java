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
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * A lot of code was adapted from Create's PressingBehaviour class.
 * @see com.simibubi.create.content.kinetics.press.PressingBehaviour
 */
public class MagnetizingBehaviour extends BeltProcessingBehaviour {
    public static final int CYCLE = 240;
    public static final int COLLAPSE_TIME = 220;
    public static final int ENTITY_SCAN = 10;

    public final MagnetizingBehaviourSpecifics specifics;
    public interface MagnetizingBehaviourSpecifics {
        boolean tryProcessOnBelt(TransportedItemStack input, List<ItemStack> outputList, boolean simulate);

        boolean tryProcessInWorld(ItemEntity itemEntity, boolean simulate);

        void onMagnetizationComplete();

        float getFieldStrength();
    }

    public Mode mode;
    public int prevRunningTicks;
    public int runningTicks;
    public boolean running;
    private int entityScanCooldown;
    public Vec3d target;

    public <T extends SmartBlockEntity & MagnetizingBehaviourSpecifics> MagnetizingBehaviour(T be) {
        super(be);
        this.specifics = be;
        whenItemEnters((s, i) -> BeltMagnetizingCallbacks.onItemReceived(s, i, this));
        whileItemHeld((s, i) -> BeltMagnetizingCallbacks.whenItemHeld(s, i, this));
        mode = Mode.WORLD;
    }

    public void start(Mode mode, Vec3d target) {
        this.mode = mode;
        running = true;
        prevRunningTicks = 0;
        runningTicks = 0;
        this.target = target;
        blockEntity.sendData();
    }

    public void updateTarget(Vec3d target) {
        this.target = target;
    }

    @Override
    public void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        running = compound.getBoolean("Running");
        mode = Mode.values()[compound.getInt("Mode")];
        prevRunningTicks = runningTicks = compound.getInt("Ticks");

        if(compound.contains("Target")) {
            var tag = compound.getCompound("Target");
            target = new Vec3d(tag.getFloat("X"), tag.getFloat("Y"), tag.getFloat("Z"));
        } else {
            target = null;
        }
    }

    @Override
    public void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putBoolean("Running", running);
        compound.putInt("Mode", mode.ordinal());
        compound.putInt("Ticks", runningTicks);

        if(target != null) {
            var tag = new NbtCompound();
            tag.putFloat("X", (float) target.x);
            tag.putFloat("Y", (float) target.y);
            tag.putFloat("Z", (float) target.z);
            compound.put("Target", tag);
        }
    }

    @Override
    public void tick() {
        super.tick();

        var world = getWorld();
        var pos = getPos();

        if(!running || world == null) {
            if(world != null && !world.isClient) {
                if(specifics.getFieldStrength() == 0)
                    return;
                if(entityScanCooldown > 0)
                    entityScanCooldown--;
                if(entityScanCooldown <= 0) {
                    entityScanCooldown = ENTITY_SCAN;

                    if(BlockEntityBehaviour.get(world, pos.down(2), TransportedItemStackHandlerBehaviour.TYPE) != null)
                        return;

                    for(var itemEntity : world.getNonSpectatingEntities(ItemEntity.class, new Box(pos.down()).contract(.125f))) {
                        if(!itemEntity.isAlive() || !itemEntity.isOnGround())
                            continue;
                        if(!specifics.tryProcessInWorld(itemEntity, true))
                            continue;
                        start(Mode.WORLD, itemEntity.getPos().add(0, 0.25f, 0));
                        return;
                    }
                }

            }
            return;
        }

        if(world.isClient && runningTicks == -COLLAPSE_TIME) {
            prevRunningTicks = COLLAPSE_TIME;
            return;
        }

        if(world.isClient) {
            var r = world.random;

            var particleChance = specifics.getFieldStrength() / 8f;
            if(runningTicks < COLLAPSE_TIME - 10 && r.nextFloat() < particleChance) {
                var facing = blockEntity.getCachedState().get(ElectromagnetBlock.FACING);
                var pos0 = pos.toCenterPos().offset(facing, 0.5f);
                var pos1 = target != null ? target.offset(facing, -0.1f) : pos0.offset(facing, 1.0f);
                var particlePos = VecHelper.lerp(r.nextFloat(), pos0, pos1).add(
                        (r.nextFloat() - 0.5f) * 0.2f,
                        (r.nextFloat() - 0.5f) * 0.2f,
                        (r.nextFloat() - 0.5f) * 0.2f
                );
                world.addParticle(new MagnetizationParticleData(pos), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            }
        }

        if(runningTicks == COLLAPSE_TIME && specifics.getFieldStrength() != 0) {
            if(mode == Mode.WORLD)
                applyInWorld();

//            if (level.getBlockState(worldPosition.down(2)).getSoundGroup() == BlockSoundGroup.WOOL)
//                AllSoundEvents.MECHANICAL_PRESS_ACTIVATION_ON_BELT.playOnServer(level, worldPosition);
//            else
//                AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, worldPosition, .5f,
//                        .75f + (Math.abs(specifics.getKineticSpeed()) / 1024f));

            if(!world.isClient)
                blockEntity.sendData();
        }

        if(!world.isClient && runningTicks > CYCLE) {
//            finished = true;
            running = false;
            specifics.onMagnetizationComplete();
            blockEntity.sendData();
            return;
        }

        prevRunningTicks = runningTicks;
        runningTicks += getRunningTickSpeed();
        if(prevRunningTicks < COLLAPSE_TIME && runningTicks >= COLLAPSE_TIME) {
            runningTicks = COLLAPSE_TIME;
            // Pause the ticks until a packet is received
            if(world.isClient && !blockEntity.isVirtual())
                runningTicks = -COLLAPSE_TIME;
        }
    }

    protected void applyInWorld() {
        var world = getWorld();
        var pos = getPos();
        var bb = new Box(pos.down(1));
        if(world.isClient)
            return;

        for(var entity : world.getOtherEntities(null, bb)) {
            if(!(entity instanceof ItemEntity itemEntity))
                continue;
            if(!entity.isAlive() || !entity.isOnGround())
                continue;

            entityScanCooldown = 0;
            if(specifics.tryProcessInWorld(itemEntity, false)) {
                target = itemEntity.getPos();
                blockEntity.sendData();
            }
            break;
        }
    }

    public int getRunningTickSpeed() {
        float speed = specifics.getFieldStrength();
        if(speed == 0)
            return 0;
        return MathHelper.lerp(MathHelper.clamp(Math.abs(speed) / 32f, 0, 1), 1, 60);
    }

    public enum Mode {
        WORLD(), BELT();

        Mode() {
        }
    }
}
