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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.PowerGrid;

import java.util.*;
import java.util.function.Consumer;

public abstract class SegmentedBehaviour<T extends SegmentedBehaviour<T>> extends BlockEntityBehaviour {
    protected T controller;
    protected BlockPos controllerPos;
    protected final Set<T> segments = new HashSet<>();
    protected Boolean isController;
    protected Runnable changeCallback;

    public SegmentedBehaviour(SmartBlockEntity be) {
        super(be);

        controller = null;
        controllerPos = null;
        isController = null;
        changeCallback = null;
    }

    public void setChangeCallback(Runnable callback) {
        changeCallback = callback;
    }

    @Override
    public void initialize() {
        super.initialize();
        if(!getWorld().isClient || blockEntity.isVirtual()) {
            grabController();
        } else {
            if(controllerPos != null && controller == null) {
                var behaviour = get(getWorld(), controllerPos, getType());
                if(behaviour != null) {
                    setController(behaviour);
                } else {
                    PowerGrid.LOGGER.error("Failed to fetch controller from given position (not good).");
                }
            }
        }
    }

    public void forEachSegment(Consumer<T> consumer) {
        var controller = getControllerOrThis();
        if(controller != null) {
            consumer.accept(controller);
            for(var segment : controller.segments) {
                consumer.accept(segment);
            }
        }
    }

    protected void makeController() {
        controller = null;
        controllerPos = null;
        isController = true;
    }

    public boolean isController() {
        return controllerPos == null;
    }

    protected abstract List<T> getConnected();
    @Override
    public abstract BehaviourType<T> getType();

    private void grabController() {
        var world = getWorld();
        assert world != null;
        if(isController == null) {
            // New entity
            var connected = getConnected();
            if(connected.size() > 1) {
                world.breakBlock(getPos(), true);
                return;
            }

            if(!connected.isEmpty()) {
                var neighbor = connected.get(0);
                if(neighbor.getController() != null) {
                    setController(neighbor.getController());
                } else {
                    setController(neighbor);
                }
            } else {
                makeController();
            }
        } else if(!isController) {
            // Segment entity
            var controller = get(world, controllerPos, getType());
            if(controller != null) {
                setController(controller);
            }
        } else {
            // Controller entity, do nothing
        }
    }

    protected void setController(T controller) {
        assert controller == null || controller.controller == null : "Controller of a controller cannot have a controller (for it is not a controller of itself)";
        if(controller == this) {
            // This is a very invalid state.
            getWorld().breakBlock(getPos(), true);
            return;
        }
        if(this.controller != controller && this.controller != null) {
            this.controller.segments.remove((T) this);
            this.controller.segmentRemoved((T) this);
        }
        this.controller = controller;
        if(controller != null) {
            // Add entity to segments of the controller
            this.controllerPos = controller.getPos();
            this.isController = false;
            controller.segments.add((T) this);
            controller.segmentAdded((T) this);
            // Move all controlled segments to the new controller
            segments.forEach(segment -> segment.setController(controller));
            segments.clear();
        } else {
            // This entity has become a controller
            this.controllerPos = null;
            if(isController == null || !isController)
                makeController();
        }
        blockEntity.sendData();
    }

    public T getController() {
        return controller;
    }

    public T getControllerOrThis() {
        return controllerPos == null ? (T) this : controller;
    }

    public abstract void readController(NbtCompound compound, boolean clientPacket);
    public abstract void writeController(NbtCompound compound, boolean clientPacket);

    @Override
    public void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if(compound.contains("Controller")) {
            var posArray = compound.getIntArray("Controller");
            var newControllerPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            if(!newControllerPos.equals(controllerPos)) {
                var world = getWorld();
                if(world != null) {
                    var behaviour = get(world, newControllerPos, getType());
                    if(behaviour != null) {
                        setController(behaviour);
                    }
                }
                controllerPos = newControllerPos;
                isController = false;
            }
        } else {
            if(isController == null || !isController)
                makeController();
            if (controller == null) {
                readController(compound, clientPacket);
            }
        }
    }

    @Override
    public void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if(controllerPos == null) {
            writeController(compound, clientPacket);
        } else {
            compound.putIntArray("Controller", new int[] { controllerPos.getX(), controllerPos.getY(), controllerPos.getZ() });
        }
    }

    protected void checkConnectivity(T without) {
        if(!isController()) {
            PowerGrid.LOGGER.error("Tried to check connectivity for a non-controller segment");
            return;
        }
        var checked = new HashSet<T>();
        var toCheck = new ArrayList<T>();
        toCheck.add((T) this);

        while(!toCheck.isEmpty()) {
            var segment = toCheck.remove(0);
            if(segment == without || !checked.add(segment))
                continue;
            var connected = segment.getConnected();
            toCheck.addAll(connected);
        }

        var removed = new ArrayList<T>();
        var iter = segments.iterator();
        while(iter.hasNext()) {
            var segment = iter.next();
            if(checked.contains(segment) || segment == without)
                continue;
            iter.remove();

            removed.add(segment);
            segmentRemoved(segment);
            segment.controller = null;
            segment.isController = null;
        }

        while(!removed.isEmpty()) {
            var zoneController = removed.remove(0);
            zoneController.makeController();

            var checkQueue2 = new ArrayList<T>();
            checkQueue2.add(zoneController);
            while(!checkQueue2.isEmpty()) {
                var segment = checkQueue2.remove(0);
                segment.blockEntity.notifyUpdate();
                var connected = segment.getConnected();
                for(var neighbor : connected) {
                    if(removed.contains(neighbor)) {
                        neighbor.controllerPos = zoneController.getPos();
                        neighbor.isController = false;
                        zoneController.segments.add(neighbor);
                        zoneController.segmentAdded(neighbor);
                        removed.remove(neighbor);
                        checkQueue2.add(neighbor);
                    }
                }
            }
        }
    }

    @Override
    public void unload() {
        super.unload();
        if(controllerPos != null) {
            if(controller != null) {
                controller.segments.remove((T) this);
                controller.segmentRemoved((T) this);
                controller = null;
            }
        } else {
            segments.forEach(segment -> segment.controller = null);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if(controllerPos == null && !segments.isEmpty()) {
            // Move all segments to a new controller.
            var firstIter = segments.iterator();
            var first = firstIter.next();
            firstIter.remove();

            first.makeController();
            segments.forEach(segment -> segment.setController(first));

            // Move controller data
            var nbt = new NbtCompound();
            this.writeController(nbt, false);
            first.readController(nbt, false);
            first.blockEntity.sendData();
        }
        getControllerOrThis().checkConnectivity((T) this);
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
}
