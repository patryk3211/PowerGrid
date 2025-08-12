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
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

        var outputs = RecipeApplier.applyRecipeOn(level, input.stack.copyWithCount(1), recipe.get());
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
        var item = itemEntity.getItem();
        var recipe = getRecipe(item);
        if(recipe.isEmpty())
            return false;
        if(simulate)
            return true;

        for(var result : RecipeApplier.applyRecipeOn(level, item.copyWithCount(1), recipe.get())) {
            var created = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), result);
            created.setDefaultPickUpDelay();
            created.setDeltaMovement(VecHelper.offsetRandomly(Vec3.ZERO, level.random, .05f));
            level.addFreshEntity(created);
        }
        item.shrink(1);
        return true;
    }

    @Override
    public void onMagnetizationComplete() {

    }

    @Override
    public float getFieldStrength() {
        var I = wire.current();
        if(isVirtual())
            I = (float) (wire.potentialDifference() * wire.conductance());
        var field = Math.abs(I * 0.1f);
        if(field < 0.25f)
            return 0;
        return field;
    }

    private static final Container magnetizingInv = new SimpleContainer(1);
    public Optional<MagnetizingRecipe> getRecipe(ItemStack item) {
        Optional<MagnetizingRecipe> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, item, MagnetizingRecipe.TYPE_INFO.getType(), MagnetizingRecipe.class);
        if(assemblyRecipe.isPresent())
            return assemblyRecipe;

        magnetizingInv.setItem(0, item);
        return level.getRecipeManager().getRecipeFor(MagnetizingRecipe.TYPE_INFO.getType(), magnetizingInv, level);
    }

    public MagnetizingBehaviour getMagnetizingBehaviour() {
        return magnetizingBehaviour;
    }
}
