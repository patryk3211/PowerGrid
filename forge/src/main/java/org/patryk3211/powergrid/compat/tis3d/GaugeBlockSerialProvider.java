package org.patryk3211.powergrid.compat.tis3d;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.util.entry.BlockEntry;
import li.cil.tis3d.api.serial.SerialInterface;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.gauge.GaugeBlockEntity;
import org.patryk3211.powergrid.electricity.gauge.IGaugeBlock;

import java.util.Optional;

public class GaugeBlockSerialProvider<T extends Block & IBE<BE> & IGaugeBlock, BE extends GaugeBlockEntity> implements SerialInterfaceProvider {
    private final SerialProtocolDocumentationReference docs;
    private final T block;

    private GaugeBlockSerialProvider(T block) {
        this.block = block;
        var key = BuiltInRegistries.BLOCK.getKey(block);
        docs = new SerialProtocolDocumentationReference(
                Component.translatable("block.powergrid." + key.getPath()),
                key.getNamespace() + "_" + key.getPath() + ".md"
        );
    }

    @Override
    public boolean matches(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
        return level.getBlockState(blockPos).is(block);
    }

    @Override
    public @NotNull Optional<SerialInterface> getInterface(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
        if (!level.getBlockState(blockPos).is(block)) return Optional.empty();
        var be = level.getBlockEntity(blockPos);
        if (!(be instanceof GaugeBlockEntity)) return Optional.empty();
        BE actualBE = (BE) be;
        return Optional.of(new GaugeBlockSerialInterface<>(actualBE));
    }

    @Override
    public @NotNull Optional<SerialProtocolDocumentationReference> getDocumentationReference() {
        return Optional.of(docs);
    }

    @Override
    public boolean stillValid(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction, @NotNull SerialInterface serialInterface) {
        return serialInterface instanceof GaugeBlockSerialProvider t && t.block == block;
    }

    public static <T extends Block & IBE<BE> & IGaugeBlock, BE extends GaugeBlockEntity> GaugeBlockSerialProvider<T, BE> of(BlockEntry<T> meter) {
        return new GaugeBlockSerialProvider<>(meter.get());
    }

    private static class GaugeBlockSerialInterface<BE extends GaugeBlockEntity> implements SerialInterface {
        private final BE blockEntity;
        private short base10;

        private GaugeBlockSerialInterface(BE blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean canWrite() {
            return true;
        }

        @Override
        public void write(short i) {
            base10 = i;
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public short peek() {
            float reading = Math.abs(blockEntity.getValue());
            float multiplier = (float) (1 / Math.pow(10.0, base10));
            return (short) (reading * multiplier);
        }

        @Override
        public void skip() {}

        @Override
        public void reset() {
            base10 = 0;
        }

        @Override
        public void save(@NotNull CompoundTag compoundTag) {
            compoundTag.putShort("base10", base10);
        }

        @Override
        public void load(@NotNull CompoundTag compoundTag) {
            base10 = compoundTag.getShort("base10");
        }
    }
}
