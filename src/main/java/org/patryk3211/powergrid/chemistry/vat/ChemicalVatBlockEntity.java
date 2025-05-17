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
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.reagent.mixture.VolumeReagentInventory;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionGetter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChemicalVatBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity {
    public static final int DIFFUSION_RATE = 100;

    private final VolumeReagentInventory reagentInventory;

    public ChemicalVatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        reagentInventory = new VolumeReagentInventory(1000 * 32);
    }

    @Override
    public void tick() {
        super.tick();

        moveReagents();

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

    public void moveReagents() {
        var gasses = new HashSet<Reagent>();
        var liquids = new HashSet<Reagent>();
        var solids = new HashSet<Reagent>();

        reagentInventory.getReagents().forEach(reagent -> {
            switch(reagentInventory.getState(reagent)) {
                case GAS -> gasses.add(reagent);
                case LIQUID -> liquids.add(reagent);
                case SOLID -> solids.add(reagent);
            }
        });

        for(var dir : Direction.values()) {
            var vat = getVat(pos.offset(dir));
            if(vat == null)
                continue;
            if(dir == Direction.DOWN) {
                // Solids can only go down.
                moveReagents(solids, vat, reagentInventory.getTotalAmount());
            }
            if(dir != Direction.UP) {
                // Liquids cannot go up.
                var liquidLevel1 = reagentInventory.getFillLevel();
                var liquidLevel2 = vat.reagentInventory.getFillLevel();
                var targetLevel = (liquidLevel1 + liquidLevel2) * 0.5f;

                float moveFraction = liquidLevel1 - targetLevel;
                int moveAmount = (int) (moveFraction * reagentInventory.getVolume());
                int diffuseAmount = DIFFUSION_RATE - moveAmount;
                moveReagents(liquids, vat, moveAmount);
                if(diffuseAmount > 0) {
                    diffuse(liquids, ReagentState.LIQUID, vat, diffuseAmount);
                }
            }
            if(reagentInventory.getFreeVolume() == 0 && vat.reagentInventory.getFreeVolume() == 0) {
                // No free volume so no gas movement can occur.
                continue;
            } else if(reagentInventory.getFreeVolume() == 0) {
                // Must move all gas from this inventory.
                moveReagents(gasses, vat, reagentInventory.getGasAmount());
            } else if(vat.reagentInventory.getFreeVolume() == 0) {
                // Cannot move gas into a full inventory.
                continue;
            } else {
                // Gasses can go anywhere.
                var gasPressure1 = reagentInventory.getGasAmount() / reagentInventory.getFreeVolume();
                var gasPressure2 = vat.reagentInventory.getGasAmount() / vat.reagentInventory.getFreeVolume();
                var targetPressure = (gasPressure1 + gasPressure2) * 0.5f;

                float moveFraction = gasPressure1 - targetPressure;
                int moveAmount = (int) (moveFraction * reagentInventory.getFreeVolume());
                int diffuseAmount = DIFFUSION_RATE - moveAmount;
                moveReagents(gasses, vat, moveAmount);
                if(diffuseAmount > 0) {
                    diffuse(gasses, ReagentState.GAS, vat, diffuseAmount);
                }
            }
        }

        markDirty();
    }

    private void diffuse(Set<Reagent> thisReagents, ReagentState state, ChemicalVatBlockEntity target, int amount) {
        if(thisReagents.isEmpty())
            return;
        var otherReagents = new HashSet<Reagent>();
        for(var reagent : target.reagentInventory.getReagents()) {
            if(target.reagentInventory.getState(reagent) == state)
                otherReagents.add(reagent);
        }

        while(amount > 0) {
            try(var transaction = Transaction.openOuter()) {
                var mix1 = reagentInventory.remove(amount, thisReagents, transaction);
                amount = mix1.getTotalAmount();
                var mix2 = target.reagentInventory.remove(amount, otherReagents, transaction);
                if(mix2.getTotalAmount() != amount) {
                    amount = mix2.getTotalAmount();
                    transaction.abort();
                    continue;
                }
                int added = reagentInventory.add(mix2, transaction);
                if(added != amount) {
                    amount = added;
                    transaction.abort();
                    continue;
                }
                added = target.reagentInventory.add(mix1, transaction);
                if(added != amount) {
                    amount = added;
                    transaction.abort();
                    continue;
                }
                transaction.commit();
                break;
            }
        }
    }

    private void moveReagents(Set<Reagent> reagents, ChemicalVatBlockEntity target, int amount) {
        if(reagents.isEmpty())
            return;
        while(amount > 0) {
            try(var transaction = Transaction.openOuter()) {
                var mix = reagentInventory.remove(amount, reagents, transaction);
                amount = mix.getTotalAmount();
                int added = target.reagentInventory.add(mix, transaction);
                if(added == amount) {
                    transaction.commit();
                    break;
                }
                transaction.abort();
                amount = added;
            }
        }
    }

    @Nullable
    public ChemicalVatBlockEntity getVat(BlockPos pos) {
        if(world == null)
            return null;
        if(world.getBlockEntity(pos) instanceof ChemicalVatBlockEntity vat)
            return vat;
        return null;
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

    @Nullable
    @Override
    public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        return reagentInventory.getItemView();
    }

    @Nullable
    @Override
    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        return reagentInventory.getFluidView();
    }
}
