package org.patryk3211.electricity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.sim.GraphedElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;

public class GraphedTest extends TestHelper {
    @Test
    void staticLeafTest() {
        var Net = new Network();

        var V1 = Net.V(5);
        var N = Net.N();

        Net.W(1, V1, N);
        Net.W(1, N, null);

        var N2 = Net.N();

        Net.W(1, N, N2);
        Net.network.makeLeaf(N2, N);

        Net.calculate();

        Assertions.assertEquals(2.5f, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(2.5f, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
    }

    @Test
    void autoLeafTest() {
        var Net = new Network();
        Net.network = new GraphedElectricalNetwork(false);

        var N = Net.N();
        var V1 = Net.V(5);

        Net.W(1, V1, N);
        Net.W(1, N, null);

        var N2 = Net.N();

        Net.W(1, N, N2);

        Net.calculate();

        Assertions.assertTrue(Net.network.isLeaf(N2), "Node 2 didn't get detected as a leaf node");
        Assertions.assertEquals(2.5f, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(2.5f, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
    }

    @Test
    void longBranchCutting() {
        var Net = new Network();
        Net.network = new GraphedElectricalNetwork(false);

        var N = Net.N();
        var V1 = Net.V(5);

        Net.W(1, V1, N);
        Net.W(1, N, null);

        var N2 = Net.N();
        var N3 = Net.N();
        var N4 = Net.N();

        Net.W(1, N, N2);
        Net.W(1, N2, N3);
        Net.W(1, N3, N4);

        Net.calculate();

        Assertions.assertTrue(Net.network.isLeaf(N2), "Node 2 didn't get detected as a leaf node");
        Assertions.assertTrue(Net.network.isLeaf(N3), "Node 3 didn't get detected as a leaf node");
        Assertions.assertTrue(Net.network.isLeaf(N4), "Node 4 didn't get detected as a leaf node");
        Assertions.assertEquals(2.5f, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(2.5f, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(2.5f, N3.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(2.5f, N4.getVoltage(), 1e-6f, "Leaf node value is invalid");
    }

    @Test
    void branchReconnect() {
        var Net = new Network();
        Net.network = new GraphedElectricalNetwork(false);

        var N = Net.N();
        var V1 = Net.V(5);

        Net.W(1, V1, N);
        Net.W(1, N, null);

        var N2 = Net.N();
        var N3 = Net.N();
        var N4 = Net.N();

        Net.W(1, N, N2);
        Net.W(1, N2, N3);
        Net.W(1, N3, N4);

        Net.calculate();

        Assertions.assertTrue(Net.network.isLeaf(N2), "Node 2 didn't get detected as a leaf node");
        Assertions.assertTrue(Net.network.isLeaf(N3), "Node 3 didn't get detected as a leaf node");
        Assertions.assertTrue(Net.network.isLeaf(N4), "Node 4 didn't get detected as a leaf node");
        Assertions.assertEquals(2.5f, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(2.5f, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(2.5f, N3.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(2.5f, N4.getVoltage(), 1e-6f, "Leaf node value is invalid");

        // Reconnect with a single node still floating
        var wire = Net.W(1, N3, null);

        Net.calculate();

        Assertions.assertFalse(Net.network.isLeaf(N2), "Node 2 didn't get reconnected");
        Assertions.assertFalse(Net.network.isLeaf(N3), "Node 3 didn't get reconnected");
        Assertions.assertTrue(Net.network.isLeaf(N4), "Node 4 didn't get detected as a leaf node");
        var V_N1 = 5 * (0.75 / 1.75);
        Assertions.assertEquals(V_N1, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(V_N1 * 2 / 3, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(V_N1 * 1 / 3, N3.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(V_N1 * 1 / 3, N4.getVoltage(), 1e-6f, "Leaf node value is invalid");
    }

    @Test
    void branchDisconnect() {
        var Net = new Network();
        Net.network = new GraphedElectricalNetwork(false);

        var N = Net.N();
        var V1 = Net.V(5);

        Net.W(1, V1, N);
        Net.W(1, N, null);

        var N2 = Net.N();
        var N3 = Net.N();
        var N4 = Net.N();

        Net.W(1, N, N2);
        Net.W(1, N2, N3);
        Net.W(1, N3, N4);

        // Reconnect with a single node still floating
        var wire = Net.W(1, N3, null);

        Net.calculate();

        Assertions.assertFalse(Net.network.isLeaf(N2), "Node 2 didn't get reconnected");
        Assertions.assertFalse(Net.network.isLeaf(N3), "Node 3 didn't get reconnected");
        Assertions.assertTrue(Net.network.isLeaf(N4), "Node 4 didn't get detected as a leaf node");
        var V_N1 = 5 * (0.75 / 1.75);
        Assertions.assertEquals(V_N1, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(V_N1 * 2 / 3, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(V_N1 * 1 / 3, N3.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(V_N1 * 1 / 3, N4.getVoltage(), 1e-6f, "Leaf node value is invalid");

        wire.remove();
        Net.calculate();

        Assertions.assertTrue(Net.network.isLeaf(N2), "Node 2 didn't get detected as a leaf node");
        Assertions.assertTrue(Net.network.isLeaf(N3), "Node 3 didn't get detected as a leaf node");
        Assertions.assertTrue(Net.network.isLeaf(N4), "Node 4 didn't get detected as a leaf node");
        Assertions.assertEquals(2.5f, N.getVoltage(), 1e-6f, "Invalid divider voltage");
        Assertions.assertEquals(2.5f, N2.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(2.5f, N3.getVoltage(), 1e-6f, "Leaf node value is invalid");
        Assertions.assertEquals(2.5f, N4.getVoltage(), 1e-6f, "Leaf node value is invalid");
    }

    @Test
    void loopTest() {
        var Net = new Network();
        Net.network = new GraphedElectricalNetwork(false);

        var N = Net.N();
        var V1 = Net.V(5);

        Net.W(1, V1, N);
        Net.W(1, N, null);

        int loopSize = 10;
        var nodes = new FloatingNode[loopSize];
        for(int i = 0; i < loopSize; ++i) {
            nodes[i] = Net.N();
            if(i > 0) {
                Net.W(1, nodes[i - 1], nodes[i]);
            }
        }
        // Finish the loop
        Net.W(1, nodes[0], nodes[loopSize - 1]);

        Net.W(1, nodes[0], N);
        Net.W(1, nodes[loopSize / 2], null);

        Net.calculate();
    }
}
