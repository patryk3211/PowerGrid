package org.patryk3211.powergrid.electricity.solarpanel;

public interface ISolarPropertyConsumer {
    void accept(double Rs, double Rsh, double I);
}
