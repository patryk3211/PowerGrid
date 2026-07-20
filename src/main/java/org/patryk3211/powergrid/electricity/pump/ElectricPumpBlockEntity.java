package org.patryk3211.powergrid.electricity.pump;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;

import javax.annotation.Nullable;
import java.util.*;

import static org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock.FACING;
import static org.patryk3211.powergrid.electricity.pump.ElectricPumpBlock.isPump;

/**
 * @see com.simibubi.create.content.fluids.pump.PumpBlockEntity
 */
public class ElectricPumpBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    protected ElectricWire pumpElement;
    private int prevSpeed;

    Couple<MutableBoolean> sidesToUpdate = Couple.create(MutableBoolean::new);
    boolean pressureUpdate;
    boolean scheduleFlip;

    public ElectricPumpBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new PumpFluidTransferBehaviour(this));
        this.registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        float power = 256 / ModdedConfigs.server().electricity.electricPumpPower.getF();
        return ThermalBehaviour.forMaxPower(this, 1.0f, power * 1.2f);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        pumpElement = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }

    public void tick() {
        super.tick();
        if(!level.isClientSide || isVirtual()) {
            if(scheduleFlip) {
                level.setBlockAndUpdate(worldPosition, getBlockState()
                        .setValue(FACING, getBlockState().getValue(FACING).getOpposite()));
                scheduleFlip = false;
            }

            sidesToUpdate.forEachWithContext((update, isFront) -> {
                if(!update.isFalse()) {
                    update.setFalse();
                    distributePressureTo(isFront ? getFront() : getFront().getOpposite());
                }
            });

            if(prevSpeed != getSpeed()) {
                prevSpeed = getSpeed();
                updatePressureChange();
            }
        }
    }

    @Override
    public void electricalTick() {
        applyPower(pumpElement);
    }

    public static int getRange() {
        return ModdedConfigs.server().electricity.electricPumpRange.get();
    }

    public int getSpeed() {
        return (int) Math.min(pumpElement.power() * ModdedConfigs.server().electricity.electricPumpPower.get(), ModdedConfigs.server().electricity.electricPumpMaxSpeed.getF());
    }

    public void updatePressureChange() {
        this.pressureUpdate = false;
        BlockPos frontPos = worldPosition.relative(getFront());
        BlockPos backPos = worldPosition.relative(getFront().getOpposite());
        FluidPropagator.propagateChangedPipe(level, frontPos, level.getBlockState(frontPos));
        FluidPropagator.propagateChangedPipe(level, backPos, level.getBlockState(backPos));
        FluidTransportBehaviour behaviour = getBehaviour(FluidTransportBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.wipePressure();
        }

        this.sidesToUpdate.forEach(MutableBoolean::setTrue);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if(tag.getBoolean("Reversed")) {
            this.scheduleFlip = true;
        }
    }

    protected void distributePressureTo(Direction side) {
        if(getSpeed() != 0) {
            BlockFace start = new BlockFace(worldPosition, side);
            boolean pull = isPullingOnSide(isFront(side));
            Set<BlockFace> targets = new HashSet<>();
            Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph = new HashMap<>();
            if(!pull) {
                FluidPropagator.resetAffectedFluidNetworks(level, worldPosition, side.getOpposite());
            }

            if (!hasReachedValidEndpoint(level, start, pull)) {
                pipeGraph.computeIfAbsent(worldPosition, $ -> Pair.of(0, new IdentityHashMap<>())).getSecond().put(side, pull);
                pipeGraph.computeIfAbsent(start.getConnectedPos(), $ -> Pair.of(1, new IdentityHashMap<>())).getSecond().put(side.getOpposite(), !pull);
                List<Pair<Integer, BlockPos>> frontier = new ArrayList<>();
                Set<BlockPos> visited = new HashSet<>();
                int maxDistance = getRange();
                frontier.add(Pair.of(1, start.getConnectedPos()));

                while(!frontier.isEmpty()) {
                    Pair<Integer, BlockPos> entry = frontier.remove(0);
                    int distance = entry.getFirst();
                    BlockPos currentPos = entry.getSecond();
                    if (level.isLoaded(currentPos) && !visited.contains(currentPos)) {
                        visited.add(currentPos);
                        BlockState currentState = level.getBlockState(currentPos);
                        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, currentPos);
                        if (pipe != null) {
                            for(Direction face : FluidPropagator.getPipeConnections(currentState, pipe)) {
                                BlockFace blockFace = new BlockFace(currentPos, face);
                                BlockPos connectedPos = blockFace.getConnectedPos();
                                if (level.isLoaded(connectedPos) && !blockFace.isEquivalent(start)) {
                                    if (hasReachedValidEndpoint(level, blockFace, pull)) {
                                        pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>())).getSecond().put(face, pull);
                                        targets.add(blockFace);
                                    } else {
                                        FluidTransportBehaviour pipeBehaviour = FluidPropagator.getPipe(level, connectedPos);
                                        if (pipeBehaviour != null && !(pipeBehaviour instanceof PumpFluidTransferBehaviour) && !visited.contains(connectedPos)) {
                                            if (distance + 1 >= maxDistance) {
                                                pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>())).getSecond().put(face, pull);
                                                targets.add(blockFace);
                                            } else {
                                                pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap<>())).getSecond().put(face, pull);
                                                pipeGraph.computeIfAbsent(connectedPos, $ -> Pair.of(distance + 1, new IdentityHashMap<>())).getSecond().put(face.getOpposite(), !pull);
                                                frontier.add(Pair.of(distance + 1, connectedPos));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Map<Integer, Set<BlockFace>> validFaces = new HashMap<>();
            searchForEndpointRecursively(pipeGraph, targets, validFaces, new BlockFace(start.getPos(), start.getOppositeFace()), pull);
            float pressure = Math.abs(getSpeed());

            for(Set<BlockFace> set : validFaces.values()) {
                int parallelBranches = Math.max(1, set.size() - 1);
                for(BlockFace face : set) {
                    BlockPos pipePos = face.getPos();
                    Direction pipeSide = face.getFace();
                    if (!pipePos.equals(worldPosition)) {
                        boolean inbound = pipeGraph.get(pipePos).getSecond().get(pipeSide);
                        FluidTransportBehaviour pipeBehaviour = FluidPropagator.getPipe(level, pipePos);
                        if (pipeBehaviour != null) {
                            pipeBehaviour.addPressure(pipeSide, inbound, pressure / (float) parallelBranches);
                        }
                    }
                }
            }
        }
    }

    protected boolean searchForEndpointRecursively(Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph, Set<BlockFace> targets, Map<Integer, Set<BlockFace>> validFaces, BlockFace currentFace, boolean pull) {
        BlockPos currentPos = currentFace.getPos();
        if (!pipeGraph.containsKey(currentPos)) {
            return false;
        } else {
            Pair<Integer, Map<Direction, Boolean>> pair = pipeGraph.get(currentPos);
            int distance = pair.getFirst();
            boolean atLeastOneBranchSuccessful = false;

            for(Direction nextFacing : Iterate.directions) {
                if (nextFacing != currentFace.getFace()) {
                    Map<Direction, Boolean> map = pair.getSecond();
                    if (map.containsKey(nextFacing)) {
                        BlockFace localTarget = new BlockFace(currentPos, nextFacing);
                        if (targets.contains(localTarget)) {
                            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(localTarget);
                            atLeastOneBranchSuccessful = true;
                        } else if (map.get(nextFacing) == pull && this.searchForEndpointRecursively(pipeGraph, targets, validFaces, new BlockFace(currentPos.relative(nextFacing), nextFacing.getOpposite()), pull)) {
                            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(localTarget);
                            atLeastOneBranchSuccessful = true;
                        }
                    }
                }
            }

            if (atLeastOneBranchSuccessful) {
                validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(currentFace);
            }

            return atLeastOneBranchSuccessful;
        }
    }

    private boolean hasReachedValidEndpoint(LevelAccessor world, BlockFace blockFace, boolean pull) {
        BlockPos connectedPos = blockFace.getConnectedPos();
        BlockState connectedState = world.getBlockState(connectedPos);
        BlockEntity blockEntity = world.getBlockEntity(connectedPos);
        Direction face = blockFace.getFace();
        if(isPump(connectedState) && connectedState.getValue(FACING).getAxis() == face.getAxis() && blockEntity instanceof ElectricPumpBlockEntity pumpBE) {
            return pumpBE.isPullingOnSide(pumpBE.isFront(blockFace.getOppositeFace())) != pull;
        } else {
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(world, connectedPos);
            if(pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace())) {
                return false;
            } else {
                if(blockEntity != null) {
                    if(platformEndpointCheck(blockEntity, face))
                        return true;
                }

                return FluidPropagator.isOpenEnd(world, blockFace.getPos(), face);
            }
        }
    }

    @ExpectPlatform
    public static boolean platformEndpointCheck(BlockEntity be, Direction face) {
        throw new AssertionError();
    }

    public void updatePipesOnSide(Direction side) {
        if(isSideAccessible(side)) {
            updatePipeNetwork(isFront(side));
            getBehaviour(FluidTransportBehaviour.TYPE).wipePressure();
        }
    }

    protected boolean isFront(Direction side) {
        BlockState blockState = this.getBlockState();
        if(!(blockState.getBlock() instanceof ElectricPumpBlock)) {
            return false;
        } else {
            Direction front = blockState.getValue(FACING);
            boolean isFront = side == front;
            return isFront;
        }
    }

    @Nullable
    protected Direction getFront() {
        BlockState blockState = this.getBlockState();
        return !(blockState.getBlock() instanceof ElectricPumpBlock) ? null : blockState.getValue(FACING);
    }

    protected void updatePipeNetwork(boolean front) {
        this.sidesToUpdate.get(front).setTrue();
    }

    public boolean isSideAccessible(Direction side) {
        BlockState blockState = this.getBlockState();
        if(!(blockState.getBlock() instanceof ElectricPumpBlock)) {
            return false;
        } else {
            return blockState.getValue(FACING).getAxis() == side.getAxis();
        }
    }

    public boolean isPullingOnSide(boolean front) {
        return !front;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.pump.info_header")
                .forGoggles(tooltip);
        Lang.translate("gui.pump.speed_header")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        Lang.number(getSpeed())
                .style(ChatFormatting.AQUA)
                .add(Lang.text(" "))
                .add(CreateLang.translate("generic.unit.rpm"))
                .forGoggles(tooltip, 1);
        return true;
    }

    class PumpFluidTransferBehaviour extends FluidTransportBehaviour {
        public PumpFluidTransferBehaviour(SmartBlockEntity be) {
            super(be);
        }

        public void tick() {
            super.tick();

            for(Map.Entry<Direction, PipeConnection> entry : this.interfaces.entrySet()) {
                boolean pull = isPullingOnSide(isFront(entry.getKey()));
                Couple<Float> pressure = entry.getValue().getPressure();
                pressure.set(pull, (float) Math.abs(getSpeed()));
                pressure.set(!pull, 0.0F);
            }

        }

        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return isSideAccessible(direction);
        }

        public FluidTransportBehaviour.AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            FluidTransportBehaviour.AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
            return attachment == AttachmentTypes.RIM ? AttachmentTypes.NONE : attachment;
        }
    }
}
