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
package org.patryk3211.powergrid.utility;

import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;

import java.util.*;

public class BlockTrace {
    public static BlockHitResult raycast(World world, Vec3d start, Vec3d end, @Nullable ITerminalPlacement passThrough) {
        return BlockView.raycast(start, end, null, (innerContext, pos) -> {
            var blockState = world.getBlockState(pos);
            var voxelShape = blockState.getOutlineShape(world, pos);
            var hit = world.raycastBlock(start, end, pos, voxelShape, blockState);
            if(hit != null && passThrough != null && passThrough.check(pos, hit.getPos()))
                return null;
//            if(hit != null && blockState.equals(passThrough) && hit.isInsideBlock())
//                return null;
            return hit;
        }, (innerContext) -> {
            var heading = start.subtract(end);
            return BlockHitResult.createMissed(end, Direction.getFacing(heading.x, heading.y, heading.z), BlockPos.ofFloored(end));
        });
    }

    private static BlockWireEntity.Point makePoint(Vec3d start, Vec3d end) {
        var distX = end.x - start.x;
        var distY = end.y - start.y;
        var distZ = end.z - start.z;

        var lenX = Math.abs(distX);
        var lenY = Math.abs(distY);
        var lenZ = Math.abs(distZ);

        if(lenX > lenY && lenX > lenZ) {
            return BlockWireEntity.Point.x((float) distX);
        } else if(lenY > lenZ) {
            return BlockWireEntity.Point.y((float) distY);
        } else {
            return BlockWireEntity.Point.z((float) distZ);
        }
    }

    public static List<BlockWireEntity.Point> findPath(World world, Vec3d start, Vec3d end, @Nullable ITerminalPlacement terminal) {
        var result = findPathWithState(world, start, end, terminal);
        if(result == null)
            return null;
        return result.getRight();
    }

    public static Pair<TraceState, List<BlockWireEntity.Point>> findPathWithState(World world, Vec3d start, Vec3d end, @Nullable ITerminalPlacement terminal) {
        var state = new TraceState(world, start, end, terminal);
        if(state.target.equals(state.originCell.position) || !state.getCell(state.target).isSupported)
            return null;

        PriorityQueue<TraceCell> visitQueue = new PriorityQueue<>(Comparator.comparingInt(state::targetDistance));
        visitQueue.add(state.originCell);

        while(!visitQueue.isEmpty()) {
            var cell = visitQueue.poll();
            if(cell.originDistance > 10 * 16)
                continue;

            for(var direction : Direction.values()) {
                var neighborPos = state.findNextPosition(cell.position, direction);
                if(neighborPos == null)
                    continue;
                var neighbor = state.getCell(neighborPos);
                var distance = cell.position.getManhattanDistance(neighborPos);
                // TODO: There needs to be a check for the total unsupported length of wire so that
                //  small hops are permitted and the behaviour is more uniform.
                if (!neighbor.isSupported && !cell.isSupported && distance > TraceState.GRID_SIZE / 4)
                    continue;
                int newDistance = cell.originDistance + distance;
                if (newDistance >= neighbor.originDistance)
                    continue;
                neighbor.originDistance = newDistance;
                neighbor.backtrace = cell;
                if (neighbor.position.equals(state.target))
                    return new Pair<>(state, state.traceResult(neighbor));
                visitQueue.add(neighbor);
            }
        }

        return new Pair<>(state, null);
    }

    public static class TraceCell {
        public final Vec3i position;
        public int originDistance;
        public TraceCell backtrace;
        public boolean isSupported;

        public TraceCell(Vec3i position, int originDistance, boolean isSupported) {
            this.position = position;
            this.originDistance = originDistance;
            this.isSupported = isSupported;
        }
    }

    public static class TraceState {
        // Specifies how much a single block is subdivided.
        public static final int GRID_SIZE = 16;
        public static final float UNIT_SIZE = 1.0f / GRID_SIZE;

        public final Map<Vec3i, TraceCell> states = new HashMap<>();
        public final World world;
        public final Vec3d origin;
        public final Vec3i target;
        public final TraceCell originCell;
        @Nullable
        public final ITerminalPlacement terminal;

        public TraceState(World world, Vec3d origin, Vec3d target, @Nullable ITerminalPlacement terminal) {
            this.world = world;
            this.origin = new Vec3d(Math.floor(origin.x), Math.floor(origin.y), Math.floor(origin.z));
            var originPos = transform(origin);
            originCell = new TraceCell(originPos, 0, true);
            states.put(originPos, originCell);
            this.target = transform(target);
            this.terminal = terminal;
        }

        public Vec3i transform(Vec3d pos) {
            return new Vec3i(
                    (int) Math.floor((pos.x - origin.x) * GRID_SIZE),
                    (int) Math.floor((pos.y - origin.y) * GRID_SIZE),
                    (int) Math.floor((pos.z - origin.z) * GRID_SIZE)
            );
        }

        public Vec3d transform(Vec3i pos) {
            return new Vec3d(
                    pos.getX() * UNIT_SIZE + origin.x,
                    pos.getY() * UNIT_SIZE + origin.y,
                    pos.getZ() * UNIT_SIZE + origin.z
            );
        }

        public boolean isSupport(Vec3i position) {
            var worldPos = transform(position);
            var blockPos = BlockPos.ofFloored(worldPos);
            var state = world.getBlockState(blockPos);
            // TODO: Might want to improve this.
            return state.isSolidBlock(world, blockPos);
        }

        @NotNull
        public TraceCell createCell(Vec3i position, int length) {
            boolean isSupported = false;
            for(var dir : Direction.values()) {
                if(position.equals(target) || isSupport(position.add(dir.getVector()))) {
                    isSupported = true;
                    break;
                }
            }
            var cell = new TraceCell(position, length, isSupported);
            states.put(position, cell);
            return cell;
        }

        public TraceCell isBetter(Vec3i cellPos, int pathLength) {
            if(!states.containsKey(cellPos)) {
                return createCell(cellPos, pathLength);
            }
            var cell = states.get(cellPos);
            if(pathLength < cell.originDistance) {
                cell.originDistance = pathLength;
                return cell;
            } else {
                return null;
            }
        }

        @NotNull
        public TraceCell getCell(Vec3i cellPos) {
            if(!states.containsKey(cellPos)) {
                return createCell(cellPos, Integer.MAX_VALUE);
            }
            return states.get(cellPos);
        }

        public int targetDistance(TraceCell cell) {
            return cell.position.getManhattanDistance(target);
        }

        public int offsetToFullBlock(int coordinate, Direction.AxisDirection direction) {
            int offset = 0;
            switch(direction) {
                case POSITIVE -> {
                    if(coordinate >= 0) {
                        offset = GRID_SIZE - (coordinate % GRID_SIZE);
                    } else {
                        offset = -(coordinate % GRID_SIZE);
                    }
                }
                case NEGATIVE -> {
                    if(coordinate >= 0) {
                        offset = -(coordinate % GRID_SIZE);
                    } else {
                        offset = -(GRID_SIZE + (coordinate % GRID_SIZE));
                    }
                }
            }
            return offset;
        }

        @Nullable
        public Vec3i raycastNextPosition(Vec3i currentPos, Direction dir) {
            var axis = dir.getAxis();
            var axialLength = Math.abs(target.getComponentAlongAxis(axis) - currentPos.getComponentAlongAxis(axis));

            var castStart = transform(currentPos);
            Vec3d castEnd = castStart.offset(dir, axialLength * UNIT_SIZE);

            var hit = raycast(world, castStart, castEnd, terminal);
            if(hit.isInsideBlock()) {
                var axisCoordinate = currentPos.getComponentAlongAxis(axis);
                int offset = offsetToFullBlock(axisCoordinate, dir.getDirection());
                if(Math.abs(offset) > GRID_SIZE / 2)
                    return null;
                // Try to move outside the block.
                return currentPos.offset(axis, offset);
            }
            var cellPos = transform(hit.getPos());
            return switch(hit.getType()) {
                case MISS -> cellPos;
                case ENTITY -> null;
                case BLOCK -> cellPos.offset(hit.getSide());
            };
        }

        @Nullable
        public Vec3i findNextPosition(Vec3i currentPos, Direction dir) {
            var axis = dir.getAxis();
            Vec3i castPos = raycastNextPosition(currentPos, dir);
            if(castPos == null || currentPos.equals(castPos))
                return null;

            var axisCoordinate = currentPos.getComponentAlongAxis(axis);

            var boundaryOffset = offsetToFullBlock(axisCoordinate, dir.getDirection());
            if(boundaryOffset == 0) boundaryOffset = -GRID_SIZE;

            var nextBoundary = axisCoordinate + boundaryOffset;
            var raycastBoundary = castPos.getComponentAlongAxis(axis);

            if(Math.abs(nextBoundary - axisCoordinate) > Math.abs(raycastBoundary - axisCoordinate)) {
                return currentPos.offset(axis, raycastBoundary - axisCoordinate);
            } else {
                return currentPos.offset(axis, nextBoundary - axisCoordinate);
            }
        }

        public void findNextPositions(Vec3i currentPos, Direction dir, Collection<Vec3i> positions) {
            var axis = dir.getAxis();
            Vec3i endPos = raycastNextPosition(currentPos, dir);
            if(endPos == null || currentPos.equals(endPos))
                return;

            var axisCoordinate = currentPos.getComponentAlongAxis(axis);

            var start = axisCoordinate + offsetToFullBlock(axisCoordinate, dir.getDirection());
            var end = endPos.getComponentAlongAxis(axis);
            if(dir.getDirection() == Direction.AxisDirection.POSITIVE) {
                for(int i = start; i < end; i += GRID_SIZE) {
                    positions.add(currentPos.offset(axis, i - axisCoordinate));
                }
            } else {
                for(int i = start; i > end; i -= GRID_SIZE) {
                    positions.add(currentPos.offset(axis, i - axisCoordinate));
                }
            }
            positions.add(endPos);
        }

        public List<Vec3i> traceback(TraceCell cell) {
            List<Vec3i> positions = new ArrayList<>();

            Direction.Axis currentAxis = null;
            Vec3i lastPosition = cell.position;
            while(cell.backtrace != null) {
                cell = cell.backtrace;

                Direction.Axis axis;
                if(cell.position.getX() != lastPosition.getX()) {
                    axis = Direction.Axis.X;
                } else if(cell.position.getY() != lastPosition.getY()) {
                    axis = Direction.Axis.Y;
                } else {
                    axis = Direction.Axis.Z;
                }

                if(axis != currentAxis) {
                    positions.add(0, lastPosition);
                    currentAxis = axis;
                }

                lastPosition = cell.position;
            }
            positions.add(0, lastPosition);
            return positions;
        }

        public List<BlockWireEntity.Point> traceResult(TraceCell cell) {
            List<Vec3d> pathPoints = new ArrayList<>();
            traceback(cell).forEach(p -> pathPoints.add(transform(p)));

            List<BlockWireEntity.Point> result = new ArrayList<>();
            var current = transform(originCell.position);
            for(var point : pathPoints) {
                result.add(makePoint(current, point));
                current = point;
            }
            return result;
        }
    }
}
