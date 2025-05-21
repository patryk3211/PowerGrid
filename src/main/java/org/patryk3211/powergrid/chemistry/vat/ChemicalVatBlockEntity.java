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

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.reagent.mixture.ConstantReagentMixture;
import org.patryk3211.powergrid.chemistry.reagent.mixture.VolumeReagentInventory;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;
import org.patryk3211.powergrid.chemistry.recipe.ReactionGetter;
import org.patryk3211.powergrid.chemistry.recipe.RecipeProgressStore;
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.*;

public class ChemicalVatBlockEntity extends SmartBlockEntity implements SidedStorageBlockEntity, IHaveGoggleInformation {
    public static final float DIFFUSION_RATE = 0.05f;
    public static final float ATMOSPHERIC_PRESSURE = 1.02f;
    // TODO: Balance this value.
    public static final float DISSIPATION_FACTOR = 200f;

    private final VolumeReagentInventory reagentInventory;
    private final RecipeProgressStore progressStore;
    @NotNull
    private ItemStack catalyzer;

    public ChemicalVatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        reagentInventory = new VolumeReagentInventory(1000 * 32);
        progressStore = new RecipeProgressStore();
        catalyzer = ItemStack.EMPTY;
        setLazyTickRate(20);
    }

    @Override
    public void tick() {
        super.tick();

        boolean stillBurning = false;
        var recipes = ReactionGetter.getValidRecipes(world.getRecipeManager(), reagentInventory);
        if(!recipes.isEmpty()) {
            Collections.shuffle(recipes);
            for(int i = 0; i < recipes.size(); ++i) {
                var reaction = recipes.get(i);
                // Test if the reaction is still valid.
                if(reaction.test(reagentInventory)) {
                    reagentInventory.applyReaction(reaction, progressStore);
                    if(reaction.hasFlag(ReactionFlag.COMBUSTION)) {
                        stillBurning = true;
                    }
                }
            }
        }
        progressStore.filter(recipes);
        if(!stillBurning) {
            reagentInventory.setBurning(false);
        }

        // Moving has to occur after recipe processing so that the burning flag is valid.
        moveReagents();

        if(getCachedState().get(ChemicalVatBlock.OPEN)) {
            reagentInventory.setOpen(true);

            // Allow gasses in and out
            var availableVolume = Math.max(reagentInventory.getFreeVolume(), 1000);
            var vatPressure = (float) reagentInventory.getGasAmount() / availableVolume;

            var difference = ATMOSPHERIC_PRESSURE - vatPressure;
            var moveAmount = (int) (difference * availableVolume);
            int diffuseAmount = (int) (DIFFUSION_RATE * reagentInventory.getGasAmount()) - Math.abs(moveAmount);

            // TODO: I'm not sure if I implemented transactions correctly (probably not) so this is split in two.
            try(var transaction = Transaction.openOuter()) {
                if (diffuseAmount > 0) {
                    var removed = reagentInventory.remove(diffuseAmount, ReagentState.GAS, transaction);
                    reagentInventory.add(ConstantReagentMixture.ATMOSPHERE.scaledTo(removed.getTotalAmount()), transaction);
                }
                transaction.commit();
            }

            try(var transaction = Transaction.openOuter()) {
                if(moveAmount < 0) {
                    if(moveAmount < -100000)
                        moveAmount = -100000;
                    reagentInventory.remove(-moveAmount, ReagentState.GAS, transaction);
                } else {
                    if(moveAmount > 100000)
                        moveAmount = 100000;
                    reagentInventory.add(ConstantReagentMixture.ATMOSPHERE.scaledTo(moveAmount), transaction);
                }
                transaction.commit();
            }
        } else {
            reagentInventory.setOpen(false);
        }

        var tempDiff = reagentInventory.temperature() - 22f;
        reagentInventory.removeEnergy(tempDiff * DISSIPATION_FACTOR * 0.05f);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        sendData();
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
            if(dir == Direction.DOWN && !solids.isEmpty()) {
                // Solids can only go down.
                moveReagents(solids, vat, reagentInventory.getTotalAmount());
            }
            if(dir != Direction.UP && !liquids.isEmpty()) {
                // Liquids cannot go up.
                var liquidLevel1 = reagentInventory.getFillLevel();
                var liquidLevel2 = vat.reagentInventory.getFillLevel();
                var targetLevel = (liquidLevel1 + liquidLevel2) * 0.5f;

                float moveFraction = liquidLevel1 - targetLevel;
                int moveAmount = (int) (moveFraction * reagentInventory.getVolume());
                int diffuseAmount = (int) (reagentInventory.getLiquidAmount() * DIFFUSION_RATE) - moveAmount;
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
            } else if(!gasses.isEmpty()) {
                // Gasses can go anywhere.
                var gasPressure1 = (float) reagentInventory.getGasAmount() / reagentInventory.getFreeVolume();
                var gasPressure2 = (float) vat.reagentInventory.getGasAmount() / vat.reagentInventory.getFreeVolume();
                var targetPressure = (gasPressure1 + gasPressure2) * 0.5f;

                float moveFraction = gasPressure1 - targetPressure;
                int moveAmount = (int) (moveFraction * reagentInventory.getFreeVolume());
                int diffuseAmount = (int) (reagentInventory.getGasAmount() * DIFFUSION_RATE) - moveAmount;
                moveReagents(gasses, vat, moveAmount);
                if(diffuseAmount > 0) {
                    diffuse(gasses, ReagentState.GAS, vat, diffuseAmount);
                }
            }

            if(reagentInventory.isBurning()) {
                // Propagate burning effects.
                vat.reagentInventory.setBurning(true);
            }
        }
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
                var mix2 = target.reagentInventory.remove(mix1.getTotalAmount(), otherReagents, transaction);

                int added = reagentInventory.add(mix2, transaction);
                if(added != mix2.getTotalAmount()) {
                    amount = added;
                    transaction.abort();
                    continue;
                }
                added = target.reagentInventory.add(mix1, transaction);
                if(added != mix1.getTotalAmount()) {
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

    public ActionResult use(PlayerEntity player, Hand hand, BlockHitResult hit) {
        assert world != null;
        var stack = player.getStackInHand(hand);
        if(stack.isOf(Items.FLINT_AND_STEEL)) {
            if(!world.isClient) {
                if (!player.isCreative())
                    stack.damage(1, player, v -> {});
                light();
            }
            return ActionResult.SUCCESS;
        } else if(stack.isIn(ModdedTags.Item.CATALYZERS.tag)) {
            if(!catalyzer.isEmpty())
                return ActionResult.FAIL;
            if(!world.isClient) {
                catalyzer = stack.copyWithCount(1);
                stack.decrement(1);
                updateCatalyzer();
            }
            return ActionResult.SUCCESS;
        } else if(stack.isEmpty()) {
            if(catalyzer.isEmpty())
                return ActionResult.FAIL;
            if(!world.isClient) {
                player.setStackInHand(hand, catalyzer);
                catalyzer = ItemStack.EMPTY;
                updateCatalyzer();
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    public void light() {
        reagentInventory.setBurning(true);
        sendData();
    }

    private void updateCatalyzer() {
        if(!catalyzer.isEmpty())
            reagentInventory.setCatalyzer(1.0f);
        else
            reagentInventory.setCatalyzer(0.0f);
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.chemical_vat.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.chemical_vat.temperature")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var temperatureText = String.format("%.2f", reagentInventory.temperature());
        Lang.builder()
                .text(temperatureText)
                .add(Text.of(" "))
                .add(Unit.TEMPERATURE.get())
                .style(Formatting.YELLOW)
                .forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        reagentInventory.read(tag);
        progressStore.read(tag);
        if(tag.contains("Catalyzer")) {
            catalyzer = ItemStack.fromNbt(tag.getCompound("Catalyzer"));
        }
        updateCatalyzer();
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        reagentInventory.write(tag);
        progressStore.write(tag);
        if(!catalyzer.isEmpty()) {
            tag.put("Catalyzer", catalyzer.serializeNBT());
        }
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
