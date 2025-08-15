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
package org.patryk3211.powergrid.kinetics.generator.winding;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ProxyElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing.HORIZONTAL_FACING;
import static org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing.UP;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.*;

public class WindingBlockEntity extends ElectricBlockEntity {
    /**
     * This is the main block entity of multiple windings connected by housings.
     */
    private BlockPos ownerPosition;
    /**
     * These are all the main block entities which are connected in parallel.
     */
    private Set<BlockPos> parallelPositions;
    /**
     * This is the main block entity of a single winding.
     */
    private WindingBlockEntity mainBE;
    /**
     * These are all the block entities of a single winding.
     */
    private Set<WindingBlockEntity> collectedBEs;
    private RotorBehaviour rotorP;
    private RotorBehaviour rotorN;

    private float coilConstant = 1;
    private float resistance = 0.1f;
    private int totalCoilCount = 0;
    private VoltageSourceNode sourceNode;
    private TransformerCoupling coupling;

    private boolean neighborChanged = false;

    public WindingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean isMain() {
        return getBlockState().getValue(PART) == 0;
    }

    public float emfVoltage() {
        float voltage = 0;
        if(rotorP != null) {
            voltage -= rotorP.getAngularVelocityRadians() * rotorP.getFieldStrength() * coilConstant;
        }
        if(rotorN != null) {
            voltage -= rotorN.getAngularVelocityRadians() * rotorN.getFieldStrength() * coilConstant;
        }
        return voltage;
    }

    public float windingVoltage() {
        if(mainBE == null || mainBE.collectedBEs == null)
            return 0;
        float total = 0;
        for(var be : mainBE.collectedBEs)
            total += be.emfVoltage();
        return total;
    }

    public float outputVoltage() {
        float min = windingVoltage();
        if(parallelPositions != null) {
            var iter = parallelPositions.iterator();
            while (iter.hasNext()) {
                var pos = iter.next();
                var be = level.getBlockEntity(pos, ModdedBlockEntities.WINDING.get());
                if (be.isEmpty()) {
                    iter.remove();
                    continue;
                }
                var V = be.get().windingVoltage();
                if((V > 0 && min < 0) || (V < 0 && min > 0)) {
                    // Opposite signs not allowed for parallel coils
                    return 0;
                }
                if(Math.abs(V) < Math.abs(min))
                    min = V;
            }
        }
        return min;
    }

    public void grabRotors() {
        var state = getBlockState();
        var along = state.getValue(ALONG_FIRST_AXIS);
        Direction.Axis magneticAxis = switch(state.getValue(AXIS)) {
            case X -> along ? Direction.Axis.Y : Direction.Axis.Z;
            case Y -> along ? Direction.Axis.Z : Direction.Axis.X;
            case Z -> along ? Direction.Axis.Y : Direction.Axis.X;
        };

        rotorP = BlockEntityBehaviour.get(level, worldPosition.relative(magneticAxis, 1), RotorBehaviour.TYPE);
        if(rotorP != null && (!rotorP.hasField() || rotorP.getAxis() == magneticAxis))
            rotorP = null;
        rotorN = BlockEntityBehaviour.get(level, worldPosition.relative(magneticAxis, -1), RotorBehaviour.TYPE);
        if(rotorN != null && (!rotorN.hasField() || rotorN.getAxis() == magneticAxis))
            rotorN = null;
        sendData();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if(isMain()) {
            electricBehaviour = new ElectricBehaviour(this);
            behaviours.add(electricBehaviour);
        }

        thermalBehaviour = specifyThermalBehaviour();
        if(thermalBehaviour != null)
            behaviours.add(thermalBehaviour);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.simple(this, 2.0f, 0.1f);
    }

    private void checkParallelPosition(BlockPos pos, boolean positive, boolean thisIsOwner) {
        var state = level.getBlockState(pos);
        var thisState = getBlockState();
        if(state.getBlock() instanceof WindingBlock windingBlock) {
            // Another winding, check for alignment
            var be = windingBlock.getMainBlockEntity(level, pos);
            be.ifPresent(winding -> {
                if(state.getValue(AXIS) == thisState.getValue(AXIS) && state.getValue(ALONG_FIRST_AXIS) == thisState.getValue(ALONG_FIRST_AXIS)) {
                    // Alignment matches and block entity is valid, these can be connected.
                    if(thisIsOwner) {
                        this.addParallel(winding);
                    } else {
                        winding.addParallel(this);
                    }
                }
            });
        } else if(state.getBlock() instanceof GeneratorHousing) {
            var windingBlock = (WindingBlock) thisState.getBlock();
            var parallelAxis = windingBlock.getParallelCheckAxis(thisState);
            if(parallelAxis.isHorizontal()) {
                var expectedFacing = Direction.fromAxisAndDirection(parallelAxis, positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                if(state.getValue(HORIZONTAL_FACING) != expectedFacing)
                    return;
                pos = state.getValue(UP) ? pos.above() : pos.below();
                var nextState = level.getBlockState(pos);
                var be = windingBlock.getMainBlockEntity(level, pos);
                be.ifPresent(winding -> {
                    if(nextState.getValue(AXIS) == thisState.getValue(AXIS) && nextState.getValue(ALONG_FIRST_AXIS) != thisState.getValue(ALONG_FIRST_AXIS)) {
                        // Alignment matches and block entity is valid, these can be connected.
                        if(thisIsOwner) {
                            this.addParallel(winding);
                        } else {
                            winding.addParallel(this);
                        }
                    }
                });
            } else {
                var expectUp = !positive;
                if(state.getValue(UP) != expectUp)
                    return;
                pos = pos.relative(state.getValue(HORIZONTAL_FACING));
                var nextState = level.getBlockState(pos);
                var be = windingBlock.getMainBlockEntity(level, pos);
                be.ifPresent(winding -> {
                    if(nextState.getValue(AXIS) == thisState.getValue(AXIS) && nextState.getValue(ALONG_FIRST_AXIS) != thisState.getValue(ALONG_FIRST_AXIS)) {
                        // Alignment matches and block entity is valid, these can be connected.
                        if(thisIsOwner) {
                            this.addParallel(winding);
                        } else {
                            winding.addParallel(this);
                        }
                    }
                });
            }
        }
    }

    private void rewire() {
        if(electricBehaviour != null) {
            for(var wires : electricBehaviour.getConnections().values()) {
                for(var wire : wires) {
                    wire.dropWire();
                }
            }
            for(var wires : electricBehaviour.getConnections().values()) {
                for(var wire : wires) {
                    wire.makeWire();
                }
            }
        }
    }

    public void addElectricBehaviour() {
        if(electricBehaviour == null) {
            electricBehaviour = new ElectricBehaviour(this);
            attachBehaviourLate(electricBehaviour);
        } else if(electricBehaviour instanceof ProxyElectricBehaviour proxy) {
            electricBehaviour = new ElectricBehaviour(this);
            electricBehaviour.inheritConnections(proxy);
            removeBehaviour(ElectricBehaviour.TYPE);
            attachBehaviourLate(electricBehaviour);
        }
        rewire();
    }

    public void removeElectricBehaviour() {
        if(electricBehaviour != null && !(electricBehaviour instanceof ProxyElectricBehaviour)) {
            var old = electricBehaviour;
            electricBehaviour = new ProxyElectricBehaviour(this, () -> ownerPosition);
            electricBehaviour.inheritConnections(old);
            removeBehaviour(ElectricBehaviour.TYPE);
            attachBehaviourLate(electricBehaviour);
            // Drop nodes
            sourceNode = null;
            coupling = null;
        }
        rewire();
    }

    private void collectWindingParts() {
        var block = (WindingBlock) getBlockState().getBlock();
        var parallelCheckAxis = block.getParallelCheckAxis(getBlockState());
        if(isMain()) {
            mainBE = this;
            collectedBEs = new HashSet<>();
            block.walk(level, worldPosition, (pos1, state) -> {
                var opt = level.getBlockEntity(pos1, ModdedBlockEntities.WINDING.get());
                if(opt.isEmpty()) {
                    level.destroyBlock(pos1, false);
                    return;
                }
                var be = opt.get();
                collectedBEs.add(be);
                if(be.collectedBEs != null && be != this) {
                    // Moving ownership
                    // These parallels will hopefully be picked back up.
                    if(be.ownerPosition != null) {
                        // This is a parallel
                        level.getBlockEntity(be.ownerPosition, ModdedBlockEntities.WINDING.get())
                                .ifPresent(owner -> {
                                    owner.dissolveParallels();
                                });
                    } else if(be.parallelPositions != null) {
                        // This is an owner
                        be.dissolveParallels();
                        be.electricBehaviour.breakConnections();
                        be.removeElectricBehaviour();
                    }
                    // Move collected segments
                    be.collectedBEs.forEach(other -> other.mainBE = this);
                    be.collectedBEs = null;
                    be.mainBE = this;
                    if(be.electricBehaviour != null) {
                        be.electricBehaviour.breakConnections();
                        be.electricBehaviour = null;
                        be.removeBehaviour(ElectricBehaviour.TYPE);
                        // Drop nodes
                        be.sourceNode = null;
                        be.coupling = null;
                    }
                }
                if(!level.isClientSide) {
                    // Check for parallel windings and housings
                    checkParallelPosition(pos1.relative(parallelCheckAxis,  1), true, false);
                    checkParallelPosition(pos1.relative(parallelCheckAxis, -1), false, false);
                }
            });
            calculateElectricalParameters();
        } else {
            var opt = block.getMainBlockEntity(level, worldPosition);
            if(opt.isEmpty()) {
                level.destroyBlock(worldPosition, false);
                return;
            }
            mainBE = opt.get();
            if(mainBE.collectedBEs != null && mainBE.collectedBEs.add(this)) {
                // Late segment join
                mainBE.electricBehaviour.breakConnections();
                mainBE.calculateElectricalParameters();
                mainBE.safeRebuildParallels();
            }
        }
    }

    public void makeMain() {
        if(mainBE == null || mainBE == this)
            return;
        collectedBEs = mainBE.collectedBEs;
        collectedBEs.remove(mainBE);
        collectedBEs.forEach(be -> be.mainBE = this);
        calculateElectricalParameters();
        if(!level.isClientSide)
            safeRebuildParallels();
    }


    private void addParallel(WindingBlockEntity otherMain) {
        assert isMain() : "Only main block entities can keep track of parallel windings";
        assert otherMain.isMain() : "Parallel block entities must be the main entities of their windings";
        assert !level.isClientSide : "Parallel block entity collection can only occur on server";
        if(otherMain == this)
            return;
        if(ownerPosition != null) {
            var ownerWinding = level.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get());
            ownerWinding.ifPresentOrElse(owner -> owner.addParallel(otherMain), () -> {
                // Owner is no longer valid, we become the new owner.
                ownerPosition = null;
                calculateElectricalParameters();
            });
        }
        if(ownerPosition == null) {
            if(parallelPositions == null)
                parallelPositions = new HashSet<>();
            if(!parallelPositions.add(otherMain.getBlockPos())) {
                // Already handled, don't need any more checking.
                return;
            }
            if(otherMain.parallelPositions != null) {
                otherMain.parallelPositions.forEach(otherPos -> {
                    var be = level.getBlockEntity(otherPos, ModdedBlockEntities.WINDING.get());
                    // Add only valid windings
                    be.ifPresent(winding -> {
                        // Update the owner
                        winding.ownerPosition = worldPosition;
                        parallelPositions.add(otherPos);
                    });
                });
                otherMain.parallelPositions = null;
            } else if(otherMain.ownerPosition != null) {
                var be = level.getBlockEntity(otherMain.ownerPosition, ModdedBlockEntities.WINDING.get());
                // Merge owners
                be.ifPresent(this::addParallel);
            }
            otherMain.ownerPosition = worldPosition;
            otherMain.removeElectricBehaviour();
            // Synchronize to client
            otherMain.sendData();
            sendData();
        }
    }

    public WindingBlockEntity getBehaviourProvider() {
        if(!isMain())
            return mainBE.getBehaviourProvider();
        return this;
    }

    @Override
    public void initialize() {
        if(collectedBEs == null)
            collectWindingParts();
        super.initialize();
        grabRotors();
    }

    public int getCoilCount() {
        if(collectedBEs == null)
            collectWindingParts();
        return collectedBEs.size();
    }

    private void calculateElectricalParameters() {
        if(ownerPosition != null) {
            // If non-owner calls this method then its structure (and possibly resistance) has changed
            level.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get()).ifPresent(WindingBlockEntity::calculateElectricalParameters);
            return;
        }
        totalCoilCount = getCoilCount();
        var conductance = 1 / (totalCoilCount * resistance());
        if(parallelPositions != null) {
            var iter = parallelPositions.iterator();
            while (iter.hasNext()) {
                var windingPos = iter.next();
                var be = level.getBlockEntity(windingPos, ModdedBlockEntities.WINDING.get());
                if (be.isPresent()) {
                    var winding = be.get();
                    totalCoilCount += winding.getCoilCount();
                    conductance += 1 / (winding.getCoilCount() * resistance());
                } else {
                    iter.remove();
                }
            }
        }
        resistance = 1 / conductance;
        if(sourceNode == null) {
            // We only need the nodes on the owner block entity
            addElectricBehaviour();
        } else {
            coupling.setResistance(resistance);
        }
        if(!level.isClientSide)
            sendData();
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(clientPacket) {
            rotorP = null;
            rotorN = null;
            ownerPosition = null;
            parallelPositions = null;
            if(tag.contains("RotorP")) {
                var posArray = tag.getIntArray("RotorP");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                rotorP = BlockEntityBehaviour.get(level, pos, RotorBehaviour.TYPE);
            }
            if(tag.contains("RotorN")) {
                var posArray = tag.getIntArray("RotorN");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                rotorN = BlockEntityBehaviour.get(level, pos, RotorBehaviour.TYPE);
            }
            if(isMain()) {
                if (tag.contains("Owner")) {
                    var owner = tag.getIntArray("Owner");
                    ownerPosition = new BlockPos(owner[0], owner[1], owner[2]);
                    removeElectricBehaviour();
                } else if(mainBE != null) {
                    calculateElectricalParameters();
                }
                if (tag.contains("Parallel")) {
                    var data = tag.getIntArray("Parallel");
                    parallelPositions = new HashSet<>();
                    for (int i = 0; i < data.length; i += 3) {
                        var pos = new BlockPos(data[i], data[i + 1], data[i + 2]);
                        parallelPositions.add(pos);
                    }
                    if(mainBE != null) {
                        // If main block entity is set then the initialization has happened
                        calculateElectricalParameters();
                    }
                }
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(clientPacket) {
            if(rotorP != null) {
                var pos = rotorP.getPos();
                tag.putIntArray("RotorP", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            if(rotorN != null) {
                var pos = rotorN.getPos();
                tag.putIntArray("RotorN", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            if(isMain()) {
                if (ownerPosition != null) {
                    tag.putIntArray("Owner", new int[]{ownerPosition.getX(), ownerPosition.getY(), ownerPosition.getZ()});
                }
                if (parallelPositions != null) {
                    var data = parallelPositions.stream()
                            .flatMap(pos -> Stream.of(pos.getX(), pos.getY(), pos.getZ()))
                            .toList();
                    tag.putIntArray("Parallel", data);
                }
            }
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(ownerPosition == null) {
            builder.setTerminalCount(2);
            sourceNode = builder.addInternalNode(VoltageSourceNode.class);
            coupling = builder.couple(1, resistance, sourceNode, builder.terminalNode(0), builder.terminalNode(1));
        }
    }

    private void rebuildParallels() {
        if(level.isClientSide)
            return;
        if(ownerPosition != null)
            PowerGrid.LOGGER.info("Non-owner winding is rebuilding parallels");
        dissolveParallels();
        var block = (WindingBlock) getBlockState().getBlock();
        var parallelCheckAxis = block.getParallelCheckAxis(getBlockState());
        // This block entity becomes the new owner, since it most likely already was one.
        // Perform the initial walk
        block.walk(level, worldPosition, (pos1, state) -> {
            // Check for parallel windings and housings
            checkParallelPosition(pos1.relative(parallelCheckAxis, 1), true, true);
            checkParallelPosition(pos1.relative(parallelCheckAxis, -1), false, true);
        });
        if(parallelPositions != null) {
            var checkedPositions = new HashSet<BlockPos>();
            checkedPositions.add(worldPosition);
            // Continue checking until no more parallel windings are added.
            boolean shouldContinue = true;
            while (shouldContinue) {
                shouldContinue = false;
                var checkPositions = List.copyOf(parallelPositions);
                for (var position : checkPositions) {
                    // Check if this wasn't previously checked
                    if (checkedPositions.add(position)) {
                        block.walk(level, position, (pos1, state) -> {
                            // Check for parallel windings and housings
                            checkParallelPosition(pos1.relative(parallelCheckAxis, 1), true, true);
                            checkParallelPosition(pos1.relative(parallelCheckAxis, -1), false, true);
                        });
                        shouldContinue = true;
                    }
                }
            }
        }
        calculateElectricalParameters();
        sendData();
    }

    private void dissolveParallels() {
        if(parallelPositions != null) {
            for(var parallelPos : parallelPositions) {
                var be = level.getBlockEntity(parallelPos, ModdedBlockEntities.WINDING.get());
                be.ifPresent(winding -> {
                    winding.ownerPosition = null;
                    winding.calculateElectricalParameters();
                });
            }
            parallelPositions = null;
        }
        if(ownerPosition != null)
            PowerGrid.LOGGER.info("Non-owner winding is dissolving parallels");
        ownerPosition = null;
        calculateElectricalParameters();
    }

    private void moveParallelOwnership(WindingBlockEntity newOwner, boolean withoutThis) {
        if(newOwner == this)
            return;
        assert this.ownerPosition == null && (worldPosition.equals(newOwner.ownerPosition) || newOwner.ownerPosition == null);
        parallelPositions.remove(newOwner.worldPosition);
        if(!withoutThis)
            parallelPositions.add(worldPosition);
        newOwner.parallelPositions = parallelPositions;
        parallelPositions = null;
        if(!withoutThis)
            ownerPosition = newOwner.worldPosition;
        removeElectricBehaviour();
        newOwner.ownerPosition = null;
        newOwner.parallelPositions.forEach(bePos -> level.getBlockEntity(bePos, ModdedBlockEntities.WINDING.get())
                .ifPresent(be -> be.ownerPosition = newOwner.worldPosition));
        newOwner.calculateElectricalParameters();
    }

    public void onNeighborChanged(BlockPos neighborPos) {
        neighborChanged = true;
    }

    private void safeRebuildParallels() {
        if(mainBE == null)
            return;
        if(mainBE.parallelPositions != null) {
            // This is the owner
            mainBE.rebuildParallels();
        } else if(mainBE.ownerPosition != null) {
            // Inform the owner
            var be = level.getBlockEntity(mainBE.ownerPosition, ModdedBlockEntities.WINDING.get());
            be.ifPresentOrElse(WindingBlockEntity::rebuildParallels, mainBE::rebuildParallels);
        } else {
            // Simply rebuild
            mainBE.rebuildParallels();
        }
    }

    public float windingCurrent() {
        if(mainBE == null)
            return 0;
        if(mainBE.ownerPosition != null) {
            if(mainBE.ownerPosition.equals(worldPosition)) {
                // A winding cannot be owned by itself.
                // This is an invalid state that can be caused if the client doesn't receive all data on time.
                return 0;
            }
            var be = level.getBlockEntity(mainBE.ownerPosition, ModdedBlockEntities.WINDING.get());
            if(be.isPresent()) {
                return be.get().windingCurrent();
            }
        }
        if(mainBE.sourceNode == null)
            return 0;
        return mainBE.sourceNode.getCurrent() / mainBE.totalCoilCount;
    }

    @Override
    public void remove() {
        super.remove();
        // Always break connections when the winding is modified.
        if(mainBE != null)
            mainBE.electricBehaviour.breakConnections();
        if(mainBE == this) {
            if(parallelPositions != null) {
                // This is the owner
                WindingBlockEntity newOwner = null;
                var iter = parallelPositions.iterator();
                while(newOwner == null && iter.hasNext()) {
                    var bePos = iter.next();
                    var be = level.getBlockEntity(bePos, ModdedBlockEntities.WINDING.get());
                    if(be.isPresent())
                        newOwner = be.get();
                }
                if(newOwner != null)
                    moveParallelOwnership(newOwner, true);
            } else if(ownerPosition != null) {
                // Inform the owner
                var be = level.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get());
                be.ifPresentOrElse(WindingBlockEntity::rebuildParallels, this::rebuildParallels);
            }
//            collectedBEs.forEach(be -> be.mainBE = null);
        } else if(mainBE != null) {
            // Segment of a winding
            mainBE.collectedBEs.remove(this);
            mainBE.calculateElectricalParameters();
            mainBE.safeRebuildParallels();
        }
    }

    @Override
    public void tick() {
        if(neighborChanged) {
            safeRebuildParallels();
            grabRotors();
            neighborChanged = false;
        }
        super.tick();
        float current = windingCurrent();
        applyLostPower(current * current * WindingBlock.resistance());

        if(rotorP != null) {
            float torque = coilConstant * rotorP.getFieldStrength() * current;

            float Pe = current * emfVoltage();
            if (Pe > 0) {
                // Generator is sourcing power
                torque *= 1.0f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
            rotorP.applyTickForce(rotorP.limitForce(torque));
        }
        if(rotorN != null) {
            float torque = coilConstant * rotorN.getFieldStrength() * current;

            float Pe = current * emfVoltage();
            if (Pe > 0) {
                // Generator is sourcing power
                torque *= 1.0f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
            rotorN.applyTickForce(rotorN.limitForce(torque));
        }

        if(sourceNode != null) {
            if(!isMain()) {
                PowerGrid.LOGGER.warn("Non-main winding has a source node.");
                return;
            }
            sourceNode.setVoltage(outputVoltage());
        }
    }
}
