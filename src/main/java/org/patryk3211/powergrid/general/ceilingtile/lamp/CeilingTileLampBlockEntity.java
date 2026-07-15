package org.patryk3211.powergrid.general.ceilingtile.lamp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.light.fixture.AbstractLightFixtureBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.general.ceilingtile.CeilingTileBlock;

import static org.patryk3211.powergrid.general.ceilingtile.lamp.CeilingTileLampBlock.STATE;

public class CeilingTileLampBlockEntity extends AbstractLightFixtureBlockEntity {
    private SwitchedWire filament;

    public CeilingTileLampBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, false);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        filament = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1), false);
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        if(bulbState != null)
            bulbState.runSpecialEffects(level, worldPosition, Direction.DOWN);
    }

    @Override
    public SwitchedWire getFilament() {
        return filament;
    }

    @Override
    public void setPowerLevel(int bulbPower) {
        var state = switch(bulbPower) {
            case 0 -> bulbState == null ? CeilingTileLampBlock.State.EMPTY : CeilingTileLampBlock.State.LAMP;
            case 1 -> CeilingTileLampBlock.State.LAMP_LOW_POWER;
            case 2 -> CeilingTileLampBlock.State.LAMP_ON;
            default -> CeilingTileLampBlock.State.LAMP;
        };
        level.setBlock(worldPosition, getBlockState().setValue(STATE, state), CeilingTileBlock.UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public int getPowerLevel() {
        var state = getBlockState().getValue(STATE);
        return switch(state) {
            case LAMP_LOW_POWER -> 1;
            case LAMP_ON -> 2;
            default -> 0;
        };
    }
}
