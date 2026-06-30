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
package org.patryk3211.powergrid.collections;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.mutable.MutableObject;
import org.patryk3211.powergrid.PowerGrid;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class ItemDisplay {
    public static class BaseItemDisplay implements CreativeModeTab.DisplayItemsGenerator {
        private static final Predicate<Item> IS_ITEM_3D_PREDICATE;

        static {
            MutableObject<Predicate<Item>> isItem3d = new MutableObject<>(item -> false);
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                isItem3d.setValue(item -> {
                    ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                    BakedModel model = itemRenderer.getModel(new ItemStack(item), null, null, 0);
                    return model.isGui3d();
                });
            });
            IS_ITEM_3D_PREDICATE = isItem3d.getValue();
        }

        private final boolean addItems;

        public BaseItemDisplay(boolean addItems) {
            this.addItems = addItems;
        }

        private static Predicate<Item> makeExclusionPredicate() {
            Set<Item> exclusions = new ReferenceOpenHashSet<>();

            List<ItemProviderEntry<?, ?>> simpleExclusions = List.of(
                    ModdedItems.INCOMPLETE_TRANSFORMER_CORE,
                    ModdedItems.INCOMPLETE_BATTERY,
                    ModdedItems.INCOMPLETE_CIRCUIT,
                    ModdedItems.INCOMPLETE_ELECTRICAL_GIZMO,
                    ModdedItems.INCOMPLETE_UNETCHED_CIRCUIT,
                    ModdedItems.INCOMPLETE_PUNCH_CARD,
                    ModdedItems.INCOMPLETE_BJT_NPN,
                    ModdedItems.INCOMPLETE_BJT_PNP,
                    ModdedItems.INCOMPLETE_SOLAR_PANEL,
                    ModdedItems.PORTABLE_BATTERY_PLACEABLE,
                    ModdedItems.CIRCUIT_SCHEMATIC,
                    ModdedItems.UNETCHED_CIRCUIT,
                    ModdedBlocks.CIRCUIT_BOARD
            );

            for (ItemProviderEntry<?, ?> entry : simpleExclusions) {
                exclusions.add(entry.asItem());
            }

            return exclusions::contains;
        }

        private static List<ItemOrdering> makeOrderings() {
            List<ItemOrdering> orderings = new ReferenceArrayList<>();

//            Map<ItemProviderEntry<?>, ItemProviderEntry<?>> simpleBeforeOrderings = Map.of(
//                    AllItems.EMPTY_BLAZE_BURNER, AllBlocks.BLAZE_BURNER,
//                    AllItems.SCHEDULE, AllBlocks.TRACK_STATION
//            );
//
//            Map<ItemProviderEntry<?>, ItemProviderEntry<?>> simpleAfterOrderings = Map.of(
//                    AllItems.VERTICAL_GEARBOX, AllBlocks.GEARBOX
//            );
//
//            simpleBeforeOrderings.forEach((entry, otherEntry) -> {
//                orderings.add(ItemOrdering.before(entry.asItem(), otherEntry.asItem()));
//            });
//
//            simpleAfterOrderings.forEach((entry, otherEntry) -> {
//                orderings.add(ItemOrdering.after(entry.asItem(), otherEntry.asItem()));
//            });

            return orderings;
        }

        private static Function<Item, ItemStack> makeStackFunc() {
            Map<Item, Function<Item, ItemStack>> factories = new Reference2ReferenceOpenHashMap<>();

            Map<ItemProviderEntry<?, ?>, Function<Item, ItemStack>> simpleFactories = Map.of(
//                    AllItems.COPPER_BACKTANK, item -> {
//                        ItemStack stack = new ItemStack(item);
//                        stack.getOrCreateTag().putInt("Air", BacktankUtil.maxAirWithoutEnchants());
//                        return stack;
//                    },
//                    AllItems.NETHERITE_BACKTANK, item -> {
//                        ItemStack stack = new ItemStack(item);
//                        stack.getOrCreateTag().putInt("Air", BacktankUtil.maxAirWithoutEnchants());
//                        return stack;
//                    }
            );

            simpleFactories.forEach((entry, factory) -> {
                factories.put(entry.asItem(), factory);
            });

            return item -> {
                Function<Item, ItemStack> factory = factories.get(item);
                if (factory != null) {
                    return factory.apply(item);
                }
                return new ItemStack(item);
            };
        }

        private static Function<Item, CreativeModeTab.TabVisibility> makeVisibilityFunc() {
            Map<Item, CreativeModeTab.TabVisibility> visibilities = new Reference2ObjectOpenHashMap<>();

            Map<ItemProviderEntry<?, ?>, CreativeModeTab.TabVisibility> simpleVisibilities = Map.of(
//                    AllItems.BLAZE_CAKE_BASE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
            );

            simpleVisibilities.forEach((entry, factory) -> {
                visibilities.put(entry.asItem(), factory);
            });

//            for (BlockEntry<ValveHandleBlock> entry : AllBlocks.DYED_VALVE_HANDLES) {
//                visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//            }
//
//            for (BlockEntry<SeatBlock> entry : AllBlocks.SEATS) {
//                SeatBlock block = entry.get();
//                if (block.getColor() != DyeColor.RED) {
//                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//                }
//            }
//
//            for (BlockEntry<TableClothBlock> entry : AllBlocks.TABLE_CLOTHS) {
//                TableClothBlock block = entry.get();
//                if (block.getColor() != DyeColor.RED) {
//                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//                }
//            }
//
//            for (BlockEntry<PostboxBlock> entry : AllBlocks.PACKAGE_POSTBOXES) {
//                PostboxBlock block = entry.get();
//                if (block.getColor() != DyeColor.WHITE) {
//                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//                }
//            }
//
//            for (BlockEntry<ToolboxBlock> entry : AllBlocks.TOOLBOXES) {
//                ToolboxBlock block = entry.get();
//                if (block.getColor() != DyeColor.BROWN) {
//                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//                }
//            }

            return item -> {
                CreativeModeTab.TabVisibility visibility = visibilities.get(item);
                if(visibility != null)
                    return visibility;
                return CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            };
        }

        @Override
        public void accept(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
            Predicate<Item> exclusionPredicate = makeExclusionPredicate();
            List<ItemOrdering> orderings = makeOrderings();
            Function<Item, ItemStack> stackFunc = makeStackFunc();
            Function<Item, CreativeModeTab.TabVisibility> visibilityFunc = makeVisibilityFunc();

            List<Item> items = new LinkedList<>();
            if (addItems) {
                items.addAll(collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE.negate())));
            }
            items.addAll(collectBlocks(exclusionPredicate));
            if (addItems) {
                items.addAll(collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE)));
            }

            applyOrderings(items, orderings);
            outputAll(output, items, stackFunc, visibilityFunc);
        }

        private List<Item> collectBlocks(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Block, ? extends Block> entry : PowerGrid.REGISTRATE.getAll(Registries.BLOCK)) {
                Item item = entry.get()
                        .asItem();
                if (item == Items.AIR)
                    continue;
                if (!exclusionPredicate.test(item))
                    items.add(item);
            }
            items = new ReferenceArrayList<>(new ReferenceLinkedOpenHashSet<>(items));
            return items;
        }

        private List<Item> collectItems(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Item, ? extends Item> entry : PowerGrid.REGISTRATE.getAll(Registries.ITEM)) {
                Item item = entry.get();
                if (item instanceof BlockItem)
                    continue;
                if (!exclusionPredicate.test(item))
                    items.add(item);
            }
            return items;
        }

        private static void applyOrderings(List<Item> items, List<ItemOrdering> orderings) {
            for (ItemOrdering ordering : orderings) {
                int anchorIndex = items.indexOf(ordering.anchor());
                if (anchorIndex != -1) {
                    Item item = ordering.item();
                    int itemIndex = items.indexOf(item);
                    if (itemIndex != -1) {
                        items.remove(itemIndex);
                        if (itemIndex < anchorIndex) {
                            anchorIndex--;
                        }
                    }
                    if (ordering.type() == ItemOrdering.Type.AFTER) {
                        items.add(anchorIndex + 1, item);
                    } else {
                        items.add(anchorIndex, item);
                    }
                }
            }
        }

        private static void outputAll(CreativeModeTab.Output output, List<Item> items, Function<Item, ItemStack> stackFunc, Function<Item, CreativeModeTab.TabVisibility> visibilityFunc) {
            for (Item item : items) {
                output.accept(stackFunc.apply(item), visibilityFunc.apply(item));
            }
        }

        private record ItemOrdering(Item item, Item anchor, ItemOrdering.Type type) {
            public static ItemOrdering before(Item item, Item anchor) {
                return new ItemOrdering(item, anchor, ItemOrdering.Type.BEFORE);
            }

            public static ItemOrdering after(Item item, Item anchor) {
                return new ItemOrdering(item, anchor, ItemOrdering.Type.AFTER);
            }

            public enum Type {
                BEFORE,
                AFTER;
            }
        }
    }
}
