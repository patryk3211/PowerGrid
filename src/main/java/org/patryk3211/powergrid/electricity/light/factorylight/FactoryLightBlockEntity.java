package org.patryk3211.powergrid.electricity.light.factorylight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ProxyElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;

import static org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock.HORIZONTAL_AXIS;
import static org.patryk3211.powergrid.electricity.light.factorylight.FactoryLightBlock.PART;

public class FactoryLightBlockEntity extends ElectricBlockEntity {
    private SwitchedWire filament;

    public FactoryLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        } while(state.is(getBlockState().getBlock()) && state.getValue(PART) != 1);
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

    @Override
    public void tick() {
        super.tick();
        int part = getBlockState().getValue(PART);
        boolean shouldBeController = part == 0 || part == 1;
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
            electricBehaviour.inheritConnections(old);
            removeBehaviour(ElectricBehaviour.TYPE);
            attachBehaviourLate(electricBehaviour);

            wires.forEach(TransmissionLinePart::refreshEndpointNodes);
            old.remove();
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        filament = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1), false);
    }
}
