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
package org.patryk3211.powergrid.electricity.electromagnet;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import java.util.List;
import java.util.Optional;

public class ElectromagnetBlockEntity extends ElectricBlockEntity implements MagnetizingBehaviour.MagnetizingBehaviourSpecifics {
    private ElectricWire wire;
    private MagnetizingBehaviour magnetizingBehaviour;

    public ElectromagnetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        magnetizingBehaviour = new MagnetizingBehaviour(this);
        behaviours.add(magnetizingBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
        applyLostPower(wire.power());
        if(magnetizingBehaviour.running) {
            wire.setResistance(ElectromagnetBlock.resistance() * 0.5f);
        } else {
            wire.setResistance(ElectromagnetBlock.resistance());
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connect(ElectromagnetBlock.resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }

    @Override
    public boolean tryProcessOnBelt(TransportedItemStack input, List<ItemStack> outputList, boolean simulate) {
        var recipe = getRecipe(input.stack);
        if(recipe.isEmpty())
            return false;
        if(simulate)
            return true;

        var outputs = RecipeApplier.applyRecipeOn(world, ItemHandlerHelper.copyStackWithSize(input.stack, 1), recipe.get());
//        for(ItemStack created : outputs) {
//            if(!created.isEmpty()) {
//                onItemPressed(created);
//                break;
//            }
//        }

        outputList.addAll(outputs);
        return true;
    }

    @Override
    public boolean tryProcessInWorld(ItemEntity itemEntity, boolean simulate) {
        var item = itemEntity.getStack();
        var recipe = getRecipe(item);
        if(recipe.isEmpty())
            return false;
        if(simulate)
            return true;

        for(var result : RecipeApplier.applyRecipeOn(world, ItemHandlerHelper.copyStackWithSize(item, 1), recipe.get())) {
            var created = new ItemEntity(world, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), result);
            created.setToDefaultPickupDelay();
            created.setVelocity(VecHelper.offsetRandomly(Vec3d.ZERO, world.random, .05f));
            world.spawnEntity(created);
        }
        item.decrement(1);
        return true;
    }

    @Override
    public void onMagnetizationComplete() {

    }

    @Override
    public float getFieldStrength() {
        var field = Math.abs(wire.current() * 0.1f);
        if(field < 0.25f)
            return 0;
        return field;
    }

    private static final Inventory magnetizingInv = new ItemStackHandlerContainer(1);
    public Optional<MagnetizingRecipe> getRecipe(ItemStack item) {
        Optional<MagnetizingRecipe> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(world, item, MagnetizingRecipe.TYPE_INFO.getType(), MagnetizingRecipe.class);
        if(assemblyRecipe.isPresent())
            return assemblyRecipe;

        magnetizingInv.setStack(0, item);
        return world.getRecipeManager().getFirstMatch(MagnetizingRecipe.TYPE_INFO.getType(), magnetizingInv, world);
    }
}
