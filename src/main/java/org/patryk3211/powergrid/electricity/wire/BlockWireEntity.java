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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedEntities;
import org.patryk3211.powergrid.electricity.base.IElectric;

import java.util.ArrayList;
import java.util.List;

public class BlockWireEntity extends WireEntity {
    public final List<Point> segments = new ArrayList<>();

    public BlockWireEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public static BlockWireEntity create(ServerWorld world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, ItemStack item, @Nullable List<Point> points) {
        var entity = new BlockWireEntity(ModdedEntities.BLOCK_WIRE.get(), world);
        entity.electricBlockPos1 = pos1;
        entity.electricBlockPos2 = pos2;
        entity.electricTerminal1 = terminal1;
        entity.electricTerminal2 = terminal2;
        entity.item = item;

        var terminal1Pos = IElectric.getTerminalPos(pos1, world.getBlockState(pos1), terminal1);
        entity.setPos(terminal1Pos.x, terminal1Pos.y, terminal1Pos.z);

        if(points != null) {
            entity.segments.addAll(points);
        }

        entity.setYaw(0);
        entity.setPitch(0);
        entity.resetPosition();
        entity.refreshPosition();
        return entity;
    }

    public static BlockWireEntity createFromPositions(ServerWorld world, BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, ItemStack item, List<Vec3d> points) {
        var entity = create(world, pos1, terminal1, pos2, terminal2, item, null);

        var lastPoint = entity.getPos();
        for(var point : points) {
            var vector = point.subtract(lastPoint);
            var lenX = Math.abs(vector.x);
            var lenY = Math.abs(vector.y);
            var lenZ = Math.abs(vector.z);

            Direction dir;
            float length;
            if(lenX > lenY && lenX > lenZ) {
                length = (float) lenX;
                if(vector.x > 0) {
                    dir = Direction.EAST;
                } else {
                    dir = Direction.WEST;
                }
            } else if(lenY > lenZ) {
                length = (float) lenY;
                if(vector.y > 0) {
                    dir = Direction.UP;
                } else {
                    dir = Direction.DOWN;
                }
            } else {
                length = (float) lenZ;
                if(vector.z > 0) {
                    dir = Direction.SOUTH;
                } else {
                    dir = Direction.NORTH;
                }
            }
            entity.segments.add(new Point(dir, length));

            lastPoint = point;
        }

        return entity;
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

    public static class Point {
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
