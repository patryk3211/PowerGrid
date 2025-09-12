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

import org.ejml.data.DMatrixRMaj;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.solver.BiCGSTABSolver;
import org.patryk3211.powergrid.electricity.sim.solver.DynamicallyTypedMatrix;
import org.patryk3211.powergrid.electricity.sim.solver.ISolver;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;
import org.patryk3211.powergrid.electricity.sim.special.CapacitorWire;
import org.slf4j.Logger;

import java.util.*;

public class ElectricalNetwork {
    private static final double PRECISION = 1e-7;
    private static final PerformanceCounter PERF = new PerformanceCounter("NetSolve");

    private final Set<AbstractElectricWire> wires = new HashSet<>();
    private final Set<ICouplingNode> couplings = new HashSet<>();
    private final List<INode> nodes = new ArrayList<>();

    private final ISolver solver;
    private boolean[] voltageSources;
    private DMatrixRMaj conductanceMatrix;
    private DynamicallyTypedMatrix AMatrix;
    private DMatrixRMaj currentMatrix;
    private int sourceCount;

    private DMatrixRMaj lastGuess;

    private boolean dirty;
    private double conductanceDelta = 0;
    private int conductanceUpdates = 0;

    public static Logger LOGGER = null;

    public ElectricalNetwork() {
        solver = new BiCGSTABSolver(PRECISION);
        dirty = true;
        sourceCount = 0;
    }

    // Make sure all variables are completely rebuilt and repopulated.
    public void setDirty() {
        this.dirty = true;
    }

    public boolean hasHooks() {
        return !solver.getHooks().isEmpty();
    }

    public void addNode(INode node) {
        if(node instanceof IElectricNode enode)
            addNode(enode);
        else if(node instanceof ICouplingNode cnode)
            addNode(cnode);
        else
            throw new IllegalArgumentException("Unsupported node type provided");
    }

    public void addNode(IElectricNode node) {
        if(nodes.contains(node))
            return;
        node.assignIndex(nodes.size());
        node.setNetwork(this);
        nodes.add(node);
        setDirty();

        if(node instanceof VoltageSourceNode || node instanceof CurrentSourceNode)
            ++sourceCount;
        if(node instanceof ISolverHook hook)
            solver.addHook(hook);
    }

    public void addNodes(IElectricNode... nodes) {
        for(var node : nodes)
            addNode(node);
    }

    public void removeNode(INode node) {
        if(node == null)
            return;
        if(node.getNetwork() != this || node.getIndex() >= nodes.size() || nodes.get(node.getIndex()) != node)
            // This node is not actually in this network.
            return;

        if(nodes.size() > 1) {
            // Move last node into the place of removed node to prevent holes in the array.
            var last = nodes.get(nodes.size() - 1);
            nodes.set(node.getIndex(), last);
            nodes.remove(nodes.size() - 1);
            last.assignIndex(node.getIndex());
        } else {
            // This is the only node so it's ok to just remove it.
            nodes.remove(node);
        }

        if(node instanceof ICouplingNode)
            couplings.remove(node);
        if(node instanceof VoltageSourceNode || node instanceof CurrentSourceNode || node instanceof VoltageSourceCoupling)
            --sourceCount;
        if(node instanceof ISolverHook hook)
            solver.removeHook(hook);

        node.setNetwork(null);
        setDirty();
    }

    public void removeNode(int index) {
        if(nodes.size() <= index)
            return;

        removeNode(nodes.get(index));
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public ConductanceMatrixAccess conductanceAccess() {
        return new ConductanceMatrixAccess();
    }

    public void addWire(AbstractElectricWire wire) {
        if(wire.node1 != null && !nodes.contains(wire.node1)) {
            // If node of a wire is not null it must be in the network's node set.
            var suffix = wire.node1.getNetwork() == null ? "no network" : "different network";
            throw new IllegalArgumentException("Both nodes of a wire must be part of the network (node1 " + wire.node1 + " isn't - " + suffix + ")");
        }
        if(wire.node2 != null && !nodes.contains(wire.node2)) {
            // If node of a wire is not null it must be in the network's node set.
            var suffix = wire.node2.getNetwork() == null ? "no network" : "different network";
            throw new IllegalArgumentException("Both nodes of a wire must be part of the network (node2 " + wire.node2 + " isn't - " + suffix + ")");
        }
        wire.setNetwork(this);
        wires.add(wire);
        if(wire instanceof CapacitorWire)
            ++sourceCount;

        updateConductance(wire, wire.conductance());
        if(wire instanceof ISolverHook hook)
            solver.addHook(hook);
    }

    public void updateConductance(AbstractElectricWire wire, double change) {
        if(conductanceMatrix == null || dirty || change == 0)
            return;

        conductanceDelta += Math.abs(change);
        ++conductanceUpdates;
        if(wire.node1 != null && wire.node2 != null) {
            var index1 = wire.node1.getIndex();
            var index2 = wire.node2.getIndex();
            conductanceMatrix.add(index1, index1, change);
            conductanceMatrix.add(index2, index2, change);
            conductanceMatrix.add(index1, index2, -change);
            conductanceMatrix.add(index2, index1, -change);
            if(!voltageSources[index1]) {
                AMatrix.add(index1, index1, change);
                AMatrix.add(index2, index1, -change);
            } else {
                var U = ((VoltageSourceNode) nodes.get(index1)).getVoltage();
                currentMatrix.add(index1, 0, U * -change);
                currentMatrix.add(index2, 0, U * change);
            }
            if(!voltageSources[index2]) {
                AMatrix.add(index2, index2, change);
                AMatrix.add(index1, index2, -change);
            } else {
                var U = ((VoltageSourceNode) nodes.get(index2)).getVoltage();
                currentMatrix.add(index2, 0, U * -change);
                currentMatrix.add(index1, 0, U * change);
            }
        } else {
            var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
            conductanceMatrix.add(index, index, change);
            if(!voltageSources[index]) {
                AMatrix.add(index, index, change);
            } else {
                var U = ((VoltageSourceNode) nodes.get(index)).getVoltage();
                currentMatrix.add(index, 0, U * change);
            }
        }
    }

    public void alterConductanceMatrix(int row, int column, double change) {
        if(conductanceMatrix == null || dirty)
            return;
        conductanceMatrix.add(row, column, change);
        if(!voltageSources[column]) {
            AMatrix.add(row, column, change);
        } else {
            var U = ((VoltageSourceNode) nodes.get(column)).getVoltage();
            currentMatrix.add(column, 0, U * -change);
        }
    }

    public void removeWire(AbstractElectricWire wire) {
        if(!wires.contains(wire))
            return;
        wires.remove(wire);
        wire.setNetwork(null);
        if(wire instanceof CapacitorWire)
            --sourceCount;

        updateConductance(wire, -wire.conductance());
        if(wire instanceof ISolverHook hook)
            solver.removeHook(hook);
    }

    public void updateResistance(AbstractElectricWire wire, double oldResistance) {
        double change = wire.conductance();
        if(oldResistance != 0)
            change -= 1 / oldResistance;
        updateConductance(wire, change);
    }

    public void addNode(ICouplingNode coupling) {
        coupling.assignIndex(nodes.size());
        coupling.setNetwork(this);
        couplings.add(coupling);
        nodes.add(coupling);
        setDirty();

        if(coupling instanceof VoltageSourceCoupling)
            ++sourceCount;

        if(coupling instanceof ISolverHook hook)
            solver.addHook(hook);
    }

    public Collection<INode> getNodes() {
        return nodes;
    }

    public DMatrixRMaj getLastGuess() {
        return lastGuess;
    }

    public void updateVoltage(VoltageSourceNode node, double oldVoltage) {
        if(conductanceMatrix == null || dirty)
            return;

        var diff = node.getVoltage() - oldVoltage;
        var index = node.getIndex();

        for(int i = 0; i < nodes.size(); ++i) {
            currentMatrix.add(i, 0, -diff * conductanceMatrix.get(i, index));
        }
    }

    public void updateCurrentMatrix(INode node, float change) {
        if(currentMatrix == null || dirty)
            return;
        currentMatrix.add(node.getIndex(), 0, change);
    }

    private void populateConductanceMatrix() {
        conductanceDelta = 0;
        conductanceUpdates = 0;
        conductanceMatrix.zero();
        List<AbstractElectricWire> staleWires = new ArrayList<>();
        var size = conductanceMatrix.getNumRows();
        for(var wire : wires) {
            var G = wire.conductance();
            if(wire.node1 != null && wire.node2 != null) {
                var index1 = wire.node1.getIndex();
                var index2 = wire.node2.getIndex();
                if(!nodes.contains(wire.node1) || !nodes.contains(wire.node2)) {
                    if(LOGGER != null) {
                        LOGGER.warn("Dropped a stale wire (wire nodes not part of this network) between {} and {}.", wire.node1, wire.node2);
                    }
                    staleWires.add(wire);
                    continue;
                }
                if(index1 >= size || index2 >= size) {
                    if(LOGGER != null) {
                        if(index1 >= size)
                            LOGGER.warn("Node {} has an index outside of the allocated matrix size, skipping", wire.node1);
                        if(index2 >= size)
                            LOGGER.warn("Node {} has an index outside of the allocated matrix size, skipping", wire.node2);
                    }
                    continue;
                }
                conductanceMatrix.add(index1, index1, G);
                conductanceMatrix.add(index2, index2, G);
                conductanceMatrix.add(index1, index2, -G);
                conductanceMatrix.add(index2, index1, -G);
            } else {
                var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
                conductanceMatrix.add(index, index, G);
            }
        }

        staleWires.forEach(wire -> {
            if(wire instanceof ISolverHook hook)
                solver.removeHook(hook);
            wires.remove(wire);
        });
        for(var node : couplings) {
            node.couple(conductanceMatrix);
        }

        AMatrix.setTo(conductanceMatrix);
    }

    private void populateCurrentMatrix() {
        currentMatrix.zero();
        for(int nodeIndex = 0; nodeIndex < nodes.size(); ++nodeIndex) {
            final var node = nodes.get(nodeIndex);
            if(node instanceof final VoltageSourceNode source) {
                var U = source.getVoltage();
                var index = node.getIndex();

                for(int i = 0; i < nodes.size(); ++i) {
                    currentMatrix.add(i, 0, -U * conductanceMatrix.get(i, index));
                    AMatrix.set(i, index, 0);
                }
                AMatrix.set(index, index, -1);
                voltageSources[nodeIndex] = true;
            } else if(node instanceof final CurrentSourceNode source) {
                currentMatrix.add(node.getIndex(), 0, source.getCurrent());
                voltageSources[nodeIndex] = false;
            } else if(node instanceof VoltageSourceCoupling source) {
                currentMatrix.add(node.getIndex(), 0, source.getVoltage());
                // This only applies to single terminal voltage sources
                voltageSources[nodeIndex] = false;
            } else {
                voltageSources[nodeIndex] = false;
            }
        }
    }

    public void merge(ElectricalNetwork other) {
        other.nodes.forEach(this::addNode);
        other.wires.forEach(this::addWire);
        // Make the other network empty.
        other.nodes.clear();
        other.wires.clear();
        other.couplings.clear();
    }

    private boolean acceptResults(DMatrixRMaj result, boolean canRepeat) {
        for(var node : nodes) {
            if(node.getIndex() >= result.getNumRows()) {
                // Why is it here???
                continue;
            }
            float value = (float) result.get(node.getIndex(), 0);
            if(!Float.isFinite(value)) {
                solver.zero();
                if(canRepeat) {
                    // Try again.
                    return false;
                } else {
                    // Failed again
                    node.receiveResult(0);
                }
            } else {
                node.receiveResult(value);
            }
        }
        return true;
    }

    private void prepareMatrices() {
        var nodeCount = nodes.size();
        if(conductanceMatrix == null || dirty || conductanceMatrix.getNumRows() != nodeCount) {
            conductanceMatrix = new DMatrixRMaj(nodeCount, nodeCount);
            AMatrix = new DynamicallyTypedMatrix(nodeCount, nodeCount);
            currentMatrix = new DMatrixRMaj(nodeCount, 1);
            voltageSources = new boolean[nodeCount];
            solver.setStateSize(nodeCount);
            dirty = false;

            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
            populateConductanceMatrix();
            populateCurrentMatrix();
            // TODO: Maybe try to populate the initial guess matrix from old node values.
        } else if(conductanceUpdates >= 20 || conductanceDelta > 1000) {
            // To prevent resistance from deviating due to floating point imprecision sometimes we rebuild
            // the matrices from scratch.
            if(LOGGER != null)
                LOGGER.debug("Cumulated conductance updates triggered admittance matrix recalculation");
            populateConductanceMatrix();
            populateCurrentMatrix();
        }
    }

    public void calculate(boolean printResult, boolean printState) {
        if(sourceCount == 0) {
            for(var node : nodes) {
                node.receiveResult(0);
            }
            return;
        }

        prepareMatrices();
        if(printState) {
            System.out.println(AMatrix);
            System.out.println(currentMatrix);
        }

        PERF.start();
        solver.saveGuess();
        int maxAttempts = hasHooks() ? 6 : 3;
        for(int i = 0; i < maxAttempts; ++i) {
            var canRepeat = i < maxAttempts - 1;
            var result = solver.solve(AMatrix, currentMatrix, canRepeat);
            lastGuess = result;
            if(solver.getInitialGuessDistance() < PRECISION) {
                acceptResults(result, false);
                break;
            }
            if (printResult) {
                System.out.println(result);
            }
            if(acceptResults(result, canRepeat)) {
                // If the network has ~special~ components, solving is continued
                // util the guess is good with updated properties (updated by the hooks)
                if(hasHooks())
                    continue;
                // If not, check if the current guess is good enough.
                if(solver.getFinalGuessDistance() < PRECISION)
                    break;
            }
        }
        PERF.end();
        for(var hook : solver.getHooks()) {
            hook.postUpperSolve();
        }
    }

    public void calculate() {
        calculate(false, false);
    }

    public void warmUp() {
        if(sourceCount == 0) {
            for(var node : nodes) {
                node.receiveResult(0);
            }
            return;
        }

        prepareMatrices();

        int maxAttempts = 4;
        for(int i = 0; i < maxAttempts; ++i) {
            solver.saveGuess();
            var result = solver.solve(AMatrix, currentMatrix, true);
            lastGuess = result;
            if(solver.getInitialGuessDistance() < PRECISION) {
                acceptResults(result, false);
                break;
            }
            if(acceptResults(result, i < maxAttempts - 1)) {
                // If the network has ~special~ components, solving is continued
                // util the guess is good with updated properties (updated by the hooks)
                if(hasHooks())
                    continue;
                break;
            }
        }
    }

    public class ConductanceMatrixAccess {
        public void add(int row, int column, double value) {
            var current = conductanceMatrix.get(row, column);
            conductanceMatrix.set(row, column, current + value);
        }
    }
}
