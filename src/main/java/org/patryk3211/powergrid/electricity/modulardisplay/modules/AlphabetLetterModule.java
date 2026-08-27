package org.patryk3211.powergrid.electricity.modulardisplay.modules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.modulardisplay.DisplayModuleType;
import org.patryk3211.powergrid.electricity.modulardisplay.IDisplayModule;

public class AlphabetLetterModule implements IDisplayModule {
    private final int Index;
    private final boolean halfClick;
    private final DyeColor color;
    private final int damaged;
    //counts A,B,C,D,E,F...X,Y,Z,Blank,A (first symbol repeated for smooth transition)
    public AlphabetLetterModule(int Index, boolean halfClick, DyeColor color) {
        if (Index < 0 || Index > getDisplayTextureCharacterCount() + 3){
            PowerGrid.LOGGER.warn("Index must be 0-" + (getDisplayTextureCharacterCount() + 3) + ", got: " + Index);
            Index = 0;
        }
        this.Index = Index;
        this.halfClick = halfClick;
        this.color = color;
        this.damaged = 0;
    }

    @Override
    public IDisplayModule withIndex(int newIndex) {
        return new AlphabetLetterModule(newIndex, this.halfClick, this.color);
    }

    @Override
    public IDisplayModule withHalfClick(boolean halfClick) {
        return new AlphabetLetterModule(this.Index, halfClick,  this.color);
    }

    @Override
    public IDisplayModule withColor(DyeColor color) {
        return new AlphabetLetterModule(this.Index, this.halfClick,  color);
    }

    public float getDisplayTextureSize() {
        return 176f;
    }

    public int getDisplayTextureCharacterCount() {
        return 25;
    }

    @Override
    public String getDisplayTexture() {
        return "block/modular_display/alphabet";
    }

    public boolean getHalfClick() {
        return halfClick;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.LETTER;
    }

    @Override
    public DisplayModuleType getDisplayModuleType() {
        return DisplayModuleType.ALPHABET;
    }

    @Override public int getIndex() {
        return Index;
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    @Override public String serialize() {
        return "alphabet:" + Index + ":" + halfClick + ":" + color.getName() + ":" + damaged;
    }
}
