package org.patryk3211.powergrid.electricity.modulardisplay.modules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.modulardisplay.DisplayModuleType;
import org.patryk3211.powergrid.electricity.modulardisplay.IDisplayModule;

public class NineToZeroNumberModule implements IDisplayModule {
    private final int Index;
    private final boolean halfClick;
    private final DyeColor color;
    //counts 9,8,7,6,5,4,3,2,1,0,blank,9 (first number repeated for smooth transition)
    public NineToZeroNumberModule(int Index, boolean halfClick, DyeColor color) {
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
        return new NineToZeroNumberModule(newIndex, this.halfClick, this.color);
    }

    @Override
    public IDisplayModule withHalfClick(boolean halfClick) {
        return new NineToZeroNumberModule(this.Index, halfClick, this.color);
    }

    @Override
    public IDisplayModule withColor(DyeColor color) {
        return new NineToZeroNumberModule(this.Index, this.halfClick, color);
    }

    @Override
    public IDisplayModule withDamaged(int damaged) {
        return null;
    }

    @Override
    public ResourceLocation getDisplayTexture() {
        return PowerGrid.texture("block/modular_display/ninetozero");
    }

    public float getDisplayTextureSize() {
        return 80f;
    }

    public int getDisplayTextureCharacterCount() {
        return 9;
    }

    public DisplayModuleType getDisplayModuleType() {
        return DisplayModuleType.NINE_TO_ZERO;
    }

    public boolean getHalfClick() {
        return halfClick;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.DIGIT;
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
        return "ninetozero:" + Index + ":" + halfClick + ":" + color;
    }

}
