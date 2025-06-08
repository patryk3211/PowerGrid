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

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.IComplexRaycast;

import java.util.ArrayList;
import java.util.List;

public class BlockWireEntity extends WireEntity implements IComplexRaycast {
    public Box mainBoundingBox;
    public final List<Box> boundingBoxes = new ArrayList<>();
    public final List<Point> segments = new ArrayList<>();

    private float totalLength = 0;

    private boolean particlesSpawned = false;

    public BlockWireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public static BlockWireEntity create(World world, IWireEndpoint endpoint1, ItemStack item, List<Point> segments) {
        if(!(item.getItem() instanceof WireItem))
            throw new IllegalArgumentException("ItemStack must be of a WireItem");
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        entity.setItem((WireItem) item.getItem(), item.getCount());

        var pos = BlockTrace.alignPosition(endpoint1.getExactPosition(world));
        entity.setPos(pos.x, pos.y, pos.z);
        entity.segments.addAll(segments);
        entity.bakeBoundingBoxes();

        entity.setEndpoint1(endpoint1);

        entity.setYaw(0);
        entity.setPitch(0);
        entity.resetPosition();
        entity.refreshPosition();
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
        totalLength = 0;

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
            totalLength += segment.length();
        }

        mainBoundingBox = new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(0.0625f);
        setBoundingBox(calculateBoundingBox());
    }

    public float getTotalLength() {
        return totalLength;
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
                    int pointCount = Math.round(segment.length() / 0.25f);
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
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if(hand != Hand.MAIN_HAND)
            return ActionResult.PASS;
        var stack = player.getStackInHand(hand);
        if(stack.getItem() instanceof WireItem wire) {
            // Connect wire to wire.
            if(player instanceof ClientPlayerEntity) {
                return ClientWireInteractions.attachWire(this);
            } else {
                // Server side.
                return ActionResult.CONSUME;
            }
        } else if(stack.getItem() == ModdedItems.WIRE_CUTTER.get()) {
            if(player.isSneaking()) {
                // Cut the whole wire.
                return super.interact(player, hand);
            } else if(player instanceof ClientPlayerEntity) {
                // Cut a segment of the wire.
                return ClientWireInteractions.segmentCut(this);
            } else {
                // Server side.
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
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

    @Override
    public void endpointRemoved(IWireEndpoint endpoint) {
        // TODO: Remove unsupported segments up to the first supported one.
        Point removedSegment;
        if(endpoint.equals(getEndpoint2())) {
            removedSegment = segments.remove(segments.size() - 1);
            setEndpoint2(null);
        } else if(endpoint.equals(getEndpoint1())) {
            removedSegment = segments.remove(0);
            setPosition(getPos().add(removedSegment.vector()));
            setEndpoint1(null);
        } else {
            return;
        }
        int items = (int) removedSegment.length();
        if(items > 0 && !getWorld().isClient) {
            var start = removedSegment.start;
            var vector = removedSegment.vector();
            if(getWireCount() <= items)
                items = items - 1;
            ItemEntity itemEntity = new ItemEntity(this.getWorld(),
                    start.x + vector.x,
                    start.y + vector.y,
                    start.z + vector.z,
                    new ItemStack(getWireItem(), items));
            itemEntity.setToDefaultPickupDelay();
            this.getWorld().spawnEntity(itemEntity);
            incrementWireCount(-items);
        }
        dropWire();
        sendExtraData();
    }

    public BlockWireEntity flip() {
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), getWorld());
        entity.setItem(getWireItem(), getWireCount());
        entity.setEndpoint1(getEndpoint2());
        entity.setEndpoint2(getEndpoint1());
        entity.getDataTracker().set(TEMPERATURE, getTemperature());

        var pos = getPos();
        for(int i = 0; i < segments.size(); ++i) {
            var segment = segments.get(i);
            pos = pos.add(segment.vector());
            var dir = segment.direction.getOpposite();
            var length = segment.gridLength;
            if(length > 0)
                entity.segments.add(0, new Point(dir, length));
        }

        entity.setPos(pos.x, pos.y, pos.z);
        entity.bakeBoundingBoxes();

        entity.setYaw(0);
        entity.setPitch(0);
        entity.resetPosition();
        entity.refreshPosition();

        this.discard();
        var serverWorld = (ServerWorld) getWorld();
        serverWorld.spawnNewEntityAndPassengers(entity);
        return entity;
    }

    public void extend(List<Point> points, int newItems, boolean notify) {
        if(getWorld().isClient)
            return;
        incrementWireCount(newItems);
        this.segments.addAll(points);
        bakeBoundingBoxes();

        if(notify)
            sendExtraData();
    }

    public void extend(List<Point> points, int newItems) {
        extend(points, newItems, true);
    }

    public JunctionWireEndpoint split(int segmentIndex, int segmentPoint) {
        var world = getWorld();
        if(world.isClient)
            return null;

        var segment = segments.get(segmentIndex);

        var junctionPos = segment.start.offset(segment.direction, segmentPoint / 16f);
        var junction = new JunctionWireEndpoint(junctionPos);

        var wire2 = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        wire2.setItem(getWireItem(), 0);
        var splitSegment = new Point(segment.direction, segment.gridLength - segmentPoint);
        wire2.segments.add(splitSegment);
        this.segments.set(segmentIndex, new Point(segment.direction, segmentPoint - 1));
        float movedLength = splitSegment.length();

        int removeCount = segments.size() - segmentIndex - 1;
        for(int i = 0; i < removeCount; ++i) {
            var removed = segments.remove(segmentIndex + 1);
            wire2.segments.add(removed);
            movedLength += removed.length();
        }

        wire2.setPos(junctionPos.x, junctionPos.y, junctionPos.z);
        wire2.bakeBoundingBoxes();
        this.bakeBoundingBoxes();

        wire2.setYaw(0);
        wire2.setPitch(0);
        wire2.resetPosition();
        wire2.refreshPosition();

        int items = (int) movedLength;
        wire2.incrementWireCount(items);

        wire2.getDataTracker().set(TEMPERATURE, getTemperature());
        wire2.setEndpoint2(getEndpoint2());
        wire2.setEndpoint1(junction);
        this.setEndpoint2(junction);
        var serverWorld = (ServerWorld) world;
        serverWorld.spawnNewEntityAndPassengers(wire2);

        sendExtraData();
        return junction;
    }

    public static class Point {
        public Vec3d start;
        public final Direction direction;
        public final int gridLength;

        public Point(Direction direction, int gridLength) {
            this.direction = direction;
            this.gridLength = gridLength;
        }

        public Point(NbtCompound tag) {
            this.direction = Direction.byId(tag.getInt("Direction"));
            this.gridLength = tag.getInt("Length");
        }

        public NbtCompound serialize() {
            var tag = new NbtCompound();
            tag.putInt("Direction", direction.getId());
            tag.putInt("Length", gridLength);
            return tag;
        }

        public float length() {
            return gridLength / 16f;
        }

        public Vec3d vector() {
            var length = length();
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
            return new Point(length >= 0 ? Direction.EAST : Direction.WEST, (int) (Math.abs(length) * 16));
        }

        public static Point y(float length) {
            return new Point(length >= 0 ? Direction.UP : Direction.DOWN, (int) (Math.abs(length) * 16));
        }

        public static Point z(float length) {
            return new Point(length >= 0 ? Direction.SOUTH : Direction.NORTH, (int) (Math.abs(length) * 16));
        }
    }
}
