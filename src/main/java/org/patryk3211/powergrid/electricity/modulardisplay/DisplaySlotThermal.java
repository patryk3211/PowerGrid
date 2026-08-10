package org.patryk3211.powergrid.electricity.modulardisplay;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;

import java.util.Collection;

import static org.patryk3211.powergrid.electricity.modulardisplay.ModularDisplayBlockEntity.SLOT_COUNT;

public class DisplaySlotThermal extends BlockEntityBehaviour {

    public static final BehaviourType<DisplaySlotThermal>[] TYPES;
    private final BehaviourType<DisplaySlotThermal> type;

    public static final float BASE_TEMPERATURE = 22.0f;
    public static final float OVERHEAT_TEMPERATURE = 175.0f;
    public static final float SMOKE_START_TEMPERATURE = OVERHEAT_TEMPERATURE - 50f;
    public static final int OVERHEAT_TICKS = 2;

    private final int slotIndex;
    private final Runnable burnoutCallback;

    private float temperature = BASE_TEMPERATURE;
    private int overheatTicks = 0;
    private float lastSyncedTemperature = BASE_TEMPERATURE;
    private final Collection<AbstractElectricWire> heatSources;

    private final float thermalMass;
    private final float dissipationFactor;

    static {
        TYPES = new BehaviourType[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            TYPES[i] = new BehaviourType<>("display_slot_thermal_" + i);
        }
    }

    public DisplaySlotThermal(SmartBlockEntity be, int slotIndex, float thermalMass,
                              float dissipationFactor, Runnable burnoutCallback, Collection<AbstractElectricWire> heatSources) {
        super(be);
        this.slotIndex = slotIndex;
        this.type = TYPES[slotIndex];
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.burnoutCallback = burnoutCallback;
        this.heatSources = heatSources;
    }


    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    private void temperatureChanged() {
        if(hasOverheated()) {
            if(burnoutCallback != null)
                burnoutCallback.run();
            resetTemperature();
        }
    }

    public boolean hasOverheated() {
        return temperature >= OVERHEAT_TEMPERATURE && overheatTicks >= OVERHEAT_TICKS;
    }

    @Override
    public void tick() {
        if (hasOverheated())
            return;

        if (!getWorld().isClientSide) {
            if (blockEntity instanceof ModularDisplayBlockEntity be) {
                SlotData slot = be.getSlot(slotIndex);
                if (slot.isEmpty()) return;
            } else return;
            float power = -dissipationFactor * (temperature - BASE_TEMPERATURE);
            for (var source : heatSources) {
                if (!source.isConverged())
                    return;
                power += source.power();
            }
            temperature += power / 20f / thermalMass;
            if (!Float.isFinite(temperature))
                temperature = BASE_TEMPERATURE;
            if (temperature > OVERHEAT_TEMPERATURE + 10)
                temperature = OVERHEAT_TEMPERATURE + 10;
            if (power < 0 && temperature < 22f)
                temperature = 22f;
            if (temperature >= OVERHEAT_TEMPERATURE && power > 0) {
                ++overheatTicks;
            } else if (power < 0) {
                overheatTicks = 0;
            }
            temperatureChanged();

            boolean crossedSmokeThreshold =
                    (lastSyncedTemperature >= SMOKE_START_TEMPERATURE) != (temperature >= SMOKE_START_TEMPERATURE);

            //todo This might be better done using a SyncAppender however those are done similar to lightbulbs
            if (crossedSmokeThreshold || Math.abs(temperature - lastSyncedTemperature) > 1f) {
                lastSyncedTemperature = temperature;
                blockEntity.sendData();
            }
        }

        if (temperature >= SMOKE_START_TEMPERATURE) {
            var world = getWorld();
            var pos = getPos();
            spawnSmokeParticles(world, pos, world.getRandom());
        }
    }

    private void spawnSmokeParticles(Level world, BlockPos pos, RandomSource random) {
        int col = slotIndex % 2;
        int row = slotIndex / 2;

        float cellX = (col * 8 + 4) / 16f;
        float cellY = ((1 - row) * 8 + 4) / 16f;

        float chance = (temperature - SMOKE_START_TEMPERATURE) / 50f;
        if (random.nextFloat() > chance) return;

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(ModularDisplayBlock.HORIZONTAL_FACING);

        float x, y, z;
        y = pos.getY() + cellY + random.nextFloat() * 0.1f;
        float offset = 2/16f;

        switch (facing) {
            case NORTH -> {
                x = pos.getX() + cellX;
                z = pos.getZ() - offset + random.nextFloat() * 0.05f;
            }
            case SOUTH -> {
                x = pos.getX() + (1 - cellX);
                z = pos.getZ() + offset + 1 - random.nextFloat() * 0.05f;
            }
            case WEST -> {
                x = pos.getX() - offset + random.nextFloat() * 0.05f;
                z = pos.getZ() + (1 - cellX);
            }
            case EAST -> {
                x = pos.getX() + offset + 1 - random.nextFloat() * 0.05f;
                z = pos.getZ() + cellX;
            }
            default -> {
                x = pos.getX() + 0.5f;
                z = pos.getZ() + 0.5f;
            }
        }

        world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
    }

    public boolean isOverheated() {
        return temperature >= OVERHEAT_TEMPERATURE;
    }

    public float getTemperature() {
        return temperature;
    }

    public void resetTemperature() {
        temperature = BASE_TEMPERATURE;
        overheatTicks = 0;
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        nbt.putFloat("SlotTemp_" + slotIndex, temperature);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.contains("SlotTemp_" + slotIndex)) {
            temperature = nbt.getFloat("SlotTemp_" + slotIndex);
        }
    }
}