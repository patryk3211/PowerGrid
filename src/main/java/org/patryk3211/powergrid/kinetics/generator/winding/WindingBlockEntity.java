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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.sim.calculation.Precalculated;
import org.patryk3211.powergrid.electricity.sim.calculation.Precalculated1;
import org.patryk3211.powergrid.electricity.sim.calculation.StampedSupplier;
import org.patryk3211.powergrid.electricity.sim.special.LRSeriesWire;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.AXIS;
import static org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock.PART;

public class WindingBlockEntity extends ElectricBlockEntity implements IMultipartSync {
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

    private float resistance = 0.1f;
    private int totalCoilCount = 0;
    private LRSeriesWire coilWire;
    private final Precalculated1<Float, StampedSupplier<LRSeriesWire>> fieldValue = new Precalculated1<>(WindingBlockEntity::fieldCalc, 0f);

    private boolean rebuildParallels = false;

    public WindingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        fieldValue.updateDependency(() -> coilWire);
        setLazyTickRate(40);
    }

    public static float coilConstant() {
        return ModdedConfigs.server().kinetics.generatorControls.windingCoilConstant.getF();
    }

    private boolean isMain() {
        return getBlockState().getValue(PART) == 0;
    }

    private static double fieldStrength(double current) {
        final float I_sat = ModdedConfigs.server().kinetics.generatorControls.fieldSaturationCurrent.getF();
        current = (I_sat * Math.tanh(1.5 * current / I_sat)) + current * 0.05;
        return current * coilConstant() + 0.001;
    }

    private static void fieldCalc(Supplier<LRSeriesWire> coilSupplier, Precalculated<Float>.ValueHandler handler) {
        var coil = coilSupplier.get();
        if(coil == null)
            return;
        if(!coil.isConverged())
            return;
        double Bprev = handler.get();
        double B = fieldStrength(coil.current());
        if(Bprev * B < 0) {
            handler.emit((float) (0.001 * Math.signum(B)));
        } else {
            handler.emit((float) B);
        }
    }

    public Precalculated<Float> fieldStrengthCalc() {
        var be = getSimElementHolder();
        if(be == null)
            return null;
        return be.fieldValue;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if(isMain()) {
            electricBehaviour = new ElectricBehaviour(this);
        } else {
            var block = (WindingBlock) getBlockState().getBlock();
            electricBehaviour = new ProxyElectricBehaviour(this, () -> block.getMainBlockPos(level, worldPosition));
        }
        behaviours.add(electricBehaviour);

        thermalBehaviour = specifyThermalBehaviour();
        if(thermalBehaviour != null)
            behaviours.add(thermalBehaviour);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var thermal = ThermalBehaviour.fromConfig(this);
        if(thermal != null) {
            thermal.overheatCallback(() -> {
                assert level != null;
                var block = (WindingBlock) getBlockState().getBlock();
                block.walk(level, worldPosition, (pos1, state) -> {
                    level.destroyBlock(pos1, false);
                });
            });
        }
        return thermal;
    }

    private void checkParallelPosition(BlockPos thisPos, Direction side, boolean thisIsOwner) {
        assert level != null;
        var checkPos = thisPos.relative(side);
        var checkState = level.getBlockState(checkPos);
        var thisState = level.getBlockState(thisPos);

        var thisConnectible = (IWindingConnectable) thisState.getBlock();
        if(!(checkState.getBlock() instanceof IWindingConnectable checkConnectible))
            return;
        if(!thisConnectible.canConnect(thisState, side))
            return;
        if(!checkConnectible.canConnect(checkState, side.getOpposite()))
            return;
        if(checkState.getBlock() instanceof WindingBlock windingBlock) {
            // Another winding, check for alignment
            var be = windingBlock.getMainBlockEntity(level, checkPos);
            be.ifPresent(winding -> {
                if(checkState.getValue(AXIS) == thisState.getValue(AXIS)) {
                    // Alignment matches and block entity is valid, these can be connected.
                    if(thisIsOwner) {
                        this.addParallel(winding);
                    } else {
                        winding.addParallel(this);
                    }
                }
            });
        } else {
            // We need to go through a connectible block into another winding.
            var sideOut = checkConnectible.getOtherSide(checkState, side.getOpposite());
            checkPos = checkPos.relative(sideOut);
            var checkState2 = level.getBlockState(checkPos);
            if(!(checkState2.getBlock() instanceof WindingBlock windingBlock))
                return;
            if(!windingBlock.canConnect(checkState2, sideOut.getOpposite()))
                return;
            if(!checkConnectible.canConnect(checkState, sideOut))
                return;
            var be = windingBlock.getMainBlockEntity(level, checkPos);
            be.ifPresent(winding -> {
                if(checkState2.getValue(AXIS) == thisState.getValue(AXIS)) {
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

    @NotNull
    private Collection<TransmissionLinePart> wires() {
        if(electricBehaviour == null)
            return List.of();
        return GlobalElectricNetworks.getWorldNetworks(level).findConnectedWires(electricBehaviour);
    }

    private void rewire() {
        if(electricBehaviour != null) {
            var wires = wires();
            if(electricBehaviour instanceof ProxyElectricBehaviour proxy)
                proxy.refreshEndpoints();
            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
        }
    }

    public void addElectricBehaviour() {
        var wires = wires();
        if(electricBehaviour == null) {
            electricBehaviour = new ElectricBehaviour(this);
            attachBehaviourLate(electricBehaviour);
        } else if(electricBehaviour instanceof ProxyElectricBehaviour proxy) {
            electricBehaviour = new ElectricBehaviour(this);
            electricBehaviour.inheritConnections(proxy);
            attachBehaviourLate(electricBehaviour);
        }
        wires.forEach(TransmissionLinePart::refreshEndpointNodes);
    }

    public void removeElectricBehaviour() {
        var wires = wires();
        ElectricBehaviour old = null;
        if(electricBehaviour != null && !(electricBehaviour instanceof ProxyElectricBehaviour)) {
            old = electricBehaviour;
            electricBehaviour = new ProxyElectricBehaviour(this, () -> ownerPosition);
            electricBehaviour.inheritConnections(old);
            // Pause to remove internals
            old.pause();
            attachBehaviourLate(electricBehaviour);
            // Drop nodes
            coilWire = null;
        }
        wires.forEach(TransmissionLinePart::refreshEndpointNodes);
        if(old != null)
            old.remove();
    }

    private void collectWindingParts() {
        assert level != null;
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
                                .ifPresent(WindingBlockEntity::dissolveParallels);
                    } else if(be.parallelPositions != null) {
                        if(parallelPositions == null)
                            parallelPositions = new HashSet<>();
                        parallelPositions.addAll(be.parallelPositions);
                        // This is an owner
                        for(var parallelPos : parallelPositions) {
                            var be2 = level.getBlockEntity(parallelPos, ModdedBlockEntities.WINDING.get());
                            be2.ifPresent(winding -> {
                                winding.ownerPosition = worldPosition;
                            });
                        }
                        be.ownerPosition = null;
                        be.parallelPositions = null;
                        be.electricBehaviour.breakConnections();
                        be.removeElectricBehaviour();
                    }
                    // Move collected segments
                    be.collectedBEs.forEach(other -> {
                        other.mainBE = this;
                        collectedBEs.add(other);
                    });
                    be.collectedBEs = null;
                    be.mainBE = this;
                    if(be.electricBehaviour != null) {
                        be.electricBehaviour.remove();
                        be.electricBehaviour = null;
                        be.removeBehaviour(ElectricBehaviour.TYPE);
                        // Drop nodes
                        be.coilWire = null;
                    }
                }
                if(!level.isClientSide || isVirtual()) {
                    // Check for parallel windings and housings
                    checkParallelPosition(pos1, Direction.get(Direction.AxisDirection.POSITIVE, parallelCheckAxis), false);
                    checkParallelPosition(pos1, Direction.get(Direction.AxisDirection.NEGATIVE, parallelCheckAxis), false);
                }
            });
        } else {
            var opt = block.getMainBlockEntity(level, worldPosition);
            if(opt.isEmpty()) {
                if(!level.isClientSide)
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
        assert level != null;
        if(mainBE == null || mainBE == this)
            return;
        collectedBEs = mainBE.collectedBEs;
        collectedBEs.remove(mainBE);
        collectedBEs.forEach(be -> be.mainBE = this);
        calculateElectricalParameters();
        if(!level.isClientSide || isVirtual())
            safeRebuildParallels();
    }


    private void addParallel(WindingBlockEntity otherMain) {
        assert level != null;
        assert isMain() : "Only main block entities can keep track of parallel windings";
        assert otherMain.isMain() : "Parallel block entities must be the main entities of their windings";
        assert !level.isClientSide || isVirtual() : "Parallel block entity collection can only occur on server";
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
            } else if(otherMain.ownerPosition != null && !otherMain.ownerPosition.equals(worldPosition)) {
                var be = level.getBlockEntity(otherMain.ownerPosition, ModdedBlockEntities.WINDING.get());
                // Merge owners
                be.ifPresent(this::addParallel);
            }
            otherMain.clearScheduledChange();
            otherMain.ownerPosition = worldPosition;
            otherMain.removeElectricBehaviour();
            // Synchronize to client
            otherMain.sendData();
            sendData();
        }
    }

    @Override
    public void initialize() {
        assert level != null;
        if(collectedBEs == null)
            collectWindingParts();
        if(parallelPositions != null) {
            // Everything is set up, and we haven't even initialized, make sure wires know where to go.
            parallelPositions.forEach(pos -> level.getBlockEntity(pos, ModdedBlockEntities.WINDING.get())
                    .ifPresent(WindingBlockEntity::rewire));
        }
        if(isMain() && ownerPosition == null)
            calculateElectricalParameters();
        super.initialize();
    }

    public int getCoilCount() {
        if(collectedBEs == null)
            collectWindingParts();
        return collectedBEs.size();
    }

    private void clearScheduledChange() {
        rebuildParallels = false;
        if(collectedBEs == null)
            return;
        for(var be : collectedBEs) {
            be.rebuildParallels = false;
        }
    }

    private void calculateElectricalParameters() {
        assert level != null;
        // This makes sure that ownership (which could change if collection method is called) is correct.
        totalCoilCount = getCoilCount();
        if(ownerPosition != null && !ownerPosition.equals(worldPosition)) {
            // If non-owner calls this method then its structure (and possibly resistance) has changed
            level.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get())
                    .ifPresent(WindingBlockEntity::calculateElectricalParameters);
            return;
        }
        resistance = totalCoilCount * resistance();
        if(parallelPositions != null) {
            var iter = parallelPositions.iterator();
            while (iter.hasNext()) {
                var windingPos = iter.next();
                var be = level.getBlockEntity(windingPos, ModdedBlockEntities.WINDING.get());
                if (be.isPresent()) {
                    var winding = be.get();
                    totalCoilCount += winding.getCoilCount();
                    resistance += winding.getCoilCount() * winding.resistance();
                } else {
                    iter.remove();
                }
            }
        }
        if(coilWire == null) {
            // We only need the nodes on the owner block entity
            addElectricBehaviour();
        } else {
            coilWire.setLR(resistance * 0.01f, resistance);
        }
        if(!level.isClientSide)
            sendData();
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(clientPacket) {
            ownerPosition = null;
            parallelPositions = null;
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
        var current = tag.getFloat("Current");
        if(coilWire != null) {
            coilWire.valueChange(current, coilWire.current(), 5);
            coilWire.setCurrent(current);
            fieldValue.setValue((float) fieldStrength(current));
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(coilWire != null) {
            tag.putFloat("Current", (float) coilWire.current());
        }
        if(clientPacket) {
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
            var R = Math.max(resistance, resistance());
            coilWire = new LRSeriesWire(R * 0.01f, R, builder.terminalNode(0), builder.terminalNode(1));
            builder.add(coilWire);
        }
    }

    private void rebuildParallels() {
        assert level != null;
        if(level.isClientSide && !isVirtual())
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
            checkParallelPosition(pos1, Direction.get(Direction.AxisDirection.POSITIVE, parallelCheckAxis), true);
            checkParallelPosition(pos1, Direction.get(Direction.AxisDirection.NEGATIVE, parallelCheckAxis), true);
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
                            var newCheckAxis = block.getParallelCheckAxis(state);
                            checkParallelPosition(pos1, Direction.get(Direction.AxisDirection.POSITIVE, newCheckAxis), true);
                            checkParallelPosition(pos1, Direction.get(Direction.AxisDirection.NEGATIVE, newCheckAxis), true);
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
        assert level != null;
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
        assert level != null;
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
        rebuildParallels = true;
    }

    private void safeRebuildParallels() {
        assert level != null;
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

    private WindingBlockEntity getSimElementHolder() {
        assert level != null;
        if(mainBE == null)
            return null;
        if(mainBE.ownerPosition != null) {
            if(mainBE.ownerPosition.equals(worldPosition)) {
                // A winding cannot be owned by itself.
                // This is an invalid state that can be caused if the client doesn't receive all data on time.
                return null;
            }
            var opt = level.getBlockEntity(mainBE.ownerPosition, ModdedBlockEntities.WINDING.get());
            if(opt.isPresent()) {
                var be = opt.get();
                return be.getSimElementHolder();
            }
        }
        if(mainBE.coilWire == null || mainBE.totalCoilCount == 0)
            return null;
        return mainBE;
    }

    public float windingCurrent() {
        var be = getSimElementHolder();
        if(be == null || !be.coilWire.isConverged())
            return 0;
        return (float) be.coilWire.current();
    }

    @Override
    public void remove() {
        assert level != null;
        // Always break connections when the winding is modified.
        if(mainBE != null) {
            for(var part : mainBE.wires()) {
                if(part.owner != null) {
                    part.owner.kill();
                } else {
                    part.remove();
                }
            }
            mainBE.electricBehaviour.breakConnections();
        }
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
        } else if(mainBE != null) {
            // Segment of a winding
            mainBE.collectedBEs.remove(this);
            mainBE.calculateElectricalParameters();
            mainBE.safeRebuildParallels();
        }
        super.remove();
    }

    @Override
    public void tick() {
        if(rebuildParallels) {
            safeRebuildParallels();
            rebuildParallels = false;
        }
        super.tick();
    }

    @Override
    public void electricalTick() {
        float current = windingCurrent();
        if (thermalBehaviour != null)
            thermalBehaviour.applyTickPower(current * current * resistance());
        setUnsaved();
    }

    @Override
    public void forSync(Consumer<ISynchronizedElement> consumer) {
        consumer.accept(electricBehaviour);
        if(collectedBEs != null) {
            for (var collected : collectedBEs) {
                if (collected == this)
                    continue;
                consumer.accept(collected.thermalBehaviour);
            }
        }
        if(parallelPositions != null) {
            for(var parallel : parallelPositions) {
                level.getBlockEntity(parallel, ModdedBlockEntities.WINDING.get()).ifPresent(winding -> {
                    if(winding.collectedBEs != null) {
                        for (var collected : winding.collectedBEs) {
                            if (collected == this)
                                continue;
                            consumer.accept(collected.thermalBehaviour);
                        }
                    }
                });
            }
        }
    }

    public void debugDump(Player user) {
        if(!isMain()) {
            user.sendSystemMessage(Component.literal("Non-main, main dump:"));
            mainBE.debugDump(user);
            return;
        } else {
            user.sendSystemMessage(Component.literal("Main").withStyle(ChatFormatting.BOLD));
        }
        if(ownerPosition != null) {
            user.sendSystemMessage(Component.literal("Owned winding").withStyle(ChatFormatting.GRAY));
            user.sendSystemMessage(Component.literal(" Owner: " + ownerPosition).withStyle(ChatFormatting.RED));
            var opt = level.getBlockEntity(ownerPosition, ModdedBlockEntities.WINDING.get());
            opt.ifPresentOrElse(be -> {
                user.sendSystemMessage(Component.literal("Owner dump:").withStyle(ChatFormatting.GRAY));
                be.debugDump(user);
            }, () -> {
                user.sendSystemMessage(Component.literal("Invalid or unloaded owner").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD));
            });
        }
        if(parallelPositions != null) {
            user.sendSystemMessage(Component.literal("Owner winding").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.BOLD));
            user.sendSystemMessage(Component.literal(" Owned windings (n = " + parallelPositions.size() + ")").withStyle(ChatFormatting.DARK_GRAY));
            for(var pos : parallelPositions) {
                user.sendSystemMessage(Component.literal(" - " + pos).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }
}
