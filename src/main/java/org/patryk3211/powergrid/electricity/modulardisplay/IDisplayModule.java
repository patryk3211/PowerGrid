package org.patryk3211.powergrid.electricity.modulardisplay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public interface IDisplayModule {

    enum ModuleType {
        CUSTOM,
        DIGIT,                      // pure numbers
        LETTER,                     // pure letters
        ALPHANUMERIC,               // number letter mix
    }

    ModuleType getType();

    DisplayModuleType getDisplayModuleType();

    default boolean getHalfClick() {return false;}

    default int getIndex() {return -1;}

    default DyeColor getColor() {return DyeColor.WHITE;}

    IDisplayModule withIndex(int newIndex);

    IDisplayModule withHalfClick(boolean halfClick);

    IDisplayModule withColor(DyeColor color);

    default ResourceLocation getDisplayTexture() {return null;}

    default float getDisplayTextureSize() {return 0;}

    default int getDisplayTextureCharacterCount() {return 0;}

    IDisplayModule isDamaged(); //todo Implement damage system if required

    String serialize();

}
