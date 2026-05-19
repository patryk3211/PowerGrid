package org.patryk3211.powergrid.electricity.modulardisplay;

import org.jetbrains.annotations.Nullable;

public class SlotData {

    @Nullable
    private final IDisplayModule module;

    public SlotData(@Nullable IDisplayModule module) {
        this.module = module;
    }

    public static SlotData empty() {
        return new SlotData(null);
    }

    public boolean isEmpty()    { return module == null; }

    public boolean getHalfClick() { return module != null ? module.getHalfClick() : false;}

    public int getIndex() {
        return module != null ? module.getIndex() : -1;
    }

    @Nullable
    public IDisplayModule getModule() { return module; }
}
