/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.DummyElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.NetworkGraph;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.ImaginaryWireEndpoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TransmissionLineRecoveryTests {
    @Test
    void verifiedScanBlocksTopologyResolutionUntilFinalAssembly() {
        Assertions.assertTrue(WorldNetworks.transmissionTopologyResolutionAllowed(false, false));
        Assertions.assertFalse(WorldNetworks.transmissionTopologyResolutionAllowed(true, false));
        Assertions.assertTrue(WorldNetworks.transmissionTopologyResolutionAllowed(true, true));
    }

    @Test
    void ordinaryEndpointBindingCannotReenterTransmissionLineSplitting() {
        Assertions.assertFalse(WorldNetworks.shouldSplitAfterNodeBinding(
                WorldNetworks.NodeBindingMode.BIND_ONLY,
                true
        ));
        Assertions.assertFalse(WorldNetworks.shouldSplitAfterNodeBinding(
                WorldNetworks.NodeBindingMode.BIND_AND_SPLIT,
                false
        ));
        Assertions.assertTrue(WorldNetworks.shouldSplitAfterNodeBinding(
                WorldNetworks.NodeBindingMode.BIND_AND_SPLIT,
                true
        ));
    }

    @Test
    void missingPartsWaitForTheEntityLoadGracePeriod() {
        var check = new WorldNetworks.CheckChunk();

        for(int tick = 0; tick < WorldNetworks.ENTITY_LOAD_GRACE_TICKS; ++tick) {
            Assertions.assertFalse(check.advanceEntityLoadCheck());
        }
        Assertions.assertTrue(check.advanceEntityLoadCheck());

        check.resetTicks();
        Assertions.assertFalse(check.advanceEntityLoadCheck());
    }

    @Test
    void transmissionLineGrabUsesTheCompletePartId() throws NoSuchMethodException {
        var entityId = UUID.randomUUID();
        var simpleId = new WorldNetworks.SimpleId(entityId);
        var firstComplexPart = new WorldNetworks.ComplexId(entityId, 0);
        var secondComplexPart = new WorldNetworks.ComplexId(entityId, 1);

        Assertions.assertEquals(simpleId, new WorldNetworks.SimpleId(entityId));
        Assertions.assertNotEquals(simpleId, entityId);
        Assertions.assertNotEquals(firstComplexPart, secondComplexPart);

        Assertions.assertNotNull(TransmissionLine.class.getDeclaredMethod(
                "grabPart",
                BaseWireEntity.class,
                WorldNetworks.PartId.class,
                TransmissionLinePart.class
        ));
    }

    @Test
    void rebuiltTransmissionLinesAcceptAlreadyResolvedNonNullNodes() throws NoSuchMethodException {
        Assertions.assertNotNull(TransmissionLine.class.getDeclaredConstructor(
                double.class,
                org.patryk3211.powergrid.electricity.wire.IWireEndpoint.class,
                org.patryk3211.powergrid.electricity.wire.IWireEndpoint.class,
                OwnedFloatingNode.class,
                OwnedFloatingNode.class,
                WorldNetworks.class
        ));
        Assertions.assertNotNull(TransmissionLine.class.getDeclaredMethod("refreshDetachedEndpointNodes"));
        Assertions.assertNotNull(WorldNetworks.class.getDeclaredMethod(
                "addPreparedTransmissionLine",
                org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.class,
                TransmissionLine.class
        ));
    }

    @Test
    void loadedEndpointNodeSupersedesItsPersistedPlaceholder() {
        var endpoint = new ImaginaryWireEndpoint(Vec3.ZERO);
        var placeholder = new OwnedFloatingNode(endpoint);
        var loadedNode = new OwnedFloatingNode(endpoint);

        var selected = WorldNetworks.selectLoadedOrIndexedNode(endpoint, placeholder, loadedNode);
        Assertions.assertSame(loadedNode, selected);
    }

    @Test
    void proxyEndpointCanResolveToANodeWithADifferentCanonicalEndpoint() {
        var endpoint = new ImaginaryWireEndpoint(Vec3.ZERO);
        var canonicalEndpoint = new ImaginaryWireEndpoint(new Vec3(1, 2, 3));
        var proxyNode = new OwnedFloatingNode(canonicalEndpoint);

        Assertions.assertSame(
                proxyNode,
                WorldNetworks.selectLoadedOrIndexedNode(endpoint, null, proxyNode)
        );
        Assertions.assertSame(
                proxyNode,
                WorldNetworks.selectLoadedOrIndexedNode(endpoint, proxyNode, null)
        );
    }

    @Test
    void transmissionTreeTraversalSelectsTheSegmentSideByCanonicalNodeIdentity() {
        var physicalAlias = new ImaginaryWireEndpoint(Vec3.ZERO);
        var canonicalEndpoint = new ImaginaryWireEndpoint(new Vec3(1, 2, 3));
        var otherEndpoint = new ImaginaryWireEndpoint(new Vec3(4, 5, 6));
        var canonicalNode = new OwnedFloatingNode(canonicalEndpoint);
        var otherNode = new OwnedFloatingNode(otherEndpoint);

        Assertions.assertNotEquals(physicalAlias, canonicalNode.endpoint);
        Assertions.assertEquals(
                WorldNetworks.TransmissionPartSide.FIRST,
                WorldNetworks.transmissionPartSide(
                        canonicalNode,
                        otherNode,
                        canonicalNode
                )
        );
        Assertions.assertEquals(
                WorldNetworks.TransmissionPartSide.SECOND,
                WorldNetworks.transmissionPartSide(
                        canonicalNode,
                        otherNode,
                        otherNode
                )
        );
        Assertions.assertEquals(
                WorldNetworks.TransmissionPartSide.NONE,
                WorldNetworks.transmissionPartSide(
                        canonicalNode,
                        otherNode,
                        new OwnedFloatingNode(physicalAlias)
                )
        );
    }

    @Test
    void missingEndpointIndexCreatesTheCorrectPlaceholder() {
        var endpoint = new ImaginaryWireEndpoint(Vec3.ZERO);

        var placeholder = WorldNetworks.selectLoadedOrIndexedNode(endpoint, null, null);
        Assertions.assertSame(endpoint, placeholder.endpoint);
    }

    @Test
    void unloadedEndpointMovesAliasesAndPhysicalPartsToItsPlaceholder() {
        var canonicalEndpoint = new ImaginaryWireEndpoint(Vec3.ZERO);
        var physicalAlias = new ImaginaryWireEndpoint(new Vec3(1, 2, 3));
        var unrelatedEndpoint = new ImaginaryWireEndpoint(new Vec3(4, 5, 6));
        var loadedNode = new OwnedFloatingNode(canonicalEndpoint);
        var placeholder = new OwnedFloatingNode(canonicalEndpoint);
        var unrelatedNode = new OwnedFloatingNode(unrelatedEndpoint);
        Map<org.patryk3211.powergrid.electricity.wire.IWireEndpoint, OwnedFloatingNode> endpointBindings =
                new HashMap<>();
        endpointBindings.put(canonicalEndpoint, loadedNode);
        endpointBindings.put(physicalAlias, loadedNode);
        endpointBindings.put(unrelatedEndpoint, unrelatedNode);
        Map<OwnedFloatingNode, Set<String>> partIndex = new HashMap<>();
        partIndex.put(loadedNode, new HashSet<>(Set.of("remote-segment")));
        partIndex.put(placeholder, new HashSet<>(Set.of("existing-segment")));

        WorldNetworks.rebindNodeAliasesByIdentity(endpointBindings, loadedNode, placeholder);
        var movedParts = WorldNetworks.moveNodeIndex(partIndex, loadedNode, placeholder);

        Assertions.assertSame(placeholder, endpointBindings.get(canonicalEndpoint));
        Assertions.assertSame(placeholder, endpointBindings.get(physicalAlias));
        Assertions.assertSame(unrelatedNode, endpointBindings.get(unrelatedEndpoint));
        Assertions.assertFalse(partIndex.containsKey(loadedNode));
        Assertions.assertEquals(
                Set.of("remote-segment", "existing-segment"),
                partIndex.get(placeholder)
        );
        Assertions.assertEquals(Set.of("remote-segment"), Set.copyOf(movedParts));
    }

    @Test
    void replacingAnUnloadedEndpointKeepsItsDerivedWireConnected() {
        var endpoint = new ImaginaryWireEndpoint(Vec3.ZERO);
        var otherEndpoint = new ImaginaryWireEndpoint(new Vec3(1, 2, 3));
        var loadedNode = new OwnedFloatingNode(endpoint);
        var placeholder = new OwnedFloatingNode(endpoint);
        var otherNode = new OwnedFloatingNode(otherEndpoint);
        var network = new DummyElectricalNetwork(new NetworkGraph());
        network.addNode(loadedNode);
        network.addNode(otherNode);
        var wire = new ElectricWire(1, loadedNode, otherNode);
        network.addWire(wire);

        network.addNode(placeholder);
        wire.setNode1(placeholder);
        network.removeNode(loadedNode);

        Assertions.assertSame(network, wire.getNetwork());
        Assertions.assertSame(placeholder, wire.getNode1());
        Assertions.assertSame(otherNode, wire.getNode2());
        Assertions.assertTrue(network.ownsNode(placeholder));
        Assertions.assertFalse(network.ownsNode(loadedNode));
    }

    @Test
    void exactPreparedNodeIsAddedWhenItsNetworkPointerIsStale() {
        var endpoint = new ImaginaryWireEndpoint(Vec3.ZERO);
        var node = new OwnedFloatingNode(endpoint);
        var staleNetwork = new DummyElectricalNetwork(new NetworkGraph());
        var targetNetwork = new DummyElectricalNetwork(new NetworkGraph());
        node.setNetwork(staleNetwork);

        WorldNetworks.addOrMergeExactNode(targetNetwork, node);

        Assertions.assertSame(targetNetwork, node.getNetwork());
        Assertions.assertTrue(targetNetwork.ownsNode(node));
    }

    @Test
    void exactPreparedNodeMergesFromARealDifferentNetwork() {
        var endpoint = new ImaginaryWireEndpoint(Vec3.ZERO);
        var node = new OwnedFloatingNode(endpoint);
        var originalNetwork = new DummyElectricalNetwork(new NetworkGraph());
        var targetNetwork = new DummyElectricalNetwork(new NetworkGraph());
        originalNetwork.addNode(node);

        WorldNetworks.addOrMergeExactNode(targetNetwork, node);

        Assertions.assertSame(targetNetwork, node.getNetwork());
        Assertions.assertTrue(targetNetwork.ownsNode(node));
        Assertions.assertFalse(originalNetwork.ownsNode(node));
    }

    @Test
    void physicalOwnerEndpointPairsMatchInEitherDirectionOnly() {
        var first = new BlockWireEndpoint(new BlockPos(1, 2, 3), 0);
        var second = new BlockWireEndpoint(new BlockPos(4, 5, 6), 1);
        var other = new BlockWireEndpoint(new BlockPos(7, 8, 9), 0);

        Assertions.assertTrue(TransmissionLinePart.sameEndpoints(first, second, first, second));
        Assertions.assertTrue(TransmissionLinePart.sameEndpoints(first, second, second, first));
        Assertions.assertFalse(TransmissionLinePart.sameEndpoints(first, second, first, other));
        Assertions.assertFalse(TransmissionLinePart.sameEndpoints(first, second, first, first));
    }
}
