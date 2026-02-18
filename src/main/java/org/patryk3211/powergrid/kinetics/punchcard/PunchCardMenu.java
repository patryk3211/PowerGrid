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
package org.patryk3211.powergrid.kinetics.punchcard;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public abstract class PunchCardMenu extends GhostItemMenu<ItemStack> {
    public static PunchCardMenuConstructors CONSTRUCTORS;
    public byte[] data;

    public PunchCardMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public PunchCardMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected void init(Inventory inv, ItemStack stack) {
        super.init(inv, stack);
        if(data == null)
            data = new byte[16];
        if(!stack.has(DataComponents.CUSTOM_DATA))
            return;
        var bytes = stack.get(DataComponents.CUSTOM_DATA).copyTag().getByteArray("Data");
        System.arraycopy(bytes, 0, data, 0, Math.min(data.length, bytes.length));
    }

    public boolean isLocked() {
        if(!contentHolder.has(DataComponents.CUSTOM_DATA))
            return false;
        return contentHolder.get(DataComponents.CUSTOM_DATA).copyTag().getBoolean("Locked");
    }

    public String getAuthor() {
        if(!contentHolder.has(DataComponents.CUSTOM_DATA))
            return "";
        return contentHolder.get(DataComponents.CUSTOM_DATA).copyTag().getString("Author");
    }

    @Override
    protected boolean allowRepeats() {
        return false;
    }

    @Override
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void addSlots() {

    }

    @Override
    protected void saveData(ItemStack contentHolder) {

    }

    public void lock() {
        var tag = contentHolder.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("Locked", true);
        tag.putString("Author", player.getDisplayName().getString());
        contentHolder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public interface PunchCardMenuConstructors {
        PunchCardMenu create(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf buf);
        PunchCardMenu create(MenuType<?> type, int id, Inventory inv, ItemStack holder);
    }
}
