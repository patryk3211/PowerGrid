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
package org.patryk3211.powergrid.electricity.battery;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Copied from Create
 * @see com.simibubi.create.api.connectivity.ConnectivityHandler
 */
public class CustomConnectivityHandler {
    public static <T extends BlockEntity & IMultiBlockBattery> void formMulti(T be) {
        SearchCache<T> cache = new SearchCache<>();
        List<T> frontier = new ArrayList<>();
        frontier.add(be);
        formMulti(be.getType(), be.getSpec(), be.getLevel(), cache, frontier);
    }

    private static <T extends BlockEntity & IMultiBlockBattery> void formMulti(BlockEntityType<?> type, BatterySpec spec,
                                                                                       BlockGetter level, SearchCache<T> cache, List<T> frontier) {
        PriorityQueue<Pair<Integer, T>> creationQueue = makeCreationQueue();
        Set<BlockPos> visited = new HashSet<>();
        Direction.Axis mainAxis = frontier.get(0)
                .getMainConnectionAxis();

        // essentially, if it's a vertical multi then the search won't be restricted by
        // Y
        // alternately, a horizontal multi search shouldn't be restricted by X or Z
        int minX = (mainAxis == Direction.Axis.Y ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        int minY = (mainAxis != Direction.Axis.Y ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        int minZ = (mainAxis == Direction.Axis.Y ? Integer.MAX_VALUE : Integer.MIN_VALUE);

        for (T be : frontier) {
            BlockPos pos = be.getBlockPos();
            minX = Math.min(pos.getX(), minX);
            minY = Math.min(pos.getY(), minY);
            minZ = Math.min(pos.getZ(), minZ);
        }
        if (mainAxis == Direction.Axis.Y)
            minX -= frontier.get(0)
                    .getMaxWidth();
        if (mainAxis != Direction.Axis.Y)
            minY -= frontier.get(0)
                    .getMaxWidth();
        if (mainAxis == Direction.Axis.Y)
            minZ -= frontier.get(0)
                    .getMaxWidth();

        while (!frontier.isEmpty()) {
            T part = frontier.remove(0);
            BlockPos partPos = part.getBlockPos();
            if (visited.contains(partPos))
                continue;

            visited.add(partPos);

            int amount = tryToFormNewMulti(part, cache, true);
            if (amount > 1) {
                creationQueue.add(Pair.of(amount, part));
            }

            for (Direction.Axis axis : Iterate.axes) {
                Direction dir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
                BlockPos next = partPos.relative(dir);

                if (next.getX() <= minX || next.getY() <= minY || next.getZ() <= minZ)
                    continue;
                if (visited.contains(next))
                    continue;
                T nextBe = partAt(type, spec, level, next);
                if (nextBe == null)
                    continue;
                if (nextBe.isRemoved())
                    continue;
                frontier.add(nextBe);
            }
        }
        visited.clear();

        while (!creationQueue.isEmpty()) {
            Pair<Integer, T> next = creationQueue.poll();
            T toCreate = next.getValue();
            if (visited.contains(toCreate.getBlockPos()))
                continue;

            visited.add(toCreate.getBlockPos());
            tryToFormNewMulti(toCreate, cache, false);
        }
    }

    private static <T extends BlockEntity & IMultiBlockBattery> int tryToFormNewMulti(T be, SearchCache<T> cache,
                                                                                              boolean simulate) {
        int bestWidth = 1;
        int bestAmount = -1;
        if (!be.isController())
            return 0;

        int radius = be.getMaxWidth();
        for (int w = 1; w <= radius; w++) {
            int amount = tryToFormNewMultiOfWidth(be, w, cache, true);
            if (amount < bestAmount)
                continue;
            bestWidth = w;
            bestAmount = amount;
        }

        if (!simulate) {
            int beWidth = be.getWidth();
            if (beWidth == bestWidth && beWidth * beWidth * be.getHeight() == bestAmount)
                return bestAmount;

            splitMultiAndInvalidate(be, cache, false);
//            be.setCapacitySize(bestAmount);

            tryToFormNewMultiOfWidth(be, bestWidth, cache, false);

            be.preventConnectivityUpdate();
            be.setWidth(bestWidth);
            be.setHeight(bestAmount / bestWidth / bestWidth);
            be.notifyMultiUpdated();
        }
        return bestAmount;
    }

    private static <T extends BlockEntity & IMultiBlockBattery> int tryToFormNewMultiOfWidth(T be, int width,
                                                                                             SearchCache<T> cache, boolean simulate) {
        int amount = 0;
        int height = 0;
        BlockEntityType<?> type = be.getType();
        Level level = be.getLevel();
        if (level == null)
            return 0;
        BlockPos origin = be.getBlockPos();

        Direction.Axis axis = be.getMainConnectionAxis();

        Search:
        for (int yOffset = 0; yOffset < be.getMaxLength(axis, width); yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };
                    Optional<T> part = cache.getOrCache(type, be.getSpec(), level, pos);
                    if (part.isEmpty())
                        break Search;

                    T controller = part.get();
                    int otherWidth = controller.getWidth();
                    if (otherWidth > width)
                        break Search;
                    if (otherWidth == width && controller.getHeight() == be.getMaxLength(axis, width))
                        break Search;

                    Direction.Axis conAxis = controller.getMainConnectionAxis();
                    if (axis != conAxis)
                        break Search;

                    BlockPos conPos = controller.getBlockPos();
                    if (!conPos.equals(origin)) {
                        if (axis == Direction.Axis.Y) { // vertical multi, like a FluidTank
                            if (conPos.getX() < origin.getX())
                                break Search;
                            if (conPos.getZ() < origin.getZ())
                                break Search;
                            if (conPos.getX() + otherWidth > origin.getX() + width)
                                break Search;
                            if (conPos.getZ() + otherWidth > origin.getZ() + width)
                                break Search;
                        } else { // horizontal multi, like an ItemVault
                            if (axis == Direction.Axis.Z && conPos.getX() < origin.getX())
                                break Search;
                            if (conPos.getY() < origin.getY())
                                break Search;
                            if (axis == Direction.Axis.X && conPos.getZ() < origin.getZ())
                                break Search;
                            if (axis == Direction.Axis.Z && conPos.getX() + otherWidth > origin.getX() + width)
                                break Search;
                            if (conPos.getY() + otherWidth > origin.getY() + width)
                                break Search;
                            if (axis == Direction.Axis.X && conPos.getZ() + otherWidth > origin.getZ() + width)
                                break Search;
                        }
                    }
//                    if (controller instanceof IMultiBlockEntityContainer.Fluid ifluidCon && ifluidCon.hasTank()) {
//                        FluidStack otherFluid = ifluidCon.getFluid(0);
//                        if (!fluid.isEmpty() && !otherFluid.isEmpty() && !fluid.isFluidEqual(otherFluid))
//                            break Search;
//                    }
                }
            }
            amount += width * width;
            height++;
        }

        if (simulate)
            return amount;

        Object extraData = be.getExtraData();

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };
                    T part = partAt(type, be.getSpec(), level, pos);
                    if (part == null)
                        continue;
                    if (part == be)
                        continue;

                    extraData = be.modifyExtraData(extraData);

                    be.inheritEnergy(part);

                    splitMultiAndInvalidate(part, cache, false);
                    part.setController(origin);
                    part.preventConnectivityUpdate();
                    cache.put(pos, be);
                    part.setHeight(height);
                    part.setWidth(width);
                    part.notifyMultiUpdated();
                }
            }
        }
        be.setExtraData(extraData);
        be.notifyMultiUpdated();
        return amount;
    }

    public static <T extends BlockEntity & IMultiBlockBattery> void splitMulti(T be) {
        splitMultiAndInvalidate(be, null, false);
    }

    // tryReconnect helps whenever only a few tanks have been removed
    private static <T extends BlockEntity & IMultiBlockBattery> void splitMultiAndInvalidate(T be,
                                                                                                @Nullable SearchCache<T> cache, boolean tryReconnect) {
        Level level = be.getLevel();
        if (level == null)
            return;

        be = be.getControllerBE();
        if (be == null)
            return;

        int height = be.getHeight();
        int width = be.getWidth();
        if (width == 1 && height == 1)
            return;

        BlockPos origin = be.getBlockPos();
        List<T> frontier = new ArrayList<>();
        Direction.Axis axis = be.getMainConnectionAxis();

        double energyPerPart = be.getEnergy() / be.getSize();
        be.setCapacitySize(1);
        be.setEnergy(energyPerPart);

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {

                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };

                    T partAt = partAt(be.getType(), be.getSpec(), level, pos);
                    if (partAt == null)
                        continue;
                    if (!partAt.getController()
                            .equals(origin))
                        continue;

                    T controllerBE = partAt.getControllerBE();
                    partAt.setExtraData((controllerBE == null ? null : controllerBE.getExtraData()));
                    partAt.removeController(true);

                    partAt.setCapacitySize(1);
                    partAt.setEnergy(energyPerPart);

                    if (tryReconnect) {
                        frontier.add(partAt);
                        partAt.preventConnectivityUpdate();
                    }
                    if (cache != null)
                        cache.put(pos, partAt);
                }
            }
        }
        // Rewires everything into the individual battery blocks.
        be.updateBehaviour();
        be.markRewire();
        if (tryReconnect)
            formMulti(be.getType(), be.getSpec(), level, cache == null ? new SearchCache<>() : cache, frontier);
    }

    private static <T extends BlockEntity & IMultiBlockBattery> PriorityQueue<Pair<Integer, T>> makeCreationQueue() {
        return new PriorityQueue<>((one, two) -> two.getKey() - one.getKey());
    }

    @Nullable
    public static <T extends BlockEntity & IMultiBlockBattery> T partAt(BlockEntityType<?> type, BatterySpec spec, BlockGetter level,
                                                                                BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && be.getType() == type && !be.isRemoved())
            return checked(be, spec);
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity & IMultiBlockBattery> T checked(BlockEntity be, BatterySpec spec) {
        if (be instanceof IMultiBlockBattery battery && battery.getSpec() == spec)
            return (T) be;
        return null;
    }

    private static class SearchCache<T extends BlockEntity & IMultiBlockBattery> {
        Map<BlockPos, Optional<T>> controllerMap;

        public SearchCache() {
            controllerMap = new HashMap<>();
        }

        void put(BlockPos pos, T target) {
            controllerMap.put(pos, Optional.of(target));
        }

        void putEmpty(BlockPos pos) {
            controllerMap.put(pos, Optional.empty());
        }

        boolean hasVisited(BlockPos pos) {
            return controllerMap.containsKey(pos);
        }

        Optional<T> getOrCache(BlockEntityType<?> type, BatterySpec spec, BlockGetter level, BlockPos pos) {
            if (hasVisited(pos))
                return controllerMap.get(pos);

            T partAt = partAt(type, spec, level, pos);
            if (partAt == null) {
                putEmpty(pos);
                return Optional.empty();
            }
            T controller = checked(level.getBlockEntity(partAt.getController()), spec);
            if (controller == null) {
                putEmpty(pos);
                return Optional.empty();
            }
            put(pos, controller);
            return Optional.of(controller);
        }
    }
}
