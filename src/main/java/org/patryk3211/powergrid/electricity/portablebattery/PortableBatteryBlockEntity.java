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
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
    private Text name;

    private NbtCompound vanillaTag = new NbtCompound();

    public PortableBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCharge = ((PortableBatteryBlock) state.getBlock()).capacity();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.forMaxPower(this, 1.0f, 100.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if(wire.getState()) {
            if(!world.isClient) {
                int prevComparatorLevel = getComparatorOutput();
                var fePerTick = Math.abs(wire.potentialDifference()) * ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF();
                charge = (int) Math.min(charge + fePerTick, maxCharge);
                markDirty();
                if(getComparatorOutput() != prevComparatorLevel) {
                    world.updateComparators(pos, getCachedState().getBlock());
                    sendData();
                }
//                if(charge == maxCharge)
//                    sendData();
            }
            applyLostPower(wire.power());
        }
        wire.setState(charge < maxCharge);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connectSwitch(PortableBatteryBlock.resistance(), builder.terminalNode(0), builder.terminalNode(1), charge < maxCharge);
    }

    private void playFilledEffect() {
        AllSoundEvents.CONFIRM.playAt(world, pos, 0.4f, 1, true);
        var baseMotion = new Vec3d(.25, 0.1, 0);
        var baseVec = pos.toCenterPos();
        for(int i = 0; i < 360; i += 10) {
            var m = VecHelper.rotate(baseMotion, i, Direction.Axis.Y);
            m = m.addRandom(world.random, 0.2f);
            var v = baseVec.add(m.normalize().multiply(.25f));
            world.addParticle(SparkParticleData.INSTANCE, v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        int prev = charge;
        charge = tag.getInt("Charge");
        capacityLevel = tag.getInt("CapacityLevel");
        maxCharge = BatteryUtils.getMaxCharge(capacityLevel);

        if(tag.contains("CustomName")) {
            name = Text.Serializer.fromJson(tag.getString("CustomName"));
        } else {
            name = null;
        }
        vanillaTag = tag.getCompound("VanillaTag");
        if(prev != 0 && prev != charge && charge == maxCharge && clientPacket)
            playFilledEffect();
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("Charge", charge);
        tag.putInt("CapacityLevel", capacityLevel);
        if(name != null)
            tag.putString("CustomName", Text.Serializer.toJson(name));
        tag.put("VanillaTag", vanillaTag);
    }

    public void setCapacityEnchantLevel(int level) {
        capacityLevel = level;
        maxCharge = BatteryUtils.getMaxCharge(level);
    }

    public void setCharge(int charge) {
        this.charge = charge;
        if(!world.isClient)
            sendData();
    }

    @Override
    public Text getName() {
        return name;
    }

    public void setName(Text name) {
        this.name = name;
    }

    public void setTags(NbtCompound vanillaTag) {
        this.vanillaTag = vanillaTag;
    }

    public NbtCompound getVanillaTag() {
        return vanillaTag.copy();
    }

    public int getCharge() {
        return charge;
    }

    public int getComparatorOutput() {
        return (int) ((float) charge / maxCharge * 15);
    }
}
