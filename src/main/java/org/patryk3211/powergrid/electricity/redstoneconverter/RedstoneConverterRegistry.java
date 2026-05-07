package org.patryk3211.powergrid.electricity.redstoneconverter;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneConverterRegistry {
    public static final SimpleRegistry<Block, IRedstoneConverterBehaviour> REGISTRY = SimpleRegistry.create();

    public static void init() {
        REGISTRY.register(Blocks.CHEST, ContainerRedstoneConverterBehaviour.chest((ChestBlock) Blocks.CHEST));
        REGISTRY.register(Blocks.TRAPPED_CHEST, ContainerRedstoneConverterBehaviour.chest((ChestBlock) Blocks.TRAPPED_CHEST));
    }

    public static float get(Level level, BlockState state, BlockPos pos, Direction face) {
        var impl = REGISTRY.get(state.getBlock());
        if(impl != null)
            return impl.getSignal(level, state, pos, face);
        // Fallback handling.
        if(level.getBlockEntity(pos) instanceof Container) {
            impl = ContainerRedstoneConverterBehaviour.containerBE();
            REGISTRY.register(state.getBlock(), impl);
            return impl.getSignal(level, state, pos, face);
        }
        return DefaultRedstoneConverterBehaviour.INSTANCE.getSignal(level, state, pos, face);
    }
}
