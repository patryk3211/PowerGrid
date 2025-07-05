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
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

import java.util.*;
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
    private List<WindingBlockEntity> collectedBEs;
    private RotorBehaviour rotorP;
    private RotorBehaviour rotorN;

    private float coilConstant = 1;
    private float resistance = 0.1f;
    private int coilCount = 0;
    private int totalCoilCount = 0;
    private VoltageSourceNode sourceNode;
    private TransformerCoupling coupling;

    private boolean neighborChanged = false;

    public WindingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean isMain() {
        return getCachedState().get(PART) == 0;
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
                var be = world.getBlockEntity(pos, ModdedBlockEntities.WINDING.get());
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
        var state = getCachedState();
        var along = state.get(ALONG_FIRST_AXIS);
        Direction.Axis magneticAxis = switch(state.get(AXIS)) {
            case X -> along ? Direction.Axis.Y : Direction.Axis.Z;
            case Y -> along ? Direction.Axis.Z : Direction.Axis.X;
            case Z -> along ? Direction.Axis.Y : Direction.Axis.X;
        };

        rotorP = BlockEntityBehaviour.get(world, pos.offset(magneticAxis, 1), RotorBehaviour.TYPE);
        if(rotorP != null && rotorP.blockEntity.getCachedState().get(AXIS) == magneticAxis)
            rotorP = null;
        rotorN = BlockEntityBehaviour.get(world, pos.offset(magneticAxis, -1), RotorBehaviour.TYPE);
        if(rotorN != null && rotorN.blockEntity.getCachedState().get(AXIS) == magneticAxis)
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
        return new ThermalBehaviour(this, 2.0f, 0.1f);
    }

    private void checkParallelPosition(BlockPos pos, boolean positive, boolean thisIsOwner) {
        var state = world.getBlockState(pos);
        var thisState = getCachedState();
        if(state.getBlock() instanceof WindingBlock windingBlock) {
            // Another winding, check for alignment
            var be = windingBlock.getMainBlockEntity(world, pos);
            be.ifPresent(winding -> {
                if(state.get(AXIS) == thisState.get(AXIS) && state.get(ALONG_FIRST_AXIS) == thisState.get(ALONG_FIRST_AXIS)) {
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
                var expectedFacing = Direction.from(parallelAxis, positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
                if(state.get(HORIZONTAL_FACING) != expectedFacing)
                    return;
                pos = state.get(UP) ? pos.up() : pos.down();
                var nextState = world.getBlockState(pos);
                var be = windingBlock.getMainBlockEntity(world, pos);
                be.ifPresent(winding -> {
                    if(nextState.get(AXIS) == thisState.get(AXIS) && nextState.get(ALONG_FIRST_AXIS) != thisState.get(ALONG_FIRST_AXIS)) {
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
                if(state.get(UP) != expectUp)
                    return;
                pos = pos.offset(state.get(HORIZONTAL_FACING));
                var nextState = world.getBlockState(pos);
                var be = windingBlock.getMainBlockEntity(world, pos);
                be.ifPresent(winding -> {
                    if(nextState.get(AXIS) == thisState.get(AXIS) && nextState.get(ALONG_FIRST_AXIS) != thisState.get(ALONG_FIRST_AXIS)) {
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

    public void addElectricBehaviour() {
        if(electricBehaviour == null) {
            electricBehaviour = new ElectricBehaviour(this);
            attachBehaviourLate(electricBehaviour);
        }
    }

    public void removeElectricBehaviour() {
        if(electricBehaviour != null) {
            electricBehaviour.breakConnections();
            electricBehaviour = null;
            removeBehaviour(ElectricBehaviour.TYPE);
            // Drop nodes
            sourceNode = null;
            coupling = null;
        }
    }

    private void collectWindingParts() {
        var block = (WindingBlock) getCachedState().getBlock();
        var parallelCheckAxis = block.getParallelCheckAxis(getCachedState());
        if(isMain()) {
            mainBE = this;
            collectedBEs = new ArrayList<>();
            coilCount = 0;
            block.walk(world, pos, (pos1, state) -> {
                ++coilCount;
                var opt = world.getBlockEntity(pos1, ModdedBlockEntities.WINDING.get());
                if(opt.isEmpty()) {
                    world.breakBlock(pos1, false);
                    return;
                }
                collectedBEs.add(opt.get());
                if(!world.isClient) {
                    // Check for parallel windings and housings
                    checkParallelPosition(pos1.offset(parallelCheckAxis,  1), true, false);
                    checkParallelPosition(pos1.offset(parallelCheckAxis, -1), false, false);
                }
            });
            resistance = coilCount * WindingBlock.resistance();
            if(ownerPosition == null)
                calculateElectricalParameters();
        } else {
            var opt = block.getMainBlockEntity(world, pos);
            if(opt.isEmpty()) {
                world.breakBlock(pos, false);
                return;
            }
            mainBE = opt.get();
        }
    }

    private void addParallel(WindingBlockEntity otherMain) {
        assert isMain() : "Only main block entities can keep track of parallel windings";
        assert otherMain.isMain() : "Parallel block entities must be the main entities of their windings";
        assert !world.isClient : "Parallel block entity collection can only occur on server";
        if(otherMain == this)
            return;
        if(ownerPosition != null) {
            var ownerWinding = world.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get());
            ownerWinding.ifPresentOrElse(owner -> owner.addParallel(otherMain), () -> {
                // Owner is no longer valid, we become the new owner.
                ownerPosition = null;
            });
        }
        if(ownerPosition == null) {
            if(parallelPositions == null)
                parallelPositions = new HashSet<>();
            if(!parallelPositions.add(otherMain.getPos())) {
                // Already handled, don't need any more checking.
                return;
            }
            if(otherMain.parallelPositions != null) {
                otherMain.parallelPositions.forEach(otherPos -> {
                    var be = world.getBlockEntity(otherPos, ModdedBlockEntities.WINDING.get());
                    // Add only valid windings
                    be.ifPresent(winding -> {
                        // Update the owner
                        winding.ownerPosition = pos;
                        parallelPositions.add(otherPos);
                    });
                });
                otherMain.parallelPositions = null;
            } else if(otherMain.ownerPosition != null) {
                var be = world.getBlockEntity(otherMain.ownerPosition, ModdedBlockEntities.WINDING.get());
                // Merge owners
                be.ifPresent(this::addParallel);
            }
            otherMain.ownerPosition = pos;
            // Move existing connections over.
            if(electricBehaviour != null && otherMain.electricBehaviour != null) {
                electricBehaviour.inheritConnections(otherMain.electricBehaviour);
                PowerGrid.LOGGER.debug("Connection inheritance happened when parallel was added");
            }
            otherMain.removeElectricBehaviour();
        }
    }

    public WindingBlockEntity getBehaviourProvider() {
        if(!isMain())
            return mainBE.getBehaviourProvider();
        if(ownerPosition == null)
            return this;
        return world.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get()).orElse(this);
    }

    @Override
    public void initialize() {
        if(coilCount == 0)
            collectWindingParts();
        super.initialize();
        grabRotors();
    }

    public int getCoilCount() {
        if(coilCount == 0)
            collectWindingParts();
        return coilCount;
    }

    private void calculateElectricalParameters() {
        assert ownerPosition == null : "Only owner of parallel coils may recalculate electrical parameters";
        totalCoilCount = getCoilCount();
        var conductance = 1 / (totalCoilCount * resistance());
        if(parallelPositions != null) {
            var iter = parallelPositions.iterator();
            while (iter.hasNext()) {
                var windingPos = iter.next();
                var be = world.getBlockEntity(windingPos, ModdedBlockEntities.WINDING.get());
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
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(clientPacket) {
            rotorP = null;
            rotorN = null;
            ownerPosition = null;
            parallelPositions = null;
            if(tag.contains("RotorP")) {
                var posArray = tag.getIntArray("RotorP");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                rotorP = BlockEntityBehaviour.get(world, pos, RotorBehaviour.TYPE);
            }
            if(tag.contains("RotorN")) {
                var posArray = tag.getIntArray("RotorN");
                var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                rotorN = BlockEntityBehaviour.get(world, pos, RotorBehaviour.TYPE);
            }
            if(isMain()) {
                if (tag.contains("Owner")) {
                    var owner = tag.getIntArray("Owner");
                    ownerPosition = new BlockPos(owner[0], owner[1], owner[2]);
                    removeElectricBehaviour();
                } else {
                    addElectricBehaviour();
                }
                if (tag.contains("Parallel")) {
                    var data = tag.getIntArray("Parallel");
                    parallelPositions = new HashSet<>();
                    for (int i = 0; i < data.length; i += 3) {
                        var pos = new BlockPos(data[i], data[i + 1], data[i + 2]);
                        parallelPositions.add(pos);
                    }
                }
            }
        }
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
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
        if(world.isClient)
            return;
        if(parallelPositions != null) {
            for(var parallelPos : parallelPositions) {
                var be = world.getBlockEntity(parallelPos, ModdedBlockEntities.WINDING.get());
                be.ifPresent(winding -> winding.ownerPosition = null);
            }
            parallelPositions = null;
        }
        if(ownerPosition != null)
            PowerGrid.LOGGER.info("Non-owner winding is rebuilding parallels");
        ownerPosition = null;
        var block = (WindingBlock) getCachedState().getBlock();
        var parallelCheckAxis = block.getParallelCheckAxis(getCachedState());
        // This block entity becomes the new owner, since it most likely already was one.
        // Perform the initial walk
        block.walk(world, pos, (pos1, state) -> {
            // Check for parallel windings and housings
            checkParallelPosition(pos1.offset(parallelCheckAxis, 1), true, true);
            checkParallelPosition(pos1.offset(parallelCheckAxis, -1), false, true);
        });
        if(parallelPositions != null) {
            var checkedPositions = new HashSet<BlockPos>();
            checkedPositions.add(pos);
            // Continue checking until no more parallel windings are added.
            boolean shouldContinue = true;
            while (shouldContinue) {
                shouldContinue = false;
                var checkPositions = List.copyOf(parallelPositions);
                for (var position : checkPositions) {
                    // Check if this wasn't previously checked
                    if (checkedPositions.add(position)) {
                        block.walk(world, position, (pos1, state) -> {
                            // Check for parallel windings and housings
                            checkParallelPosition(pos1.offset(parallelCheckAxis, 1), true, true);
                            checkParallelPosition(pos1.offset(parallelCheckAxis, -1), false, true);
                        });
                        shouldContinue = true;
                    }
                }
            }
            if(electricBehaviour != null) {
                // Rewire all the wires.
                for(var list : electricBehaviour.getConnections().values()) {
                    for(var wire : list) {
                        wire.makeWire();
                    }
                }
            }
        }
        calculateElectricalParameters();
        sendData();
    }

    public void onNeighborChanged(BlockPos neighborPos) {
        neighborChanged = true;
    }

    public float windingCurrent() {
        if(mainBE == null || mainBE.sourceNode == null)
            return 0;
        if(mainBE.ownerPosition != null) {
            var be = world.getBlockEntity(mainBE.ownerPosition, ModdedBlockEntities.WINDING.get());
            if(be.isPresent()) {
                return be.get().windingCurrent();
            }
        }
        return mainBE.sourceNode.getCurrent() / mainBE.totalCoilCount;
    }

    @Override
    public void tick() {
        if(neighborChanged) {
            if(mainBE.parallelPositions != null) {
                // This is the owner
                mainBE.rebuildParallels();
            } else if(mainBE.ownerPosition != null) {
                // Inform the owner
                var be = world.getBlockEntity(mainBE.ownerPosition, ModdedBlockEntities.WINDING.get());
                be.ifPresentOrElse(WindingBlockEntity::rebuildParallels, mainBE::rebuildParallels);
            } else {
                // Simply rebuild
                mainBE.rebuildParallels();
            }
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
                // Apply more torque to account for losses, use
                // those losses to heat the coil up.
                torque *= 1.1f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
//            float Pm = rotorP.getAngularVelocityRadians() * torque;
//            PowerGrid.LOGGER.info("Efficiency: {}", Pm / Pe);
//            PowerGrid.LOGGER.info("P_e: {}", current * emfVoltage());
//            PowerGrid.LOGGER.info("P_m: {}", rotor.getAngularVelocityRadians() * torque);

            rotorP.applyTickForce(torque);
        }
        if(rotorN != null) {
            float torque = coilConstant * rotorN.getFieldStrength() * current;

            float Pe = current * emfVoltage();
            if (Pe > 0) {
                // Generator is sourcing power
                // Apply more torque to account for losses, use
                // those losses to heat the coil up.
                torque *= 1.1f;
            } else {
                // Generator is sinking power
                // Reduce torque to account for losses
                torque *= 0.9f;
            }
//            float Pm = rotorN.getAngularVelocityRadians() * torque;
//            PowerGrid.LOGGER.info("Efficiency: {}", Pm / Pe);
//            PowerGrid.LOGGER.info("P_e: {}", current * emfVoltage());
//            PowerGrid.LOGGER.info("P_m: {}", rotor.getAngularVelocityRadians() * torque);

            rotorN.applyTickForce(torque);
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
