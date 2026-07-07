package org.patryk3211.powergrid.electricity.sim.special;

import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

public class SamplingWire extends ElectricWire implements ElectricBehaviour.SyncAppender {
    public double[] samples;
    private int currentTick;

    public SamplingWire(double resistance, IElectricNode node1, IElectricNode node2) {
        super(resistance, node1, node2);
    }

    @Override
    public void prepare(int multiTicks) {
        super.prepare(multiTicks);
        if(multiTicks <= 1) {
            samples = null;
        } else if(samples == null || samples.length != multiTicks) {
            samples = new double[multiTicks];
        }
        currentTick = 0;
    }

    @Override
    public void postMicroTick() {
        super.postMicroTick();
        samples[currentTick++] = potentialDifference();
    }

    @Override
    public void writeToSync(FriendlyByteBuf buffer) {
        if(samples == null || network == null) {
            buffer.writeInt(0);
            return;
        }
        buffer.writeInt(samples.length);
        for(int i = 0; i < samples.length; ++i) {
            buffer.writeDouble(samples[i]);
        }
    }

    @Override
    public void readFromSync(FriendlyByteBuf buffer) {
        int length = buffer.readInt();
        if(length == 0) {
            samples = null;
            return;
        }
        if(samples == null || samples.length != length)
            samples = new double[length];
        for(int i = 0; i < length; ++i) {
            samples[i] = buffer.readDouble();
        }
    }
}
