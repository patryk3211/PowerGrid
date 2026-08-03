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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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
    private boolean rebuildClient = true;

    private boolean isChecking = false;

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
        if((!getWorld().isClientSide || blockEntity.isVirtual()) && checkSizeConstraint()) {
            makeController();
            checkConnectivity(null);
        }
    }

    private void sync() {
        if(!getWorld().isClientSide) {
            rebuildClient = true;
            blockEntity.notifyUpdate();
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
            lastKnownPos = pos;
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

        onChange();
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

        sync();
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

    @Override
    public void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if(compound.contains("LastKnownPos")) {
            var posArray = compound.getIntArray("LastKnownPos");
            lastKnownPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        }
        boolean rebuild = compound.getBoolean("SegmentedRebuild") || segments == null;
        if(clientPacket && !rebuild)
            return;
        if(compound.contains("Controller")) {
            var posArray = compound.getIntArray("Controller");
            controllerPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            segments = null;
        } else {
            controllerPos = null;
            if(segments == null)
                segments = new HashSet<>();
            segments.clear();
            if(clientPacket) {
                makeController();
                var segments = compound.getList("Segments", ListTag.TAG_INT_ARRAY);
                var level = getWorld();
                for(int i = 0; i < segments.size(); ++i) {
                    var ints = segments.getIntArray(i);
                    if(ints.length != 3)
                        continue;
                    var pos = new BlockPos(ints[0], ints[1], ints[2]);
                    var behavior = BlockEntityBehaviour.get(level, pos, getType());
                    if(behavior == null)
                        continue;
                    behavior.makePeripheral((T) this);
                }
            }
        }
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if(lastKnownPos != null) {
            compound.putIntArray("LastKnownPos", new int[]{lastKnownPos.getX(), lastKnownPos.getY(), lastKnownPos.getZ()});
        }
        if(clientPacket) {
            compound.putBoolean("SegmentedRebuild", rebuildClient);
            rebuildClient = false;
            if(segments != null && isController()) {
                var segments = new ListTag();
                for (var segment : this.segments) {
                    segments.add(NbtUtils.writeBlockPos(segment.getPos()));
                }
                compound.put("Segments", segments);
            }
        }
        if (!isController()) {
            compound.putIntArray("Controller", new int[] { controllerPos.getX(), controllerPos.getY(), controllerPos.getZ() });
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
        if(isChecking) {
            PowerGrid.LOGGER.error("checkConnectivity recursive call prevented");
            return;
        }
        isChecking = true;
        // Make sure this is always run on the controller
        if(!isController() && !getPos().equals(controllerPos)) {
            var controller = getController();
            if(controller.isPresent()) {
                controller.get().checkConnectivity(without);
            } else {
                PowerGrid.LOGGER.warn("Tried to check connectivity but controller is null");
            }
            isChecking = false;
            return;
        }
        if(segments == null) {
            isChecking = false;
            return;
        }
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
            if(segment == this)
                continue;
            segment.makePeripheral((T) this);
        }

        sync();
        isChecking = false;
    }

    public void remove() {
        if(getWorld().isClientSide)
            return;
        var controller = getControllerOrThis();
        controller.checkConnectivity((T) this);
    }

    protected void onChange() {
        sync();
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
        var controller = getControllerOrThis();
        int count = controller.isCounted() ? 1 : 0;
        if(controller.segments == null)
            return count;
        for(var segment : controller.segments) {
            if(segment.isCounted())
                ++count;
        }
        return count;
    }
}
