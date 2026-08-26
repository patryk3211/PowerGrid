package org.patryk3211.powergrid.advancements;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedAdvancements;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class PowerGridAdvancement {
    public static final ResourceLocation BACKGROUND = PowerGrid.texture("gui/advancements");
    public static final String LANG = "advancement.powergrid.";
    public static final String SECRET_SUFFIX = "\n§7(Hidden Advancement)";

    private final Advancement.Builder builder = Advancement.Builder.advancement();
    private final Builder pgBuilder = new Builder();
    private SimplePowerGridTrigger builtinTrigger;
    private PowerGridAdvancement parent;
    AdvancementHolder datagenResult;
    private final String id;
    private String title;
    private String description;

    public PowerGridAdvancement(String id, UnaryOperator<Builder> b) {
        this.id = id;
        b.apply(pgBuilder);
        if (!pgBuilder.externalTrigger) {
            builtinTrigger = PowerGridTriggers.addSimple(id + "_builtin");
            builder.addCriterion("0", builtinTrigger.createCriterion(builtinTrigger.instance()));
        }

        if (pgBuilder.type == PowerGridAdvancement.TaskType.SECRET) {
            this.description = this.description + SECRET_SUFFIX;
        }

        ModdedAdvancements.ENTRIES.add(this);
    }

    private String titleKey() {
        return LANG + this.id;
    }

    private String descriptionKey() {
        return this.titleKey() + ".desc";
    }

    public boolean isAlreadyAwardedTo(Player player) {
        if (player instanceof ServerPlayer sp) {
            AdvancementHolder advancement = sp.getServer().getAdvancements().get(PowerGrid.asResource(this.id));
            return advancement == null || sp.getAdvancements().getOrStartProgress(advancement).isDone();
        } else {
            return true;
        }
    }

    public void awardTo(Player player) {
        if (player instanceof ServerPlayer sp) {
            if (this.builtinTrigger == null) {
                throw new UnsupportedOperationException("Advancement " + this.id + " uses external Triggers, it cannot be awarded directly");
            } else {
                this.builtinTrigger.trigger(sp);
            }
        }
    }

    public void save(Consumer<AdvancementHolder> t, HolderLookup.Provider registries) {
        if (parent != null)
            builder.parent(parent.datagenResult);

        if (pgBuilder.func != null)
            pgBuilder.icon(pgBuilder.func.apply(registries));

        builder.display(pgBuilder.icon, Component.translatable(titleKey()),
                Component.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)),
                id.equals("root") ? BACKGROUND : null, pgBuilder.type.type, pgBuilder.type.toast,
                pgBuilder.type.announce, pgBuilder.type.hide);

        datagenResult = builder.save(t, PowerGrid.asResource(id)
                .toString());
    }

    public void provideLang(BiConsumer<String, String> consumer) {
        consumer.accept(this.titleKey(), this.title);
        consumer.accept(this.descriptionKey(), this.description);
    }

    public enum TaskType {
        SILENT(AdvancementType.TASK, false, false, false),
        NORMAL(AdvancementType.TASK, true, false, false),
        NOISY(AdvancementType.TASK, true, true, false),
        EXPERT(AdvancementType.GOAL, true, true, false),
        SECRET(AdvancementType.GOAL, true, true, true);

        private final AdvancementType type;
        private final boolean toast;
        private final boolean announce;
        private final boolean hide;

        TaskType(AdvancementType type, boolean toast, boolean announce, boolean hide) {
            this.type = type;
            this.toast = toast;
            this.announce = announce;
            this.hide = hide;
        }
    }

    public class Builder {
        private TaskType type;
        private boolean externalTrigger;
        private int keyIndex;
        private ItemStack icon;

        private Function<HolderLookup.Provider, ItemStack> func;

        Builder() {
            this.type = TaskType.NORMAL;
        }

        public Builder special(PowerGridAdvancement.TaskType type) {
            this.type = type;
            return this;
        }

        public Builder after(PowerGridAdvancement other) {
            PowerGridAdvancement.this.parent = other;
            return this;
        }

        public Builder icon(ItemProviderEntry<?, ?> item) {
            return icon(item.asStack());
        }

        public Builder icon(ItemLike item) {
            return icon(new ItemStack(item));
        }

        public Builder icon(ItemStack stack) {
            this.icon = stack;
            return this;
        }

        public Builder title(String title) {
            PowerGridAdvancement.this.title = title;
            return this;
        }

        public Builder description(String description) {
            PowerGridAdvancement.this.description = description;
            return this;
        }

        public Builder whenBlockPlaced(Block block) {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        public Builder whenIconCollected() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(this.icon.getItem()));
        }

        public Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
            return whenItemCollected(item.asStack().getItem());
        }

        public Builder whenItemCollected(ItemLike itemProvider) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
        }

        public Builder whenItemCollected(TagKey<Item> tag) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance
                    .hasItems(ItemPredicate.Builder.item().of(tag).build()));
        }

        public Builder awardedForFree() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[0]));
        }

        public Builder externalTrigger(Criterion<?> trigger) {
            PowerGridAdvancement.this.builder.addCriterion(String.valueOf(this.keyIndex), trigger);
            this.externalTrigger = true;
            ++this.keyIndex;
            return this;
        }
    }
}
