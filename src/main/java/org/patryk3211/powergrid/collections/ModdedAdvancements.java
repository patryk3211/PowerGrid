package org.patryk3211.powergrid.collections;

import com.google.common.collect.Sets;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.advancements.PowerGridAdvancement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.patryk3211.powergrid.advancements.PowerGridAdvancement.TaskType.*;

public class ModdedAdvancements implements DataProvider {
    public static final List<PowerGridAdvancement> ENTRIES = new ArrayList<>();
    public static final PowerGridAdvancement

    ROOT = create("root", b -> b
            .icon(ModdedBlocks.ELECTRIC_MOTOR)
            .title("Welcome to Power Grid")
            .description("Don't electrocute yourself")
            .special(SILENT)
            .awardedForFree()),

    /* -------======= Crafting =======------- */
    ELECTRICAL_AGE = create("conductive_casing", b -> b
            .icon(ModdedBlocks.CONDUCTIVE_CASING)
            .title("The Electrical Age")
            .description("Apply Zinc Ingot to Andesite Casing, creating a casing for your electrical machines")
            .whenIconCollected()
            .after(ROOT)
            .special(NOISY)),
    ELECTRICAL_GIZMO = create("electrical_gizmo", b -> b
            .icon(ModdedItems.ELECTRICAL_GIZMO)
            .title("Electronic wonders")
            .description("Obtain an Electrical Gizmo")
            .special(NOISY)
            .after(ELECTRICAL_AGE)
            .whenIconCollected()),
    MAGNET = create("magnet", b -> b
            .icon(ModdedItems.MAGNET)
            .title("Magic metal")
            .description("Obtain a Magnet")
            .special(NOISY)
            .after(ELECTRICAL_AGE)
            .whenIconCollected()),
    ZAPPER = create("zapper", b -> b
            .icon(ModdedItems.ELECTROZAPPER)
            .title("I cast lightning bolt! ...gun")
            .description("Obtain an Electron-Zapper")
            .after(ELECTRICAL_GIZMO)
            .whenIconCollected()),
    ACID = create("acid", b -> b
            .icon(ModdedFluids.acid().getBucket())
            .title("Evil water")
            .description("Make acid")
            .after(ROOT)
            .whenIconCollected()),

    /* -------======= Custom Triggers =======------- */
    BLOW_UP = create("blow_up", b -> b
            .icon(Items.TNT)
            .title("My mango, is to blow up")
            .description("Make something explode (be more careful next time)")
            .special(NOISY)
            .after(ELECTRICAL_AGE)),
    LIGHTNING = create("lightning", b -> b
            .icon(Items.LIGHTNING_ROD)
            .title("Poking grey clouds")
            .description("Trigger a thunder strike with a thunder attractor")
            .after(ROOT)),
    ELECTRIC_MOTOR = create("electric_motor", b -> b
            .icon(ModdedBlocks.ELECTRIC_MOTOR)
            .title("Stress through wires")
            .description("Place and activate an Electrical Motor")
            .after(MAGNET)),
    BOOSTING_CHIP = create("boosting_chip", b -> b
            .icon(ModdedItems.INTEGRATED_CIRCUIT)
            .title("To another level")
            .description("Use a boosting chip")
            .after(ELECTRICAL_GIZMO)),
    TRANSFORMER = create("transformer", b -> b
            .icon(ModdedBlocks.TRANSFORMER_CORE)
            .title("AC/DC wannabe")
            .description("Assemble and power a transformer")
            .after(ELECTRICAL_AGE)),
    GAUGE = create("gauge", b -> b
            .icon(ModdedBlocks.VOLTAGE_METER)
            .title("Monitoring the electrons")
            .description("Use an electric gauge")
            .after(ELECTRICAL_AGE)),

    /* -------======= Ooo, very ~secret~, don't look here =======------- */
    POTATO_BATTERY = create("potato_battery", b -> b
            .after(ROOT)
            .icon(Items.BAKED_POTATO)
            .title("Innovative kitchen")
            .description("Cook a Potato Battery")
            .special(SECRET)),
    WIRE_CUT = create("wire_cut", b -> b
            .after(ROOT)
            .icon(Items.SHEARS)
            .title("Wrong tool for the job")
            .description("Use shears to cut a wire")
            .special(SECRET))

    ;

    private static PowerGridAdvancement create(String id, UnaryOperator<PowerGridAdvancement.Builder> b) {
        return new PowerGridAdvancement(id, b);
    }

    private final PackOutput output;

    public ModdedAdvancements(PackOutput output) {
        this.output = output;
    }


    @NotNull
    @Override
    public CompletableFuture<?> run(@NotNull CachedOutput cache) {
        PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Set<ResourceLocation> set = Sets.newHashSet();
        Consumer<Advancement> consumer = (advancementx) -> {
            ResourceLocation id = advancementx.getId();
            if (!set.add(id)) {
                throw new IllegalStateException("Duplicate advancement " + id);
            } else {
                Path path = pathProvider.json(id);
                futures.add(DataProvider.saveStable(cache, advancementx.deconstruct().serializeToJson(), path));
            }
        };

        for(var advancement : ENTRIES) {
            advancement.save(consumer);
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public String getName() {
        return "Power Grid's Advancements";
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for(var advancement : ENTRIES) {
            advancement.provideLang(consumer);
        }
    }

    public static void register() { }
}
