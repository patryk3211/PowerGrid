package org.patryk3211.powergrid.electricity.modulardisplay.modules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.modulardisplay.DisplayModuleType;
import org.patryk3211.powergrid.electricity.modulardisplay.IDisplayModule;

public class SymbolLetterModule implements IDisplayModule {
    private final int Index;
    private final boolean halfClick;
    private final DyeColor color;
    //counts .,comma,<,>,=,+,-,x,/,blank,. (first symbol repeated for smooth transition)
    public SymbolLetterModule(int Index, boolean halfClick, DyeColor color){
        if (Index < 0 || Index > getDisplayTextureCharacterCount() + 3){
            PowerGrid.LOGGER.warn("Index must be 0-" + (getDisplayTextureCharacterCount() + 3) + ", got: " + Index);
            Index = 0;
        }
        this.Index = Index;
        this.halfClick = halfClick;
        this.color = color;
    }

    @Override
    public IDisplayModule withIndex(int newIndex) {
        return new SymbolLetterModule(newIndex, this.halfClick, this.color);
    }

    @Override
    public IDisplayModule withHalfClick(boolean halfClick) {
        return new SymbolLetterModule(this.Index, halfClick, this.color);
    }

    @Override
    public IDisplayModule withColor(DyeColor color) {
        return new SymbolLetterModule(this.Index, this.halfClick, color);
    }

    public float getDisplayTextureSize() {
        return 80f;
    }

    public int getDisplayTextureCharacterCount() {
        return 8;
    }

    @Override
    public String getDisplayTexture() {
        return "block/modular_display/symbols";
    }

    public DisplayModuleType getDisplayModuleType() {
        return DisplayModuleType.SYMBOLS;
    }

    public boolean getHalfClick() {
        return halfClick;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.LETTER;
    }

    @Override public int getIndex() {
        return Index;
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    @Override public String serialize() {
        return "symbol:" + Index + ":" + halfClick + ":" + color;
    }
}
