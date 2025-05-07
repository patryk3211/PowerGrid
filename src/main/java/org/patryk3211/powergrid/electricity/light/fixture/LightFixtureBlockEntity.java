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
package org.patryk3211.powergrid.electricity.light.fixture;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.Collection;
import java.util.List;

import static org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock.POWER;

public class LightFixtureBlockEntity extends ElectricBlockEntity {
    private FloatingNode node1;
    private FloatingNode node2;
    private SwitchedWire filament;

    private ItemStack lightBulb;
    private boolean burned;

    public LightFixtureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        burned = false;
        lightBulb = null;
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 0.005f, 0.1f, 1700f)
                .noOverheatBehaviour();
    }

    @Override
    public void tick() {
        super.tick();

        if(lightBulb != null && !burned) {
            var current = filament.current();
            applyLostPower(current * current * filament.getResistance());

            assert lightBulb.getItem() instanceof ILightBulb;
            var lightBulbProperties = (ILightBulb) lightBulb.getItem();

            filament.setResistance(lightBulbProperties.resistanceFunction(thermalBehaviour.getTemperature()));

            if(thermalBehaviour.isOverheated()) {
                burned = true;
                filament.setState(false);
            }
        }

        if(!world.isClient) {
            int powerLevel = 0;
            if(!burned) {
                var temperature = thermalBehaviour.getTemperature();
                if(temperature > 1400f) {
                    powerLevel = 2;
                } else if(temperature > 1200f) {
                    powerLevel = 1;
                }
            }
            if(powerLevel != getCachedState().get(POWER)) {
                world.setBlockState(pos, getCachedState().with(POWER, powerLevel));
            }
        }
    }

    private void lightBulbChanged() {
        if(lightBulb == null) {
            filament.setState(false);
            notifyUpdate();
            return;
        }

        assert lightBulb.getItem() instanceof ILightBulb;
        var lightBulbProperties = (ILightBulb) lightBulb.getItem();
        filament.setResistance(lightBulbProperties.resistanceFunction(thermalBehaviour.getTemperature()));
        thermalBehaviour.setDissipationFactor(lightBulbProperties.dissipationFactor());
        filament.setState(!burned);
        notifyUpdate();
    }

    @Nullable
    public ILightBulb getLightBulbProperties() {
        if(lightBulb == null)
            return null;
        return (ILightBulb) lightBulb.getItem();
    }

    public boolean isBurned() {
        return burned;
    }

    @Nullable
    public ILightBulb.State getState() {
        if(lightBulb == null)
            return null;
        var powerLevel = getCachedState().get(POWER);
        return burned ? ILightBulb.State.BROKEN : powerLevel > 0 ? ILightBulb.State.ON : ILightBulb.State.OFF;
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(lightBulb != null) {
            tag.put("Item", lightBulb.serializeNBT());
            if(burned) {
                tag.putBoolean("Burned", true);
            }
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if(tag.contains("Item")) {
            lightBulb = ItemStack.fromNbt(tag.getCompound("Item"));
            if(tag.contains("Burned")) {
                burned = tag.getBoolean("Burned");
            }
        } else {
            lightBulb = null;
            burned = false;
        }
        lightBulbChanged();
    }

    @Override
    public void initializeNodes() {
        node1 = new FloatingNode();
        node2 = new FloatingNode();
        filament = new SwitchedWire(1, node1, node2, false);
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(node1);
        nodes.add(node2);
    }

    @Override
    public void addInternalWires(Collection<ElectricWire> wires) {
        wires.add(filament);
    }

    public boolean replaceBulb(PlayerEntity player, Hand hand, ItemStack usedStack) {
        boolean result = replaceBulbInternal(player, hand, usedStack);
        if(result) {
            lightBulbChanged();
            thermalBehaviour.resetTemperature();
        }
        return result;
    }

    private boolean replaceBulbInternal(PlayerEntity player, Hand hand, ItemStack usedStack) {
        assert world != null;
        if(usedStack == null || usedStack.isEmpty()) {
            if(lightBulb == null)
                return false;
            if(!world.isClient) {
                if(!burned)
                    player.setStackInHand(hand, lightBulb);
                lightBulb = null;
            }
            return true;
        } else {
            if(lightBulb == null) {
                if(!world.isClient) {
                    lightBulb = usedStack.copyWithCount(1);
                    if(!player.isCreative())
                        usedStack.decrement(1);
                    burned = false;
                }
                return true;
            } else if(burned) {
                if(!world.isClient) {
                    lightBulb = null;
                }
                return true;
            } else if(lightBulb.isOf(usedStack.getItem()) && usedStack.getCount() < usedStack.getMaxCount()) {
                if(!world.isClient) {
                    usedStack.increment(1);
                    lightBulb = null;
                }
                return true;
            }
        }
        return false;
    }
}
