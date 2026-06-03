package org.patryk3211.powergrid.electricity.solarpanel;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

public interface IMultiBlockSolarPanel extends IMultiBlockEntityContainer {
    int getSize();
    void updateBehaviour();
    void setCapacitySize(int blocks);
    void markRewire();
}
