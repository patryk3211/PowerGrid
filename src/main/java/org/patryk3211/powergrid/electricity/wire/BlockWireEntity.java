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

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.utility.BlockTrace;
import org.patryk3211.powergrid.utility.IComplexRaycast;

import java.util.ArrayList;
import java.util.List;

public class BlockWireEntity extends WireEntity implements IComplexRaycast {
    public AABB mainBoundingBox;
    public final List<AABB> boundingBoxes = new ArrayList<>();
    public final List<Point> segments = new ArrayList<>();

    private float totalLength = 0;

    private boolean particlesSpawned = false;

    public BlockWireEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public static BlockWireEntity create(Level world, IWireEndpoint endpoint1, ItemStack item, List<Point> segments) {
        if(!IWire.isWire(world, item.getItem()))
            throw new IllegalArgumentException("ItemStack must be of a WireItem");
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        entity.setItem(item.getItem(), item.getCount());

        var pos = BlockTrace.alignPosition(endpoint1.getExactPosition(world));
        entity.setPosRaw(pos.x, pos.y, pos.z);
        entity.segments.addAll(segments);
        entity.bakeBoundingBoxes();

        entity.setEndpoint1(endpoint1);

        entity.setYRot(0);
        entity.setXRot(0);
        entity.setOldPosAndRot();
        entity.reapplyPosition();

        if(entity.getWireEntry().colorable())
            entity.setColor(0x413c31);

        return entity;
    }

    public static BlockWireEntity create(Level world, Vec3 pos, ItemStack item, List<Point> segments) {
        if(!IWire.isWire(world, item.getItem()))
            throw new IllegalArgumentException("ItemStack must be of a WireItem");
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        entity.setItem(item.getItem(), item.getCount());

        entity.setPosRaw(pos.x, pos.y, pos.z);
        entity.segments.addAll(segments);
        entity.bakeBoundingBoxes();

        entity.setYRot(0);
        entity.setXRot(0);
        entity.setOldPosAndRot();
        entity.reapplyPosition();

        if(entity.getWireEntry().colorable())
            entity.setColor(0x413c31);

        return entity;
    }

    private void optimizeSegments() {
        if(segments.size() <= 1)
            return;
        int i = 1;
        while(i < segments.size()) {
            var prev = segments.get(i - 1);
            var segment = segments.get(i);
            if(prev.direction == segment.direction) {
                segments.set(i - 1, new Point(prev.direction, prev.gridLength + segment.gridLength));
                segments.remove(i);
            } else {
                ++i;
            }
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        if(mainBoundingBox != null) {
            return mainBoundingBox.move(position());
        } else {
            return super.makeBoundingBox();
        }
    }

    public void bakeBoundingBoxes() {
        optimizeSegments();
        boundingBoxes.clear();
        totalLength = 0;

        // Starting from zero will make the bounding boxes independent of entity position,
        // but they will need to be offset before using them.
        var currentPos = Vec3.ZERO;
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;
        for(var segment : segments) {
            segment.start = currentPos.add(position());
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

            boundingBoxes.add(new AABB(currentPos, nextPos).inflate(0.0625f));
            currentPos = nextPos;
            totalLength += segment.length();
        }

        mainBoundingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.0625f);
        setBoundingBox(makeBoundingBox());
    }

    public float getTotalLength() {
        return totalLength;
    }

    @Override
    protected boolean isConnectedTo(LivingEntity entity) {
        if(!super.isConnectedTo(entity))
            return false;
        var pos = position();
        var bb = entity.getBoundingBox().move(-pos.x, -pos.y, -pos.z);
        for(var segmentBB : boundingBoxes) {
            if(segmentBB.intersects(bb))
                return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        var world = level();
        var temperature = getTemperature();

        var pos = position();
        if(isOverheated()) {
            if(world.isClientSide && !particlesSpawned) {
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
        } else if(temperature >= overheatTemperature - 50 && world.isClientSide) {
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
    public @Nullable ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        if (stack.isEmpty()) {
            return null;
        } else if (this.level().isClientSide) {
            return null;
        } else {
            var center = mainBoundingBox.getCenter();
            ItemEntity itemEntity = new ItemEntity(this.level(),
                    this.getX() + center.x,
                    this.getY() + (double)yOffset + center.y,
                    this.getZ() + center.z,
                    stack);
            itemEntity.setDefaultPickUpDelay();
            this.level().addFreshEntity(itemEntity);
            return itemEntity;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        segments.clear();
        var segmentList = nbt.getList("Segments", Tag.TAG_COMPOUND);
        for(var segment : segmentList) {
            var point = new Point((CompoundTag) segment);
            segments.add(point);
        }

        bakeBoundingBoxes();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        var segmentList = new ListTag();
        for(var segment : segments) {
            segmentList.add(segment.serialize());
        }
        nbt.put("Segments", segmentList);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        var stack = player.getItemInHand(hand);
        if(IWire.isWire(player.level(), stack.getItem())) {
            // Connect wire to wire.
            if(player.level().isClientSide) {
                return ClientWireInteractions.attachWire(this);
            } else {
                // Server side.
                return InteractionResult.CONSUME;
            }
        } else if(stack.getItem() == ModdedItems.WIRE_CUTTER.get()) {
            if(!player.isShiftKeyDown()) {
                if(player.level().isClientSide) {
                    // Cut a segment of the wire.
                    return ClientWireInteractions.segmentCut(this);
                } else {
                    // Server side.
                    return InteractionResult.CONSUME;
                }
            }
        }
        return super.interact(player, hand);
    }

    @Override
    public @Nullable Vec3 raycast(Vec3 min, Vec3 max) {
        Vec3 closestHit = null;
        min = min.subtract(position());
        max = max.subtract(position());
        double distance = max.distanceToSqr(min);
        for(var bb : boundingBoxes) {
            var hit = bb.clip(min, max);
            if(hit.isEmpty())
                continue;
            var hitDistance = hit.get().distanceToSqr(min);
            if(hitDistance < distance) {
                distance = hitDistance;
                closestHit = hit.get().add(position());
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
            setPos(position().add(removedSegment.vector()));
            setEndpoint1(null);
        } else {
            return;
        }
        int items = (int) removedSegment.length();
        if(items > 0 && !level().isClientSide) {
            var start = removedSegment.start;
            var vector = removedSegment.vector();
            if(getWireCount() <= items)
                items = items - 1;
            ItemEntity itemEntity = new ItemEntity(this.level(),
                    start.x + vector.x,
                    start.y + vector.y,
                    start.z + vector.z,
                    new ItemStack(getItem(), items));
            itemEntity.setDefaultPickUpDelay();
            this.level().addFreshEntity(itemEntity);
            incrementWireCount(-items);
        }
        dropWire();
        sendExtraData();
    }

    public BlockWireEntity flip() {
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), level());
        entity.setItem(getItem(), getWireCount());
        entity.setEndpoint1(getEndpoint2());
        entity.setEndpoint2(getEndpoint1());
        entity.getEntityData().set(TEMPERATURE, getTemperature());
        entity.setColor(getColor());

        var pos = position();
        for(int i = 0; i < segments.size(); ++i) {
            var segment = segments.get(i);
            pos = pos.add(segment.vector());
            var dir = segment.direction.getOpposite();
            var length = segment.gridLength;
            if(length > 0)
                entity.segments.add(0, new Point(dir, length));
        }

        entity.setPosRaw(pos.x, pos.y, pos.z);
        entity.bakeBoundingBoxes();

        entity.setYRot(0);
        entity.setXRot(0);
        entity.setOldPosAndRot();
        entity.reapplyPosition();

        this.discard();
        var serverWorld = (ServerLevel) level();
        serverWorld.tryAddFreshEntityWithPassengers(entity);
        return entity;
    }

    public void extend(List<Point> points, int newItems, boolean notify) {
        if(level().isClientSide)
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
        var world = level();
        if(world.isClientSide)
            return null;

        var segment = segments.get(segmentIndex);

        var junctionPos = segment.start.relative(segment.direction, segmentPoint / 16f);
        var junction = new JunctionWireEndpoint(junctionPos);

        var wire2 = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        wire2.setColor(getColor());
        wire2.setItem(getItem(), 0);
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

        wire2.setPosRaw(junctionPos.x, junctionPos.y, junctionPos.z);
        wire2.bakeBoundingBoxes();
        this.bakeBoundingBoxes();

        wire2.setYRot(0);
        wire2.setXRot(0);
        wire2.setOldPosAndRot();
        wire2.reapplyPosition();

        int items = (int) movedLength;
        wire2.incrementWireCount(items);

        wire2.getEntityData().set(TEMPERATURE, getTemperature());
        wire2.setEndpoint2(getEndpoint2());
        wire2.setEndpoint1(junction);
        this.setEndpoint2(junction);
        var serverWorld = (ServerLevel) world;
        serverWorld.tryAddFreshEntityWithPassengers(wire2);

        sendExtraData();
        return junction;
    }

    public static class Point {
        public Vec3 start;
        public final Direction direction;
        public final int gridLength;

        public Point(Direction direction, int gridLength) {
            this.direction = direction;
            this.gridLength = gridLength;
        }

        public Point(CompoundTag tag) {
            this.direction = Direction.from3DDataValue(tag.getInt("Direction"));
            this.gridLength = tag.getInt("Length");
        }

        public CompoundTag serialize() {
            var tag = new CompoundTag();
            tag.putInt("Direction", direction.get3DDataValue());
            tag.putInt("Length", gridLength);
            return tag;
        }

        public float length() {
            return gridLength / 16f;
        }

        public Vec3 vector() {
            var length = length();
            return switch(direction) {
                case EAST -> new Vec3(length, 0, 0);
                case WEST -> new Vec3(-length, 0, 0);
                case UP -> new Vec3(0, length, 0);
                case DOWN -> new Vec3(0, -length, 0);
                case SOUTH -> new Vec3(0, 0, length);
                case NORTH -> new Vec3(0, 0, -length);
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
