/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.portablebattery;

import com.simibubi.create.AllSoundEvents;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class PortableBatteryBlockEntity extends ElectricBlockEntity implements Nameable {
    private int charge;
    private int maxCharge;
    private int capacityLevel;
    private SwitchedWire wire;
    private Component name;

    private CompoundTag vanillaTag = new CompoundTag();

    public PortableBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCharge = ((PortableBatteryBlock) state.getBlock()).capacity();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void tick() {
        applyPower(wire);
        super.tick();
        if(wire.getState()) {
            if(!level.isClientSide) {
                int prevComparatorLevel = getComparatorOutput();
                var fePerTick = Math.abs(wire.potentialDifference()) * ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF();
                charge = (int) Math.min(charge + fePerTick, maxCharge);
                setChanged();
                if(getComparatorOutput() != prevComparatorLevel) {
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                    sendData();
                }
//                if(charge == maxCharge)
//                    sendData();
            }
        }
        wire.setState(charge < maxCharge);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connectSwitch(resistance(), builder.terminalNode(0), builder.terminalNode(1), charge < maxCharge);
    }

    private void playFilledEffect() {
        AllSoundEvents.CONFIRM.playAt(level, worldPosition, 0.4f, 1, true);
        var baseMotion = new Vec3(.25, 0.1, 0);
        var baseVec = worldPosition.getCenter();
        for(int i = 0; i < 360; i += 10) {
            var m = VecHelper.rotate(baseMotion, i, Direction.Axis.Y);
            m = m.offsetRandom(level.random, 0.2f);
            var v = baseVec.add(m.normalize().scale(.25f));
            level.addParticle(SparkParticleData.INSTANCE, v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        int prev = charge;
        charge = tag.getInt("Charge");
        capacityLevel = tag.getInt("CapacityLevel");
        maxCharge = BatteryUtils.getMaxCharge(capacityLevel);

        if(tag.contains("CustomName")) {
            name = Component.Serializer.fromJson(tag.getString("CustomName"));
        } else {
            name = null;
        }
        vanillaTag = tag.getCompound("VanillaTag");
        if(prev != 0 && prev != charge && charge == maxCharge && clientPacket)
            playFilledEffect();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("Charge", charge);
        tag.putInt("CapacityLevel", capacityLevel);
        if(name != null)
            tag.putString("CustomName", Component.Serializer.toJson(name));
        tag.put("VanillaTag", vanillaTag);
    }

    public void setCapacityEnchantLevel(int level) {
        capacityLevel = level;
        maxCharge = BatteryUtils.getMaxCharge(level);
    }

    public void setCharge(int charge) {
        this.charge = charge;
        if(!level.isClientSide)
            sendData();
    }

    @Override
    public Component getName() {
        return name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public void setTags(CompoundTag vanillaTag) {
        this.vanillaTag = vanillaTag;
    }

    public CompoundTag getVanillaTag() {
        return vanillaTag.copy();
    }

    public int getCharge() {
        return charge;
    }

    public int getComparatorOutput() {
        return (int) ((float) charge / maxCharge * 15);
    }
}
