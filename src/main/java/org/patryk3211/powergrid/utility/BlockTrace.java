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

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;

import java.util.*;

public class BlockTrace {
    public static BlockHitResult raycast(Level world, Vec3 start, Vec3 end, @Nullable ITerminalPlacement passThrough) {
        return BlockGetter.traverseBlocks(start, end, null, (innerContext, pos) -> {
            var blockState = world.getBlockState(pos);
            var voxelShape = blockState.getShape(world, pos);
            var hit = world.clipWithInteractionOverride(start, end, pos, voxelShape, blockState);
            if(hit != null && passThrough != null && passThrough.check(pos, hit.getLocation()))
                return null;
            return hit;
        }, (innerContext) -> {
            var heading = start.subtract(end);
            return BlockHitResult.miss(end, Direction.getNearest(heading.x, heading.y, heading.z), BlockPos.containing(end));
        });
    }

    public static BlockHitResult raycast(Level world, Vec3 start, Vec3 end) {
        var ignore1 = BlockPos.containing(start);
        var ignore2 = BlockPos.containing(end);
        return BlockGetter.traverseBlocks(start, end, null, (innerContext, pos) -> {
            var blockState = world.getBlockState(pos);
            var voxelShape = blockState.getShape(world, pos);
            var hit = world.clipWithInteractionOverride(start, end, pos, voxelShape, blockState);
            if(hit != null && (hit.getBlockPos().equals(ignore1) || hit.getBlockPos().equals(ignore2)))//passThrough != null && passThrough.check(pos, hit.getPos()))
                return null;
            return hit;
        }, (innerContext) -> {
            var heading = start.subtract(end);
            return BlockHitResult.miss(end, Direction.getNearest(heading.x, heading.y, heading.z), BlockPos.containing(end));
        });
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(Math.min(val, max), min);
    }

    public static Vec3 closestPoint(AABB box, Vec3 point) {
        return new Vec3(
                clamp(point.x, box.minX, box.maxX),
                clamp(point.y, box.minY, box.maxY),
                clamp(point.z, box.minZ, box.maxZ)
        );
    }

    private static BlockWireEntity.Point makePoint(Vec3 start, Vec3 end) {
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

    public static Vec3 alignPosition(Vec3 position) {
        return new Vec3(
                (int) Math.round(position.x * TraceState.GRID_SIZE) / (float) TraceState.GRID_SIZE - 1/32f,
                (int) Math.round(position.y * TraceState.GRID_SIZE) / (float) TraceState.GRID_SIZE - 1/32f,
                (int) Math.round(position.z * TraceState.GRID_SIZE) / (float) TraceState.GRID_SIZE - 1/32f
        );
    }

    public static TraceResult findPath(Level world, Vec3 start, Vec3 end, @Nullable ITerminalPlacement terminal, @Nullable Direction continueDirection) {
        var result = findPathWithState(world, start, end, terminal, continueDirection);
        if(result == null)
            return null;
        return result.getSecond();
    }

    private static TraceCell testDirection(@NotNull Direction direction, TraceCell cell, TraceState state, PriorityQueue<TraceCell> visitQueue, int scoreAdjust) {
        var neighborPos = state.findNextPosition(cell, direction);
        if(neighborPos == null)
            return null;
        var neighbor = state.getCell(neighborPos);
        var distance = cell.position.distManhattan(neighborPos);

        int newDistance = cell.originDistance + Math.max(distance + (neighbor.isSupported() ? scoreAdjust : 0), 1);
        if(newDistance >= neighbor.originDistance)
            return null;
        if(!neighbor.isSupported()) {
            var newUnsupportedDistance = cell.unsupportedDistance + distance;
            if(neighbor.unsupportedDistance != 0 && newUnsupportedDistance >= neighbor.unsupportedDistance)
                return null;
            neighbor.unsupportedDistance = newUnsupportedDistance;
        }
        if(neighbor.unsupportedDistance > TraceState.GRID_SIZE)
            return null;

        neighbor.originDistance = newDistance;
        neighbor.backtrace = cell;
        visitQueue.add(neighbor);
        return neighbor;
    }

    public static Pair<TraceState, TraceResult> findPathWithState(Level world, Vec3 start, Vec3 end, @Nullable ITerminalPlacement terminal, @Nullable Direction continueDirection) {
        var state = new TraceState(world, start, end, terminal);
        if(state.target.equals(state.originCell.position) || !state.getCell(state.target).isSupported())
            return null;

        PriorityQueue<TraceCell> visitQueue = new PriorityQueue<>(Comparator.comparingInt(state::score));
        visitQueue.add(state.originCell);

        int bestScore = Integer.MAX_VALUE;
        TraceCell bestCell = null;

        while(!visitQueue.isEmpty()) {
            var cell = visitQueue.poll();
            if(cell.originDistance > 10 * 16)
                continue;
            if(cell.position.distManhattan(state.target) <= 1)
                return Pair.of(state, new TraceResult(state.traceResult(cell), true));
            if(state.states.size() > 150)
                break;

            Direction prevDirection = cell.cellDirection();
            if(prevDirection == null)
                prevDirection = continueDirection;

            if(prevDirection != null) {
                // Try to go the same direction first.
                var continuedNeighbor = testDirection(prevDirection, cell, state, visitQueue, -1);
                if (continuedNeighbor != null) {
//                if (continuedNeighbor.position.equals(state.target))
//                    return Pair.of(state, new TraceResult(state.traceResult(continuedNeighbor), true));
                    int neighborScore = state.targetDistance(continuedNeighbor);
                    if (neighborScore < bestScore) {
                        bestScore = neighborScore;
                        bestCell = continuedNeighbor;
                    }
                }
            }

            for(var direction : Direction.values()) {
                if(direction == prevDirection || (prevDirection != null && direction == prevDirection.getOpposite()))
                    continue;
                var neighbor = testDirection(direction, cell, state, visitQueue, 0);
                if(neighbor == null)
                    continue;
//                if(neighbor.position.equals(state.target))
//                    return Pair.of(state, new TraceResult(state.traceResult(neighbor), true));
                int neighborScore = state.targetDistance(neighbor);
                if(neighborScore < bestScore) {
                    bestScore = neighborScore;
                    bestCell = neighbor;
                }
            }
        }

        if(bestCell != null)
            return Pair.of(state, new TraceResult(state.traceResult(bestCell), false));

        return Pair.of(state, null);
    }

    public static TraceResult alternatePath(Vec3 start, Vec3 end) {
        var points = new ArrayList<BlockWireEntity.Point>();
        double x = Math.floor((end.x - start.x) * 16) / 16;
        double y = Math.floor((end.y - start.y) * 16) / 16;
        double z = Math.floor((end.z - start.z) * 16) / 16;
        if(x != 0) points.add(BlockWireEntity.Point.x((float) x));
        if(y != 0) points.add(BlockWireEntity.Point.y((float) y));
        if(z != 0) points.add(BlockWireEntity.Point.z((float) z));
        points.sort(Comparator.comparingInt(a -> a.gridLength));
        return new TraceResult(points, true);
    }

    public record TraceResult(List<BlockWireEntity.Point> points, boolean reachedTarget) { }

    public static class TraceCell {
        public final Vec3i position;
        public int originDistance;
        public TraceCell backtrace;
        public boolean isSupported;
        public int unsupportedDistance;
        public boolean isInside;
        public Vec3 nudge = Vec3.ZERO;

        public TraceCell(Vec3i position, int originDistance, boolean isSupported) {
            this.position = position;
            this.originDistance = originDistance;
            this.isSupported = isSupported;
            this.unsupportedDistance = 0;
        }

        public boolean isSupported() {
            return isSupported;
        }

        @Nullable
        public Direction cellDirection() {
            if(backtrace == null)
                return null;
            var p1 = backtrace.position;
            var p2 = position;
            return Direction.fromDelta(p2.getX() - p1.getX(), p2.getY() - p1.getY(), p2.getZ() - p1.getZ());
        }
    }

    public static class TraceState {
        // Specifies how much a single block is subdivided.
        public static final int GRID_SIZE = 16;
        public static final float UNIT_SIZE = 1.0f / GRID_SIZE;
        public static final float HALF_UNIT = UNIT_SIZE * 0.5f;

        public final Map<Vec3i, TraceCell> states = new HashMap<>();
        public final Level world;
        public final Vec3 origin;
        public final Vec3i target;
        public final TraceCell originCell;
        @Nullable
        public final ITerminalPlacement terminal;

        public TraceState(Level world, Vec3 origin, Vec3 target, @Nullable ITerminalPlacement terminal) {
            this.world = world;
            this.origin = new Vec3(Math.floor(origin.x), Math.floor(origin.y), Math.floor(origin.z));
            var originPos = transform(origin);
            originCell = createCell(originPos, 0);
            states.put(originPos, originCell);
            this.target = transform(target);
            this.terminal = terminal;
        }

        public Vec3i transform(Vec3 pos) {
            return new Vec3i(
                    (int) Math.floor((pos.x - origin.x) * GRID_SIZE),
                    (int) Math.floor((pos.y - origin.y) * GRID_SIZE),
                    (int) Math.floor((pos.z - origin.z) * GRID_SIZE)
            );
        }

        public Vec3 transform(Vec3i pos) {
            return new Vec3(
                    pos.getX() * UNIT_SIZE + origin.x + 1/32f,
                    pos.getY() * UNIT_SIZE + origin.y + 1/32f,
                    pos.getZ() * UNIT_SIZE + origin.z + 1/32f
            );
        }

        public Vec3 transform(TraceCell cell) {
            return transform(cell.position);
//            return base.add(cell.nudge);
        }

        public boolean isSupport(Vec3i position) {
            var worldPos = transform(position);
            var blockPos = BlockPos.containing(worldPos);
            var state = world.getBlockState(blockPos);
            // TODO: Might want to improve this.
            return state.isRedstoneConductor(world, blockPos);
        }

        @NotNull
        public TraceCell createCell(Vec3i position, int length) {
            boolean isSupported = false;
            for(var dir : Direction.values()) {
                if(position.equals(target) || isSupport(position.offset(dir.getNormal()))) {
                    isSupported = true;
                    break;
                }
            }
            var cell = new TraceCell(position, length, isSupported);
            states.put(position, cell);
            return cell;
        }

        public void writeCell(Vec3i position, Vec3 nudge) {
            if(!states.containsKey(position)) {
                // Nudge is written for fresh cells only
                var cell = createCell(position, Integer.MAX_VALUE);
                cell.nudge = nudge;
            }
        }

        @NotNull
        public TraceCell getCell(Vec3i cellPos) {
            if(!states.containsKey(cellPos)) {
                return createCell(cellPos, Integer.MAX_VALUE);
            }
            return states.get(cellPos);
        }

        public int score(TraceCell cell) {
            return cell.originDistance + targetDistance(cell);
        }

        public int targetDistance(TraceCell cell) {
            return cell.position.distManhattan(target);
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

        public boolean insideShape(Vec3i cellPos) {
            var pos = BlockPos.containing(transform(cellPos));
            var isInside = new MutableBoolean(false);
            var local = transform(cellPos).subtract(pos.getX(), pos.getY(), pos.getZ());
            world.getBlockState(pos)
                    .getCollisionShape(world, pos)
                    .toAabbs().forEach(aabb -> {
                        if (aabb.contains(local))
                            isInside.setTrue();
                    });
            return isInside.getValue();
        }

        @Nullable
        public Vec3i raycastNextPosition(TraceCell currentCell, Direction dir) {
            var axis = dir.getAxis();
            var currentPos = currentCell.position;
            var axialLength = Math.abs(target.get(axis) - currentPos.get(axis));

            BlockHitResult hit = null;
            for(var castOffsetDir : Direction.values()) {
                if(castOffsetDir.getAxis() == dir.getAxis())
                    continue;
                var castStart = transform(currentCell).relative(castOffsetDir, HALF_UNIT);
                Vec3 castEnd = castStart.relative(dir, axialLength * UNIT_SIZE);

                hit = raycast(world, castStart, castEnd, terminal);
                if(!hit.isInside())
                    break;
            }
            if(hit == null) {
                // Something has gone wrong. Hit should have been written.
                return null;
            }
            if (hit.isInside()) {
                var axisCoordinate = currentPos.get(axis);
                int offset = offsetToFullBlock(axisCoordinate, dir.getAxisDirection());

                // Try to move outside the block.
                var outside = currentPos.relative(axis, offset);
                if (Math.abs(offset) > GRID_SIZE / 2) {
                    if(insideShape(outside))
                        return null;
                } else {
                    if(insideShape(outside))
                        return outside.relative(dir, 1);
                }
                return outside;
            }
            var cellPos = transform(hit.getLocation());
            if(hit.getType() != HitResult.Type.MISS && hit.getDirection().getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                cellPos = cellPos.relative(hit.getDirection());
            }
            var retPos = switch(hit.getType()) {
                case MISS -> cellPos;
                case ENTITY -> null;
                case BLOCK -> cellPos;//.relative(hit.getDirection());
            };
            if(retPos != null) {
                var side = hit.getDirection();
                if(side.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    // This is to prevent later raycasts from being stuck
                    // on the boundary and landing inside the supporting block.
//                    retPos = retPos.relative(hit.getDirection());
//                    writeCell(cellPos, Vec3.ZERO.with(side.getAxis(), -UNIT_SIZE * 0.5f));
                }
            }
            return retPos;
        }

        @Nullable
        public Vec3i findNextPosition(TraceCell currentCell, Direction dir) {
            var axis = dir.getAxis();
            var currentPos = currentCell.position;
            Vec3i castPos = raycastNextPosition(currentCell, dir);
            if(castPos == null || currentPos.equals(castPos))
                return null;

            var axisCoordinate = currentPos.get(axis);

            var boundaryOffset = offsetToFullBlock(axisCoordinate, dir.getAxisDirection());
            if(boundaryOffset == 0) boundaryOffset = -GRID_SIZE;

            var nextBoundary = axisCoordinate + boundaryOffset;
            var raycastBoundary = castPos.get(axis);

            if(Math.abs(nextBoundary - axisCoordinate) > Math.abs(raycastBoundary - axisCoordinate)) {
                return currentPos.relative(axis, raycastBoundary - axisCoordinate);
            } else {
                return currentPos.relative(axis, nextBoundary - axisCoordinate);
            }
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
            List<Vec3> pathPoints = new ArrayList<>();
            traceback(cell).forEach(p -> pathPoints.add(transform(p)));

            List<BlockWireEntity.Point> result = new ArrayList<>();
            var current = transform(originCell.position);
            for(var point : pathPoints) {
                var segment = makePoint(current, point);
                if(segment.gridLength > 0)
                    result.add(segment);
                current = point;
            }
            return result;
        }
    }
}
