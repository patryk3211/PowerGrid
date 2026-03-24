package org.patryk3211.powergrid.electricity.light.factorylight;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;

public class FactoryLightBlock extends HorizontalAxisElectricBlock implements IAcceptCord, IAcceptConnector {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

    public FactoryLightBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx);
    }
}
