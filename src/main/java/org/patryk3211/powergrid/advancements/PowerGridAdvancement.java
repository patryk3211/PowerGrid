package org.patryk3211.powergrid.advancements;

import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.advancement.SimpleCreateTrigger;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.*;
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
import java.util.function.UnaryOperator;

public class PowerGridAdvancement {
    public static final ResourceLocation BACKGROUND = PowerGrid.texture("gui/advancements");
    public static final String LANG = "advancement.powergrid.";
    public static final String SECRET_SUFFIX = "\n§7(Hidden Advancement)";

    private final Advancement.Builder builder = Advancement.Builder.advancement();
    private SimpleCreateTrigger builtinTrigger;
    private PowerGridAdvancement parent;
    Advancement datagenResult;
    private final String id;
    private String title;
    private String description;

    public PowerGridAdvancement(String id, UnaryOperator<Builder> b) {
        this.id = id;
        PowerGridAdvancement.Builder t = new PowerGridAdvancement.Builder();
        b.apply(t);
        if (!t.externalTrigger) {
            this.builtinTrigger = AllTriggers.addSimple(id + "_builtin");
            this.builder.addCriterion("0", builtinTrigger.instance());
        }

        this.builder.display(t.icon, Component.translatable(titleKey()), Component.translatable(this.descriptionKey()).withStyle((s) -> s.withColor(14393875)), id.equals("root") ? BACKGROUND : null, t.type.frame, t.type.toast, t.type.announce, t.type.hide);
        if (t.type == PowerGridAdvancement.TaskType.SECRET) {
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
            Advancement advancement = sp.getServer().getAdvancements().getAdvancement(PowerGrid.asResource(this.id));
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

    public void save(Consumer<Advancement> t) {
        if (this.parent != null) {
            this.builder.parent(this.parent.datagenResult);
        }

        this.datagenResult = this.builder.save(t, PowerGrid.asResource(this.id).toString());
    }

    public void provideLang(BiConsumer<String, String> consumer) {
        consumer.accept(this.titleKey(), this.title);
        consumer.accept(this.descriptionKey(), this.description);
    }

    public enum TaskType {
        SILENT(FrameType.TASK, false, false, false),
        NORMAL(FrameType.TASK, true, false, false),
        NOISY(FrameType.TASK, true, true, false),
        EXPERT(FrameType.GOAL, true, true, false),
        SECRET(FrameType.GOAL, true, true, true);

        private final FrameType frame;
        private final boolean toast;
        private final boolean announce;
        private final boolean hide;

        TaskType(FrameType frame, boolean toast, boolean announce, boolean hide) {
            this.frame = frame;
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

        Builder() {
            this.type = TaskType.NORMAL;
        }

        public PowerGridAdvancement.Builder special(PowerGridAdvancement.TaskType type) {
            this.type = type;
            return this;
        }

        public PowerGridAdvancement.Builder after(PowerGridAdvancement other) {
            PowerGridAdvancement.this.parent = other;
            return this;
        }

        public PowerGridAdvancement.Builder icon(ItemProviderEntry<?> item) {
            return icon(item.asStack());
        }

        public PowerGridAdvancement.Builder icon(ItemLike item) {
            return icon(new ItemStack(item));
        }

        public PowerGridAdvancement.Builder icon(ItemStack stack) {
            this.icon = stack;
            return this;
        }

        public PowerGridAdvancement.Builder title(String title) {
            PowerGridAdvancement.this.title = title;
            return this;
        }

        public PowerGridAdvancement.Builder description(String description) {
            PowerGridAdvancement.this.description = description;
            return this;
        }

        public PowerGridAdvancement.Builder whenBlockPlaced(Block block) {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        public PowerGridAdvancement.Builder whenIconCollected() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(this.icon.getItem()));
        }

        public PowerGridAdvancement.Builder whenItemCollected(ItemProviderEntry<?> item) {
            return whenItemCollected(item.asStack().getItem());
        }

        public PowerGridAdvancement.Builder whenItemCollected(ItemLike itemProvider) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
        }

        public PowerGridAdvancement.Builder whenItemCollected(TagKey<Item> tag) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemPredicate(tag, null, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, EnchantmentPredicate.NONE, EnchantmentPredicate.NONE, null, NbtPredicate.ANY)));
        }

        public PowerGridAdvancement.Builder awardedForFree() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[0]));
        }

        public PowerGridAdvancement.Builder externalTrigger(CriterionTriggerInstance trigger) {
            PowerGridAdvancement.this.builder.addCriterion(String.valueOf(this.keyIndex), trigger);
            this.externalTrigger = true;
            ++this.keyIndex;
            return this;
        }
    }
}
