package org.patryk3211.powergrid.electricity.light.factorylight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IMultipartSync;
import org.patryk3211.powergrid.electricity.base.ISynchronizedElement;
import org.patryk3211.powergrid.electricity.base.ProxyElectricBehaviour;
import org.patryk3211.powergrid.electricity.light.fixture.AbstractLightFixtureBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;

import java.util.function.Consumer;

import static net.minecraft.world.level.block.Block.UPDATE_ALL;
import static net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE;
import static org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock.HORIZONTAL_AXIS;
import static org.patryk3211.powergrid.electricity.light.factorylight.FactoryLightBlock.PART;

public class FactoryLightBlockEntity extends AbstractLightFixtureBlockEntity implements IMultipartSync {
    private final SharedFilamentWire filament = new SharedFilamentWire();
    private TopLevelSharedFilamentWire masterFilament;
    private BlockPos effectsPos;

    public FactoryLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, false);
        this.effectsPos = pos;
        setLazyTickRate(20);
    }

    public static int projectionDistance() {
        return ModdedConfigs.server().electricity.factoryLightProjectionRange.get();
    }

    private BlockPos getController() {
        BlockState state;
        var pos = worldPosition;
        var axis = getBlockState().getValue(HORIZONTAL_AXIS);
        do {
            pos = pos.relative(axis, -1);
            if(!level.isLoaded(pos))
                return null;
            state = level.getBlockState(pos);
        } while(state.is(getBlockState().getBlock()) && !FactoryLightBlock.isNegativeEdge(state.getValue(PART)));
        if(state.is(getBlockState().getBlock()))
            return pos;
        return null;
    }

    @Override
    public ElectricBehaviour specifyElectricBehaviour() {
        int part = getBlockState().getValue(PART);
        if(part != 0 && part != 1)
            return new ProxyElectricBehaviour(this, this::getController);
        return super.specifyElectricBehaviour();
    }

    private void spreadFilament() {
        // Trace and spread the master filament everywhere
        var axis = getBlockState().getValue(HORIZONTAL_AXIS);
        BlockState state;
        BlockPos pos = worldPosition;
        do {
            pos = pos.relative(axis, 1);
            if(!level.isLoaded(pos))
                break;
            state = level.getBlockState(pos);
            var be = level.getBlockEntity(pos);
            if(!(be instanceof FactoryLightBlockEntity fbe))
                continue;
            fbe.filament.setSimWire(masterFilament);
        } while(state.is(getBlockState().getBlock()) && !FactoryLightBlock.isPositiveEdge(state.getValue(PART)));
    }

    private void grabFilament() {
        // Trace and find controller
        var axis = getBlockState().getValue(HORIZONTAL_AXIS);
        BlockState state;
        BlockPos pos = worldPosition;
        do {
            pos = pos.relative(axis, -1);
            if(!level.isLoaded(pos))
                break;
            state = level.getBlockState(pos);
            if(state.is(getBlockState().getBlock())) {
                var otherPart = state.getValue(PART);
                if(FactoryLightBlock.isNegativeEdge(otherPart)) {
                    var be = level.getBlockEntity(pos);
                    if(!(be instanceof FactoryLightBlockEntity fbe))
                        continue;
                    filament.setSimWire(fbe.masterFilament);
                    break;
                }
            }
        } while(state.is(getBlockState().getBlock()));
    }

    @Override
    public void tick() {
        if(filament.getSimWire() == null) {
            int part = getBlockState().getValue(PART);
            boolean shouldBeController = part == 0 || FactoryLightBlock.isNegativeEdge(part);
            if (shouldBeController) {
                filament.setSimWire(masterFilament);
                if (FactoryLightBlock.isNegativeEdge(part)) {
                    spreadFilament();
                }
            } else {
                grabFilament();
            }
        }
        super.tick();
        int part = getBlockState().getValue(PART);
        boolean shouldBeController = part == 0 || FactoryLightBlock.isNegativeEdge(part);
        boolean isController = !(electricBehaviour instanceof ProxyElectricBehaviour);
        if(shouldBeController != isController) {
            var wires = electricBehaviour.wires();
            var old = electricBehaviour;
            if(shouldBeController) {
                electricBehaviour = new ElectricBehaviour(this);
            } else {
                old.pause();
                electricBehaviour = new ProxyElectricBehaviour(this, this::getController);
            }
            electricBehaviour.setSyncAppender(bulbState);
            electricBehaviour.inheritConnections(old);
            removeBehaviour(ElectricBehaviour.TYPE);
            attachBehaviourLate(electricBehaviour);

            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
            old.remove();
            filament.setSimWire(masterFilament);
            if(FactoryLightBlock.isNegativeEdge(part)) {
                spreadFilament();
            }
        }
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        if(bulbState != null) {
            bulbState.runSpecialEffects(level, effectsPos, null);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        masterFilament = new TopLevelSharedFilamentWire(builder.terminalNode(0), builder.terminalNode(1));
        builder.add(masterFilament);
    }

    @Override
    protected void lightBulbChanged() {
        super.lightBulbChanged();
        if(level != null && !level.isClientSide) {
            if(bulbState == null) {
                level.setBlock(worldPosition, getBlockState().setValue(FactoryLightBlock.POWER, 0), UPDATE_ALL_IMMEDIATE);
            } else {
                level.setBlock(worldPosition, getBlockState().setValue(FactoryLightBlock.POWER, 1), UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        projectDown();
    }

    @Override
    public SwitchedWire getFilament() {
        return filament;
    }

    @Override
    public int getPowerLevel() {
        return Math.max(getBlockState().getValue(FactoryLightBlock.POWER) - 1, 0);
    }

    @Override
    public void setPowerLevel(int bulbPower) {
        assert level != null;
        if(bulbState != null) {
            level.setBlock(worldPosition, getBlockState().setValue(FactoryLightBlock.POWER, bulbPower + 1), UPDATE_ALL_IMMEDIATE);
        } else {
            level.setBlock(worldPosition, getBlockState().setValue(FactoryLightBlock.POWER, 0), UPDATE_ALL_IMMEDIATE);
        }
        projectDown();
    }

    private void projectDown() {
        assert level != null;
        var bulbPower = getPowerLevel();
        effectsPos = worldPosition;
        if(bulbPower > 0) {
            for(int i = 1; i < projectionDistance(); ++i) {
                var pos = worldPosition.below(i);
                var state = level.getBlockState(pos);
                if(state.getBlock() instanceof FactoryLightLightBlock) {
                    level.setBlock(pos, state.setValue(FactoryLightLightBlock.POWER, bulbPower - 1), UPDATE_ALL_IMMEDIATE);
                } else if(state.isAir()) {
                    level.setBlock(pos, ModdedBlocks.FACTORY_LIGHT_LIGHT.getDefaultState()
                            .setValue(FactoryLightLightBlock.POWER, bulbPower - 1), UPDATE_ALL);
                } else {
                    break;
                }
                effectsPos = pos;
            }
        } else {
            for(int i = 1; i < projectionDistance(); ++i) {
                var pos = worldPosition.below(i);
                var state = level.getBlockState(pos);
                if(state.getBlock() instanceof FactoryLightLightBlock) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL_IMMEDIATE);
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public void forSync(Consumer<ISynchronizedElement> consumer) {
        consumer.accept(electricBehaviour);
        var axis = getBlockState().getValue(HORIZONTAL_AXIS);
        BlockState state;
        BlockPos pos = worldPosition;
        do {
            pos = pos.relative(axis, 1);
            if(!level.isLoaded(pos))
                break;
            state = level.getBlockState(pos);
            var be = level.getBlockEntity(pos);
            if(!(be instanceof FactoryLightBlockEntity fbe))
                continue;
            consumer.accept(fbe.electricBehaviour);
        } while(state.is(getBlockState().getBlock()) && !FactoryLightBlock.isPositiveEdge(state.getValue(PART)));
    }
}
