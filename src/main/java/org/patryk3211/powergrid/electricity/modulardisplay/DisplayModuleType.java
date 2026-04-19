package org.patryk3211.powergrid.electricity.modulardisplay;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.utility.Lang;

import java.util.function.IntFunction;

public enum DisplayModuleType implements INamedIconOptions {
    ZERO_TO_NINE(0, Lang.translateDirect("gui.modular_display.0 - 9").getString(), "zerotonine", 80f, 9f, AllIcons.I_NONE),
    NINE_TO_ZERO(1, Lang.translateDirect("gui.modular_display.9 - 0").getString(), "ninetozero", 80f, 9f, AllIcons.I_NONE),
    ONE_TO_ZERO(2, Lang.translateDirect("gui.modular_display.1 - 0").getString(), "onetozero", 80f, 9f, AllIcons.I_NONE),
    HEXADECIMAL(3, Lang.translateDirect("gui.modular_display.hexadecimal").getString(), "zerotof", 112f, 15f, AllIcons.I_NONE),
    SYMBOLS(4, Lang.translateDirect("gui.modular_display.symbols").getString(), "symbols", 80f, 8f, AllIcons.I_NONE),
    ALPHABET(5, Lang.translateDirect("gui.modular_display.alphabet").getString(), "alphabet", 176f, 25f, AllIcons.I_NONE);

    private static final IntFunction<DisplayModuleType> BY_ID = ByIdMap.continuous(DisplayModuleType::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    private final int id;

    private final String name;
    private final String displayTexture;
    private final AllIcons icon;
    private final float spriteWidth;
    private final float characterCount;

    DisplayModuleType(int id, String name, String displayTexture, float spriteWidth, float characterCount, AllIcons icon) {
        this.id = id;
        this.name = name;
        this.displayTexture = displayTexture;
        this.spriteWidth = spriteWidth;
        this.characterCount = characterCount;
        this.icon = icon;
    }

    public int getId() {
        return this.id;
    }

    public String getDisplayTexture() {
        return this.displayTexture;
    }

    public float getSpriteWidth() {
        return this.spriteWidth;
    }

    public float getCharacterCount() {
        return this.characterCount;
    }

    public static DisplayModuleType byId(int colorId) {
        return (DisplayModuleType)BY_ID.apply(colorId);
    }

    @Override
    public String getTranslationKey() {
        return name;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }
}