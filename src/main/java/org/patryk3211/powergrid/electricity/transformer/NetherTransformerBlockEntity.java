package org.patryk3211.powergrid.electricity.transformer;

import it.unimi.dsi.fastutil.objects.Object2ReferenceArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.special.SplitTransformerControllerWire;

import java.util.Map;
import java.util.UUID;

import static org.patryk3211.powergrid.electricity.sim.special.SplitTransformerControllerWire.AVG_SAMPLE_COUNT;

public class NetherTransformerBlockEntity extends ElectricBlockEntity {
    private static final Map<UUID, TransformerEntry> TRANSFORMERS = new Object2ReferenceArrayMap<>();

    private UUID id;
    private boolean secondary;

    private SplitTransformerControllerWire wire;

    public NetherTransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = new SplitTransformerControllerWire(builder.terminalNode(0), builder.terminalNode(1), 0.0075, 75);
        builder.add(wire);
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(id == null || clientPacket)
            return;
        tag.putUUID("Link", id);
        tag.putBoolean("Secondary", secondary);

        var avgData = new ListTag();
        for(int i = 0; i < AVG_SAMPLE_COUNT; ++i) {
            avgData.add(DoubleTag.valueOf(wire.samples[i]));
        }
        tag.put("TrAvgDat", avgData);
        tag.putInt("AvgHead", wire.avgHead);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(!tag.contains("Link") || clientPacket)
            return;
        id = tag.getUUID("Link");
        secondary = tag.getBoolean("Secondary");
        if(id != null) {
            var entry = TRANSFORMERS.computeIfAbsent(id, $ -> new TransformerEntry());
            entry.setWire(secondary, wire);
        }
        var avgData = tag.getList("TrAvgDat", CompoundTag.TAG_DOUBLE);
        if(avgData.size() == AVG_SAMPLE_COUNT) {
            for(int i = 0; i < AVG_SAMPLE_COUNT; ++i) {
                wire.samples[i] = avgData.getDouble(i * 2);
            }
            wire.avgHead = tag.getInt("AvgHead");
        }
    }

    @Override
    public void remove() {
        super.remove();
        var entry = TRANSFORMERS.get(id);
        if(entry == null)
            return;
        entry.setWire(secondary, null);
        if(entry.isEmpty())
            TRANSFORMERS.remove(id);
    }

    public void link(UUID id, boolean secondary) {
        this.id = id;
        this.secondary = secondary;
        var entry = TRANSFORMERS.computeIfAbsent(id, $ -> new TransformerEntry());
        entry.setWire(secondary, wire);
        setChanged();
    }

    private static class TransformerEntry {
        public SplitTransformerControllerWire wire1;
        public SplitTransformerControllerWire wire2;

        public void setWire1(@Nullable SplitTransformerControllerWire wire) {
            if(wire1 != null) {
                // Break link
                wire1.secondary = null;
                if(wire2 != null) {
                    // Break other side
                    wire2.secondary = null;
                }
            }
            wire1 = wire;
            if(wire1 == null)
                // Primary deleted
                return;
            // New wire is linked somewhere, delete the link
            if(wire1.secondary != null) {
                // Delete reference to this wire from the other side of the link
                wire1.secondary.secondary = null;
            }
            // Form link to secondary
            wire1.secondary = wire2;
            if(wire2 != null) {
                // Form link to primary
                wire2.secondary = wire1;
            }
        }

        public void setWire2(@Nullable SplitTransformerControllerWire wire) {
            if(wire2 != null) {
                // Break link
                wire2.secondary = null;
                if(wire1 != null) {
                    // Break other side
                    wire1.secondary = null;
                }
            }
            wire2 = wire;
            if(wire2 == null)
                // Primary deleted
                return;
            // New wire is linked somewhere, delete the link
            if(wire2.secondary != null) {
                // Delete reference to this wire from the other side of the link
                wire2.secondary.secondary = null;
            }
            // Form link to secondary
            wire2.secondary = wire1;
            if(wire1 != null) {
                // Form link to primary
                wire1.secondary = wire2;
            }
        }

        public void setWire(boolean secondary, @Nullable SplitTransformerControllerWire wire) {
            if(secondary) {
                setWire2(wire);
            } else {
                setWire1(wire);
            }
        }

        public boolean isEmpty() {
            return wire1 == null && wire2 == null;
        }
    }
}
