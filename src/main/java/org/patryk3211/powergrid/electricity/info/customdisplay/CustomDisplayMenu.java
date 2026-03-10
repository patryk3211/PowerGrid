/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity.info.customdisplay;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.utility.Unit;

public class CustomDisplayMenu extends MenuBase<SmartBlockEntity> {
    protected String expression;
    protected Unit unit;
    protected String unitStr;
    protected boolean enablePrefixes;

    public CustomDisplayMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public CustomDisplayMenu(MenuType<?> type, int id, Inventory inv, SmartBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected SmartBlockEntity createOnClient(FriendlyByteBuf extraData) {
        var world = Minecraft.getInstance().level;
        var be = world.getBlockEntity(extraData.readBlockPos());
        if(be instanceof SmartBlockEntity sbe) {
            sbe.readClient(extraData.readNbt());
            return sbe;
        }
        return null;
    }

    @Override
    protected void initAndReadInventory(SmartBlockEntity be) {
        var cdb = be.getBehaviour(CustomDisplayBehaviour.TYPE);
        if(cdb == null)
            return;
        expression = cdb.equationStr;
        unit = cdb.unit;
        unitStr = cdb.unitStr;
        enablePrefixes = cdb.prefixes;
    }

    @Override
    protected void addSlots() {

    }

    @Override
    protected void saveData(SmartBlockEntity contentHolder) {

    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
