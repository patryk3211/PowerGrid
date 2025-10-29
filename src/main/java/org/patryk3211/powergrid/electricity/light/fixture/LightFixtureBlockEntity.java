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

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulbState;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class LightFixtureBlockEntity extends ElectricBlockEntity {
    private SwitchedWire filament;
    @Nullable
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
            setChanged();
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
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if(bulbState != null) {
            bulbState.write(tag);
        }
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        if(bulbState != null) {
            bulbState.write(tag);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
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
        builder.setTerminalCount(2);
        filament = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1), false);
    }

    public boolean replaceBulb(Player player, InteractionHand hand, ItemStack usedStack) {
        boolean result = replaceBulbInternal(player, hand, usedStack);
        if(result) {
            lightBulbChanged();
        }
        return result;
    }

    private boolean replaceBulbInternal(Player player, InteractionHand hand, ItemStack usedStack) {
        assert level != null;
        if(usedStack == null || usedStack.isEmpty()) {
            if(bulbState == null)
                return false;
            if(!level.isClientSide) {
                if(!bulbState.isBurned())
                    player.setItemInHand(hand, bulbState.toStack());
                bulbState = null;
            }
            return true;
        } else {
            if(bulbState == null) {
                if(!level.isClientSide) {
                    var item = usedStack.getItem();
                    if(item instanceof ILightBulb bulb) {
                        bulbState = bulb.createState(this);
                        if (!player.isCreative())
                            usedStack.shrink(1);
                    }
                }
                return true;
            } else if(bulbState.isBurned()) {
                if(!level.isClientSide) {
                    bulbState = null;
                }
                return true;
            } else if(bulbState.isOf(usedStack.getItem()) && usedStack.getCount() < usedStack.getMaxStackSize()) {
                if(!level.isClientSide) {
                    usedStack.grow(1);
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
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        if(bulbState != null)
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, bulbState.getItem());
        return ItemRequirement.NONE;
    }
}
