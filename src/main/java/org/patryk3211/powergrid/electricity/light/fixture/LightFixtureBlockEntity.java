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

import io.github.fabricators_of_create.porting_lib.block.CustomRenderBoundingBoxBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulbState;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class LightFixtureBlockEntity extends ElectricBlockEntity implements CustomRenderBoundingBoxBlockEntity {
    private SwitchedWire filament;
    private LightBulbState bulbState;

    public LightFixtureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        bulbState = null;
    }

    @Override
    public void tick() {
        super.tick();

        if(bulbState != null) {
            bulbState.tick();
            markDirty();
        }
    }

    private void lightBulbChanged() {
        if(bulbState == null) {
            filament.setState(false);
        } else {
            filament.setResistance(bulbState.resistance());
            filament.setState(!bulbState.isBurned());
        }
        notifyUpdate();
    }

    @Nullable
    public LightBulbState getBulbState() {
        return bulbState;
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(bulbState != null) {
            bulbState.write(tag);
        }
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        var currentItem = bulbState != null ? bulbState.getItem() : null;
        var nbtItem = LightBulbState.getBulbItem(tag);
        if(currentItem != nbtItem) {
            if(nbtItem == null) {
                bulbState = null;
            } else {
                bulbState = ((ILightBulb) nbtItem).createState(this);
            }
        }
        if(bulbState != null) {
            bulbState.read(tag);
        }
        lightBulbChanged();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        var node1 = builder.addExternalNode();
        var node2 = builder.addExternalNode();
        filament = builder.connectSwitch(1, node1, node2, false);
    }

    public boolean replaceBulb(PlayerEntity player, Hand hand, ItemStack usedStack) {
        boolean result = replaceBulbInternal(player, hand, usedStack);
        if(result) {
            lightBulbChanged();
        }
        return result;
    }

    private boolean replaceBulbInternal(PlayerEntity player, Hand hand, ItemStack usedStack) {
        assert world != null;
        if(usedStack == null || usedStack.isEmpty()) {
            if(bulbState == null)
                return false;
            if(!world.isClient) {
                if(!bulbState.isBurned())
                    player.setStackInHand(hand, bulbState.toStack());
                bulbState = null;
            }
            return true;
        } else {
            if(bulbState == null) {
                if(!world.isClient) {
                    var item = usedStack.getItem();
                    if(item instanceof ILightBulb bulb) {
                        bulbState = bulb.createState(this);
                        if (!player.isCreative())
                            usedStack.decrement(1);
                    }
                }
                return true;
            } else if(bulbState.isBurned()) {
                if(!world.isClient) {
                    bulbState = null;
                }
                return true;
            } else if(bulbState.isOf(usedStack.getItem()) && usedStack.getCount() < usedStack.getMaxCount()) {
                if(!world.isClient) {
                    usedStack.increment(1);
                    bulbState = null;
                }
                return true;
            }
        }
        return false;
    }

    public SwitchedWire getFilament() {
        return filament;
    }

    @Override
    public Box getRenderBoundingBox() {
        return new Box(pos);
    }
}
