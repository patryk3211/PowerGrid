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
package org.patryk3211.powergrid.chemistry.vat;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.reagent.OpenReagentInventory;
import org.patryk3211.powergrid.chemistry.reagent.source.ReagentSource;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionGetter;

import java.util.List;

public class ChemicalVatBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity {
    private final OpenReagentInventory reagentInventory = new OpenReagentInventory();
    private final SmartInventory itemInventory;
    private Storage<FluidVariant> fluidStorage;

    public ChemicalVatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        itemInventory = new SmartInventory(9, this, 16, false);
        fluidStorage = reagentInventory.getFluidView();
    }

    @Override
    public void tick() {
        super.tick();

        reagentInventory.tick();
        var recipes = ReactionGetter.getValidRecipes(world.getRecipeManager(), reagentInventory);
        if(recipes.isEmpty()) {
            reagentInventory.setBurning(false);
            return;
        }

        boolean stillBurning = false;
        var random = world.getRandom();
        for(int i = recipes.size(); i > 0; --i) {
            // Pick random recipe and apply it.
            int reactionIndex = i == 1 ? 0 : random.nextInt(i);
            var reaction = recipes.get(reactionIndex);
            // Test if the reaction is still valid.
            if(reaction.test(reagentInventory)) {
                reagentInventory.applyReaction(reaction);
                if(reaction.hasFlag(ReactionFlag.COMBUSTION)) {
                    stillBurning = true;
                }
            }
        }

        if(!stillBurning) {
            reagentInventory.setBurning(false);
        }
        markDirty();
    }

    public boolean addStack(ReagentSource source) {
        var stack = source.defaultStack();
        if(reagentInventory.accepts(stack) == stack.getAmount()) {
            reagentInventory.add(source.defaultStack());
            markDirty();
            return true;
        }
        return false;
    }

    public void light() {
        reagentInventory.setBurning(true);
        markDirty();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        reagentInventory.read(tag);
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        reagentInventory.write(tag);
    }

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        return itemInventory;
    }

    @Override
    public @Nullable Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        return fluidStorage;
    }
}
