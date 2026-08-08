package org.patryk3211.powergrid.electricity.modulardisplay.modules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.modulardisplay.DisplayModuleType;
import org.patryk3211.powergrid.electricity.modulardisplay.IDisplayModule;

public class HexadecimalAlphanumericModule implements IDisplayModule {
    private final int Index;
    private final boolean halfClick;
    private final DyeColor color;
    //counts 0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,blank,0 (first number repeated for smooth transition)
    public HexadecimalAlphanumericModule(int Index, boolean halfClick, DyeColor color) {
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
        return new HexadecimalAlphanumericModule(newIndex, this.halfClick, this.color);
    }

    @Override
    public IDisplayModule withHalfClick(boolean halfClick) {
        return new HexadecimalAlphanumericModule(this.Index, halfClick, this.color);
    }

    @Override
    public IDisplayModule withColor(DyeColor color) {
        return new HexadecimalAlphanumericModule(this.Index, this.halfClick, color);
    }

    public float getDisplayTextureSize() {
        return 112f;
    }

    public int getDisplayTextureCharacterCount() {
        return 15;
    }

    @Override
    public ResourceLocation getDisplayTexture() {
        return PowerGrid.texture("block/modular_display/zerotof");
    }

    public DisplayModuleType getDisplayModuleType() {
        return DisplayModuleType.HEXADECIMAL;
    }

    public boolean getHalfClick() {
        return halfClick;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.ALPHANUMERIC;
    }

    @Override
    public int getIndex() {
        return Index;
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    @Override
    public String serialize() {
        return "hexadecimal:" + Index + ":" + halfClick + ":" + color;
    }

}