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
package org.patryk3211.powergrid.electricity.electromagnet.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class ElectromagnetBlockEntityImpl  {
    public static int tryTransferPower(ItemStack item, int power, boolean simulate) {
        LazyOptional<IEnergyStorage> itemEnergy = item.getCapability(ForgeCapabilities.ENERGY);
        if (!itemEnergy.isPresent()) return 0;
        int toTransfer = power;
        int powerTransfered;
        do { //Take all my power >:3
            powerTransfered = itemEnergy.map(handler -> handler.receiveEnergy(power, simulate)).orElse(0);
            toTransfer -= powerTransfered;
        } while(!simulate && powerTransfered != 0 && toTransfer > 0);
        return power - toTransfer;
    }
}
