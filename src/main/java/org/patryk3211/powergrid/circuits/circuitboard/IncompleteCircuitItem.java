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
package org.patryk3211.powergrid.circuits.circuitboard;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.utility.Lang;

import java.util.HashMap;
import java.util.List;

public class IncompleteCircuitItem extends Item {
    public IncompleteCircuitItem(Settings settings) {
        super(settings.maxCount(1));
    }

    private static NbtCompound makeAssemblyTag(NbtCompound schematicTag) {
        var schematic = CircuitSchematic.fromNbt(schematicTag);
        var componentAmounts = new HashMap<Item, Integer>();
        int componentCount = 0;
        for(var placed : schematic.components()) {
            var item = placed.component.getRequiredItem();
            componentAmounts.compute(item, (key, current) -> current == null ? 1 : current + 1);
            ++componentCount;
        }
        var componentTag = new NbtCompound();
        componentAmounts.forEach((item, count) -> {
            var id = Registries.ITEM.getId(item);
            componentTag.putInt(id.toString(), count);
        });

        var assemblyTag = new NbtCompound();
        assemblyTag.put("Missing", componentTag);
        assemblyTag.putInt("Inserted", 0);
        assemblyTag.putInt("Total", componentCount);
        return assemblyTag;
    }

    private static boolean insertComponent(NbtCompound assemblyTag, ItemStack component) {
        var missingComponents = assemblyTag.getCompound("Missing");
        var id = Registries.ITEM.getId(component.getItem()).toString();
        if(!missingComponents.contains(id))
            return false;
        int missingAmount = missingComponents.getInt(id);
        if(--missingAmount <= 0) {
            missingComponents.remove(id);
        } else {
            missingComponents.putInt(id, missingAmount);
        }
        assemblyTag.putInt("Inserted", assemblyTag.getInt("Inserted") + 1);
        return true;
    }

//    @Nullable
//    public static ItemVariant insert(ItemVariant circuit, ItemStack component) {
//        if(!circuit.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get()))
//            return null;
//        var tag = circuit.copyNbt();
//        if(!tag.contains("Assembly")) {
//            tag.put("Assembly", makeAssemblyTag(tag.getCompound("Schematic")));
//        }
//        if(!insertComponent(tag.getCompound("Assembly"), component))
//            return null;
//        var missing = tag.getCompound("Assembly").getCompound("Missing");
//        if(missing.isEmpty()) {
//            tag.remove("Assembly");
//            return ItemVariant.of(ModdedBlocks.CIRCUIT_BOARD, tag);
//        }
//        return ItemVariant.of(ModdedItems.INCOMPLETE_CIRCUIT, tag);
//    }

    @Nullable
    public static ItemStack insert(ItemStack circuit, ItemStack component) {
        if(!circuit.isOf(ModdedItems.INCOMPLETE_CIRCUIT.get()) || !circuit.hasNbt())
            return null;
        var tag = circuit.getNbt().copy();
        if(!tag.contains("Assembly")) {
            tag.put("Assembly", makeAssemblyTag(tag.getCompound("Schematic")));
        }
        if(!insertComponent(tag.getCompound("Assembly"), component))
            return null;
        var missing = tag.getCompound("Assembly").getCompound("Missing");
        ItemStack newStack;
        if(missing.isEmpty()) {
            tag.remove("Assembly");
            newStack = new ItemStack(ModdedBlocks.CIRCUIT_BOARD);
        } else {
            newStack = new ItemStack(ModdedItems.INCOMPLETE_CIRCUIT);
        }
        newStack.setNbt(tag);
        return newStack;
    }

    public static float getProgress(ItemStack stack) {
        if(!stack.hasNbt() || !stack.getNbt().contains("Assembly"))
            return 0;
        var assemblyTag = stack.getSubNbt("Assembly");
        return (float) assemblyTag.getInt("Inserted") / assemblyTag.getInt("Total");
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13 * getProgress(stack));
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return Color.mixColors(0xFFFFC074, 0xFF46FFE0, getProgress(stack));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(!stack.hasNbt())
            return;
        NbtCompound assemblyTag;
        if(!stack.getNbt().contains("Assembly")) {
            if(!stack.getNbt().contains("Schematic"))
                return;
            assemblyTag = makeAssemblyTag(stack.getSubNbt("Schematic"));
        } else {
            assemblyTag = stack.getSubNbt("Assembly");
        }
        tooltip.add(Text.empty());
        tooltip.add(Lang.translate("tooltip.circuit_assembly")
                .style(Formatting.GRAY)
                .component());

        var inserted = assemblyTag.getInt("Inserted");
        var total = assemblyTag.getInt("Total");
        tooltip.add(Lang.translate("tooltip.circuit_assembly.progress")
                .add(Text.literal(String.format(": %d/%d", inserted, total)))
                .style(Formatting.DARK_GRAY)
                .component());
        var missing = assemblyTag.getCompound("Missing");
        int index = 0;
        for(var itemId : missing.getKeys()) {
            var item = Registries.ITEM.get(new Identifier(itemId));
            var key = item.getTranslationKey();
            var line = switch(index) {
                case 0 -> Lang.translate("tooltip.circuit_assembly.insert")
                        .add(Text.literal(" "))
                        .add(Text.translatable(key))
                        .style(Formatting.AQUA)
                        .component();
                case 1 -> Lang.text("-> ")
                        .add(Lang.translate("tooltip.circuit_assembly.insert"))
                        .add(Text.literal(" "))
                        .add(Text.translatable(key))
                        .style(Formatting.DARK_AQUA)
                        .component();
                default -> throw new IllegalStateException();
            };
            tooltip.add(line);
            if(++index >= 2)
                break;
        }
    }
}
