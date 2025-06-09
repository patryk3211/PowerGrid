/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.sim;

import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.HashSet;
import java.util.Set;

// This is not realistic in any way, but it's good enough
// for this purpose. It even has "capacitance".
public class TransmissionLine extends ElectricWire {
    public final Set<WireEntity> holders = new HashSet<>();
    private double charge = 0;

    private double prevV1 = 0;
    private double prevV2 = 0;

    private double prevI1 = 0;
    private double prevI2 = 0;

    private ElectricWire node1Wire = null;
    private ElectricWire node2Wire = null;

    public TransmissionLine(double resistance, VoltageSourceNode node1, VoltageSourceNode node2) {
        super(resistance, node1, node2);
    }

    public TransmissionLine() {
        this(1, new VoltageSourceNode(), new VoltageSourceNode());
        // Hack to prevent exception.
        this.resistance = 0;
    }

    public void addHolder(WireEntity wire) {
        holders.add(wire);
        resistance += wire.getResistance();
    }

    public void merge(TransmissionLine line) {
        for(var holder : line.holders) {
            holder.setWire(this);
            addHolder(holder);
        }
        line.holders.clear();
    }

    @Override
    public void remove() {
        GlobalElectricNetworks.removeTransmissionLine(this);
        holders.clear();

        if(node1Wire != null) {
            node1Wire.remove();
            node1Wire = null;
        }
        if(node2Wire != null) {
            node2Wire.remove();
            node2Wire = null;
        }
        var net1 = node1.getNetwork();
        if(net1 != null)
            net1.removeNode(node1);
        var net2 = node2.getNetwork();
        if(net2 != null)
            net2.removeNode(node2);
    }

    public void unassignNode1() {
        if(node1Wire != null) {
            node1Wire.remove();
            node1Wire = null;
        }
        var net = node1.getNetwork();
        if(net != null)
            net.removeNode(node1);
    }

    public void unassignNode2() {
        if(node2Wire != null) {
            node2Wire.remove();
            node2Wire = null;
        }
        var net = node2.getNetwork();
        if(net != null)
            net.removeNode(node2);
    }

    public void unassignNodes() {
        unassignNode1();
        unassignNode2();
    }

    public void connectNode1(IElectricNode target) {
        if(node1Wire != null)
            throw new IllegalStateException("Node 1 is already assigned");
        var wire = new ElectricWire(resistance * 0.5f, node1, target);
        var net = target.getNetwork();
        net.addNode(node1);
        net.addWire(wire);
        node1Wire = wire;
    }

    public void connectNode2(IElectricNode target) {
        if(node2Wire != null)
            throw new IllegalStateException("Node 1 is already assigned");
        var wire = new ElectricWire(resistance * 0.5f, node2, target);
        var net = target.getNetwork();
        net.addNode(node2);
        net.addWire(wire);
        node2Wire = wire;
    }

    public void connect(IElectricNode target) {
        if(node1Wire == null) connectNode1(target);
        else if(node2Wire == null) connectNode2(target);
        else throw new IllegalStateException("Both ends of this transmission line are already assigned");
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        throw new IllegalCallerException("Transmission line cannot be assigned to a network");
    }

    public void tick() {
        assert resistance != 0 : "Resistance must not be zero in transmission line tick";
//        float impedance = (float) resistance * 0.25f;
//
//        var Va_Ref = prevV2;
//        var Va_Fwd = -node1.getCurrent() * impedance;
//        double Va = node1.getVoltage() + Va_Fwd;
//        ((VoltageSourceNode) node1).setVoltage((float) (Va+ Va_Ref));
//
//        var Vb_Ref = prevV1;
//        var Vb_Fwd = -node2.getCurrent() * impedance;
//        double Vb = node2.getVoltage() + Vb_Fwd;
//        ((VoltageSourceNode) node2).setVoltage((float) (Vb+ Vb_Ref));
//
//        prevV1 = Va_Fwd;
//        prevV2 = Vb_Fwd;

//        var V_A = node1.getVoltage() + node2.getCurrent() * resistance * 0.5f;
//        var V_B = node2.getVoltage() + node1.getCurrent() * resistance * 0.5f;
//        charge1 -= node2.getCurrent();
//        charge2 -= node1.getCurrent();

        var deltaI = -(node1.getCurrent() + node2.getCurrent()) * 0.5f;
        charge += deltaI;

        ((VoltageSourceNode) node1).setVoltage((float) (charge * resistance * 0.5f));
        ((VoltageSourceNode) node2).setVoltage((float) (charge * resistance * 0.5f));
    }

    @Override
    public float potentialDifference() {
//        var node1Voltage = node1.getVoltage() - node1.getCurrent() * resistance * 0.25f;
//        var node2Voltage = node2.getVoltage() - node2.getCurrent() * resistance * 0.25f;
        var node1Voltage = -node1.getCurrent() * resistance * 0.5f;
        var node2Voltage = -node2.getCurrent() * resistance * 0.5f;
        return (float) (node1Voltage - node2Voltage);
    }
}
