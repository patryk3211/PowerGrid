package org.patryk3211.powergrid.electricity.modulardisplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

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
    private float prevTemperature = BASE_TEMPERATURE;
    private int overheatTicks = 0;
    private boolean firstTick = true;
    private float lastSyncedTemperature = BASE_TEMPERATURE;

    private final float thermalMass;
    private final float dissipationFactor;

    static {
        TYPES = new BehaviourType[16];
        for (int i = 0; i < 16; i++) {
            TYPES[i] = new BehaviourType<>("display_slot_thermal_" + i);
        }
    }

    public DisplaySlotThermal(SmartBlockEntity be, int slotIndex, float thermalMass,
                              float dissipationFactor, Runnable burnoutCallback) {
        super(be);
        this.slotIndex = slotIndex;
        this.type = TYPES[slotIndex];
        this.thermalMass = thermalMass;
        this.dissipationFactor = dissipationFactor;
        this.burnoutCallback = burnoutCallback;
    }


    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public void tick() {
        super.tick();

        if (firstTick) {
            firstTick = false;
            return;
        }

        var world = getWorld();
        var pos = getPos();

        if (!world.isClientSide) {

            float dissipatedPower = dissipationFactor * (temperature - BASE_TEMPERATURE);
            temperature -= dissipatedPower / 20f / thermalMass;

            if (dissipatedPower > 0 && temperature < BASE_TEMPERATURE)
                temperature = BASE_TEMPERATURE;

            if (!Float.isFinite(temperature)) {
                temperature = BASE_TEMPERATURE;
                prevTemperature = BASE_TEMPERATURE;
            }

            float temperatureDelta = temperature - prevTemperature;
            prevTemperature = temperature;

            if (isOverheated()) {
                if (temperatureDelta > 0 && overheatTicks++ >= OVERHEAT_TICKS) {
                    if (burnoutCallback != null)
                        burnoutCallback.run();
                    resetTemperature();
                } else if (temperatureDelta <= 0) {
                    overheatTicks = 0;
                    if (temperature > OVERHEAT_TEMPERATURE + 10)
                        temperature = OVERHEAT_TEMPERATURE + 10;
                }
            }

            boolean crossedSmokeThreshold =
                    (lastSyncedTemperature >= SMOKE_START_TEMPERATURE) != (temperature >= SMOKE_START_TEMPERATURE);

            //todo This might be better done using a SyncAppender however those are done similar to lightbulbs
            if (crossedSmokeThreshold || Math.abs(temperature - lastSyncedTemperature) > 1f) {
                lastSyncedTemperature = temperature;
                blockEntity.sendData();
            }

        } else {
            if (temperature >= SMOKE_START_TEMPERATURE) {
                spawnSmokeParticles(world, pos, world.getRandom());
            }
        }
    }

    private void spawnSmokeParticles(Level world, BlockPos pos, RandomSource random) {
        int col = slotIndex % 4;
        int row = slotIndex / 4;

        float cellX = (col * 4 + 2) / 16f;
        float cellY = ((3 - row) * 4 + 2) / 16f;

        float chance = (temperature - SMOKE_START_TEMPERATURE) / 50f;
        if (random.nextFloat() > chance) return;

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(ModularDisplayBlock.HORIZONTAL_FACING);

        float x, y, z;
        y = pos.getY() + cellY + random.nextFloat() * 0.1f;

        switch (facing) {
            case NORTH -> {
                x = pos.getX() + cellX;
                z = pos.getZ() + random.nextFloat() * 0.05f;
            }
            case SOUTH -> {
                x = pos.getX() + (1 - cellX);
                z = pos.getZ() + 1 - random.nextFloat() * 0.05f;
            }
            case WEST -> {
                x = pos.getX() + random.nextFloat() * 0.05f;
                z = pos.getZ() + (1 - cellX);
            }
            case EAST -> {
                x = pos.getX() + 1 - random.nextFloat() * 0.05f;
                z = pos.getZ() + cellX;
            }
            default -> {
                x = pos.getX() + 0.5f;
                z = pos.getZ() + 0.5f;
            }
        }

        world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
    }

    public void applyTickPower(double power) {
        if (Double.isFinite(power)) {
            float energy = (float)(power / 20f);
            temperature += energy / thermalMass;
        }
    }

    public void applyWirePower(@Nullable AbstractElectricWire wire) {
        if (wire == null) return;
        if (wire.isConverged())
            applyTickPower(wire.power());
    }

    public boolean isOverheated() {
        return temperature >= OVERHEAT_TEMPERATURE;
    }

    public float getTemperature() {
        return temperature;
    }

    public void resetTemperature() {
        temperature = BASE_TEMPERATURE;
        prevTemperature = BASE_TEMPERATURE;
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
            prevTemperature = temperature;
        }
    }
}