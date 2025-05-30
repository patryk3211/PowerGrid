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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.IComplexRaycast;

import java.util.ArrayList;
import java.util.List;

public class BlockWireEntity extends WireEntity implements IComplexRaycast {
    public Box mainBoundingBox;
    public final List<Box> boundingBoxes = new ArrayList<>();
    public final List<Point> segments = new ArrayList<>();

    private boolean particlesSpawned = false;

    public BlockWireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public static BlockWireEntity create(ServerWorld world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, ItemStack item, float resistance, @Nullable List<Point> points) {
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        entity.electricBlockPos1 = pos1;
        entity.electricBlockPos2 = pos2;
        entity.electricTerminal1 = terminal1;
        entity.electricTerminal2 = terminal2;
        entity.item = item;
        entity.resistance = resistance;

        var terminal1Pos = IElectric.getTerminalPos(pos1, world.getBlockState(pos1), terminal1);
        terminal1Pos = BlockTrace.alignPosition(terminal1Pos);
        entity.setPos(terminal1Pos.x, terminal1Pos.y, terminal1Pos.z);

        if(points != null) {
            entity.segments.addAll(points);
            entity.bakeBoundingBoxes();
        }

        entity.setYaw(0);
        entity.setPitch(0);
        entity.resetPosition();
        entity.refreshPosition();

        entity.makeWire();
        return entity;
    }

    @Override
    protected Box calculateBoundingBox() {
        if(mainBoundingBox != null) {
            return mainBoundingBox.offset(getPos());
        } else {
            return super.calculateBoundingBox();
        }
    }

    private void bakeBoundingBoxes() {
        boundingBoxes.clear();

        // Starting from zero will make the bounding boxes independent of entity position,
        // but they will need to be offset before using them.
        var currentPos = Vec3d.ZERO;
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;
        for(var segment : segments) {
            if(segment.start == null)
                segment.start = currentPos.add(getPos());
            var nextPos = currentPos.add(segment.vector());
            if(nextPos.x > maxX)
                maxX = nextPos.x;
            if(nextPos.y > maxY)
                maxY = nextPos.y;
            if(nextPos.z > maxZ)
                maxZ = nextPos.z;
            if(nextPos.x < minX)
                minX = nextPos.x;
            if(nextPos.y < minY)
                minY = nextPos.y;
            if(nextPos.z < minZ)
                minZ = nextPos.z;

            boundingBoxes.add(new Box(currentPos, nextPos).expand(0.0625f));
            currentPos = nextPos;
        }

        mainBoundingBox = new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(0.0625f);
    }

    @Override
    public void tick() {
        super.tick();
        var world = getWorld();
        var temperature = getTemperature();

        var pos = getPos();
        if(isOverheated()) {
            if(world.isClient && !particlesSpawned) {
                for(var segment : segments) {
                    var dir = segment.vector();
                    int pointCount = Math.round(segment.length / 0.25f);
                    for(int i = 0; i < pointCount; ++i) {
                        float r = (float) i / pointCount;
                        double x = segment.start.x + dir.x * r;
                        double y = segment.start.y + dir.y * r;
                        double z = segment.start.z + dir.z * r;
                        world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0f, 0.0f, 0.0f);
                    }
                }
                particlesSpawned = true;
            }
        } else if(temperature >= overheatTemperature - 50 && world.isClient) {
            var segment = segments.get(random.nextInt(segments.size()));
            var dir = segment.vector();
            float r = random.nextFloat();
            double x = segment.start.x + dir.x * r;
            double y = segment.start.y + dir.y * r;
            double z = segment.start.z + dir.z * r;
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
        }
    }

    @Override
    public @Nullable ItemEntity dropStack(ItemStack stack, float yOffset) {
        if (stack.isEmpty()) {
            return null;
        } else if (this.getWorld().isClient) {
            return null;
        } else {
            var center = mainBoundingBox.getCenter();
            ItemEntity itemEntity = new ItemEntity(this.getWorld(),
                    this.getX() + center.x,
                    this.getY() + (double)yOffset + center.y,
                    this.getZ() + center.z,
                    stack);
            itemEntity.setToDefaultPickupDelay();
            this.getWorld().spawnEntity(itemEntity);
            return itemEntity;
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        segments.clear();
        var segmentList = nbt.getList("Segments", NbtElement.COMPOUND_TYPE);
        for(var segment : segmentList) {
            var point = new Point((NbtCompound) segment);
            segments.add(point);
        }

        bakeBoundingBoxes();
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        var segmentList = new NbtList();
        for(var segment : segments) {
            segmentList.add(segment.serialize());
        }
        nbt.put("Segments", segmentList);
    }

    @Override
    public @Nullable Vec3d raycast(Vec3d min, Vec3d max) {
        Vec3d closestHit = null;
        min = min.subtract(getPos());
        max = max.subtract(getPos());
        double distance = max.squaredDistanceTo(min);
        for(var bb : boundingBoxes) {
            var hit = bb.raycast(min, max);
            if(hit.isEmpty())
                continue;
            var hitDistance = hit.get().squaredDistanceTo(min);
            if(hitDistance < distance) {
                distance = hitDistance;
                closestHit = hit.get().add(getPos());
            }
        }
        return closestHit;
    }

    public static class Point {
        public Vec3d start;
        public final Direction direction;
        public final float length;

        public Point(Direction direction, float length) {
            this.direction = direction;
            this.length = length;
        }

        public Point(NbtCompound tag) {
            this.direction = Direction.byId(tag.getInt("Direction"));
            this.length = tag.getFloat("Length");
        }

        public NbtCompound serialize() {
            var tag = new NbtCompound();
            tag.putInt("Direction", direction.getId());
            tag.putFloat("Length", length);
            return tag;
        }

        public Vec3d vector() {
            return switch(direction) {
                case EAST -> new Vec3d(length, 0, 0);
                case WEST -> new Vec3d(-length, 0, 0);
                case UP -> new Vec3d(0, length, 0);
                case DOWN -> new Vec3d(0, -length, 0);
                case SOUTH -> new Vec3d(0, 0, length);
                case NORTH -> new Vec3d(0, 0, -length);
            };
        }

        public static Point x(float length) {
            return new Point(length >= 0 ? Direction.EAST : Direction.WEST, Math.abs(length));
        }

        public static Point y(float length) {
            return new Point(length >= 0 ? Direction.UP : Direction.DOWN, Math.abs(length));
        }

        public static Point z(float length) {
            return new Point(length >= 0 ? Direction.SOUTH : Direction.NORTH, Math.abs(length));
        }
    }
}
