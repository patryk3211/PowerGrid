package org.patryk3211.powergrid.electricity.gauge;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.block.entity.BlockEntityType;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class CurrentGaugeBlock extends GaugeBlock<CurrentGaugeBlockEntity> {
    float resistance;

    public CurrentGaugeBlock(Settings settings) {
        super(settings);
        resistance = 1;
    }

    public static <B extends CurrentGaugeBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> setResistance(float value) {
        return b -> b.onRegister(block -> block.resistance = value);
    }

    public float getResistance() {
        return resistance;
    }

    @Override
    public Class<CurrentGaugeBlockEntity> getBlockEntityClass() {
        return CurrentGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CurrentGaugeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.CURRENT_METER.get();
    }
}
