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
package org.patryk3211.powergrid.base;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class SegmentedBehaviour<T extends SegmentedBehaviour<T>> extends BlockEntityBehaviour {
    @Nullable
    protected BlockPos controllerPos;
    protected BlockPos lastKnownPos;
    protected Set<T> segments;
    @Nullable
    protected Runnable changeCallback;
    protected final int maxSize;

    protected Predicate<T> countToSize;

    public SegmentedBehaviour(SmartBlockEntity be, int maxSize, Predicate<T> countToSize) {
        super(be);

        this.maxSize = maxSize;
        this.countToSize = countToSize;
        controllerPos = null;
        segments = null;
        changeCallback = null;
    }

    public void setChangeCallback(@Nullable Runnable callback) {
        changeCallback = callback;
    }

    @Override
    public void initialize() {
        if(checkSizeConstraint()) {
            makeController();
            super.initialize();
            checkConnectivity(null);
        }
    }

    @Override
    public void tick() {
        super.tick();
        var pos = getPos();
        if (lastKnownPos == null)
            lastKnownPos = pos;
        else if (!lastKnownPos.equals(pos) && pos != null) {
            onPositionChanged();
        }
    }

    protected void onPositionChanged() {
        // Reinitialize
        if(checkSizeConstraint()) {
            makeController();
            super.initialize();
            checkConnectivity(null);
        }
    }

    public void forEachSegment(Consumer<T> consumer) {
        var controller = getControllerOrThis();
        consumer.accept(controller);
        if(controller.segments != null) {
            for (var segment : controller.segments) {
                consumer.accept(segment);
            }
        }
    }

    protected void makeController() {
        controllerPos = null;
        segments = new HashSet<>();

        blockEntity.notifyUpdate();
    }

    protected void makePeripheral(T controller) {
        assert controller.isController();
        this.controllerPos = controller.getPos();
        this.segments = null;
        if(isCounted()) {
            if (controller.getLimitedSize() + 1 > maxSize) {
                // This assembly is too big.
                var world = getWorld();
                if (!world.isClientSide)
                    world.destroyBlock(getPos(), true);
                return;
            }
        }

        controller.segments.add((T) this);
        controller.segmentAdded((T) this);

        blockEntity.notifyUpdate();
    }

    public boolean isController() {
        return controllerPos == null;
    }

    protected abstract List<T> getConnected();
    @Override
    public abstract BehaviourType<T> getType();

    public Optional<T> getController() {
        if(controllerPos == null)
            return Optional.empty();
        var world = getWorld();
        if(world == null)
            throw new IllegalCallerException("Tried to get controller before receiving world");
        if(world.hasChunk(SectionPos.blockToSectionCoord(controllerPos.getX()), SectionPos.blockToSectionCoord(controllerPos.getZ())))
            return Optional.ofNullable(get(world, controllerPos, getType()));
        return Optional.empty();
    }

    @NotNull
    public T getControllerOrThis() {
        return getController().orElse((T) this);
    }

    public abstract void readController(CompoundTag compound, boolean clientPacket);
    public abstract void writeController(CompoundTag compound, boolean clientPacket);

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if(compound.contains("Controller")) {
            var posArray = compound.getIntArray("Controller");
            controllerPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            segments = null;
        } else {
            controllerPos = null;
            if(segments == null)
                segments = new HashSet<>();
            if(clientPacket)
                checkConnectivity(null);
            readController(compound, clientPacket);
        }
        if(compound.contains("LastKnownPos")) {
            var posArray = compound.getIntArray("LastKnownPos");
            lastKnownPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if(isController()) {
            writeController(compound, clientPacket);
        } else {
            compound.putIntArray("Controller", new int[] { controllerPos.getX(), controllerPos.getY(), controllerPos.getZ() });
        }
        if(lastKnownPos != null) {
            compound.putIntArray("LastKnownPos", new int[]{lastKnownPos.getX(), lastKnownPos.getY(), lastKnownPos.getZ()});
        }
    }

    private boolean checkSizeConstraint() {
        if(controllerPos != null || segments != null) {
            // Already in an assembly so the size check doesn't matter
            return true;
        }
        int totalSize = isCounted() ? 1 : 0;
        for(var connected : getConnected()) {
            totalSize += connected.getLimitedSize();
        }
        if(totalSize > maxSize) {
            var world = getWorld();
            if(!world.isClientSide)
                world.destroyBlock(getPos(), true);
            return false;
        }
        return true;
    }

    public void checkConnectivity(@Nullable T without) {
        // Make sure this is always run on the controller
        if(!isController()) {
            var controller = getController();
            if(controller.isPresent()) {
                controller.get().checkConnectivity(without);
            } else {
                PowerGrid.LOGGER.warn("Tried to check connectivity but controller is null");
            }
            return;
        }
        if(segments == null)
            return;
        var allConnected = new HashSet<T>();
        var toCheck = new ArrayList<T>();
        toCheck.add((T) this);

        while(!toCheck.isEmpty()) {
            var segment = toCheck.remove(0);
            if(segment == without || !allConnected.add(segment))
                continue;
            var connected = segment.getConnected();
            toCheck.addAll(connected);
        }

        var kept = new HashSet<T>();
        var removed = new ArrayList<T>();
        if(without == this) {
            // All removed, none kept
            removed.addAll(segments);
            segments.forEach(SegmentedBehaviour::makeController);
        } else {
            var iter = segments.iterator();
            while(iter.hasNext()) {
                var segment = iter.next();
                if(segment == without)
                    continue;
                if(allConnected.contains(segment)) {
                    kept.add(segment);
                    continue;
                }
                iter.remove();

                removed.add(segment);
                segmentRemoved(segment);
                // Make all removed segments standalone controllers
                segment.makeController();
            }
        }

        while(!removed.isEmpty()) {
            // Pick an arbitrary segment to become a controller
            var zoneController = removed.remove(0);

            var checkQueue2 = new ArrayList<T>();
            checkQueue2.add(zoneController);
            while(!checkQueue2.isEmpty()) {
                var segment = checkQueue2.remove(0);
                var connected = segment.getConnected();
                for(var neighbor : connected) {
                    if(removed.contains(neighbor)) {
                        neighbor.makePeripheral(zoneController);

                        removed.remove(neighbor);
                        checkQueue2.add(neighbor);
                    }
                }
            }
        }
        // Add newly connected segments
        segments.clear();
        for(var segment : allConnected) {
            if(kept.contains(segment) || segment == this)
                continue;
            segment.makePeripheral((T) this);
        }

        blockEntity.notifyUpdate();
    }

    public void remove() {
        var controller = getControllerOrThis();
        controller.checkConnectivity((T) this);
    }

    protected void onChange() {
        if(changeCallback != null)
            changeCallback.run();
    }

    public void segmentAdded(T behaviour) {
        onChange();
        for(var segment : segments)
            segment.onChange();
    }

    public void segmentRemoved(T behaviour) {
        onChange();
        for(var segment : segments)
            segment.onChange();
    }

    public int getSegmentCount() {
        var controller = getControllerOrThis();
        if(controller.segments == null)
            return 1;
        return controller.segments.size() + 1;
    }

    protected boolean isCounted() {
        return countToSize.test((T) this);
    }

    public int getLimitedSize() {
        int count = isCounted() ? 1 : 0;
        var controller = getControllerOrThis();
        if(controller.segments == null)
            return count;
        for(var segment : controller.segments) {
            if(segment.isCounted())
                ++count;
        }
        return count;
    }
}
