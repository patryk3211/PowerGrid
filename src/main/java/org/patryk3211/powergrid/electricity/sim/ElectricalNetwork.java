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
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.solver.*;
import org.patryk3211.powergrid.electricity.sim.special.CapacitorWire;
import org.slf4j.Logger;

import java.util.*;

public class ElectricalNetwork {
    public static final double G_MIN = 1e-8;
    private static final double PRECISION = 1e-7;
    private static final PerformanceCounter PERF = new PerformanceCounter("NetSolve");

    private final boolean addGMin;
    private final Set<AbstractElectricWire> wires = new HashSet<>();
    private final Set<ICouplingNode> couplings = new HashSet<>();
    private final List<INode> nodes = new ArrayList<>();

    private final Set<ISolverHook> hooks = new HashSet<>();

    private final ISolver solver;
    private boolean[] voltageSources;
    private int sourceCount;

    private DMatrixRMaj residualMatrix;
    private DMatrixRMaj conductanceMatrix;
    private DynamicallyTypedMatrix AMatrix;
    private DynamicallyTypedMatrix ScaledJ;
    private DMatrixRMaj currentMatrix;
    private DMatrixRMaj stateMatrix;
    private DMatrixRMaj bufferMatrix;
    private DMatrixRMaj auxMatrix;

    private double[] columnScale;
    private double[] rowScale;

    private boolean dirty;
    private boolean warmUp;
    private double conductanceDelta = 0;
    private int conductanceUpdates = 0;

    public static Logger LOGGER = null;

    private final Random random = new Random();

    public ElectricalNetwork(boolean addGMin) {
        solver =
                new DirectSolver();
//                new BiCGSTABSolver(0.001, 0.1);
        dirty = true;
        sourceCount = 0;
        this.addGMin = addGMin;
    }

    // Make sure all variables are completely rebuilt and repopulated.
    public void setDirty() {
        this.dirty = true;
    }

    public boolean hasHooks() {
        return !hooks.isEmpty();
    }

    public void addNode(INode node) {
        if(nodes.contains(node))
            return;
        node.assignIndex(nodes.size());
        node.setNetwork(this);
        nodes.add(node);
        setDirty();

        if(node instanceof ISolverHook hook)
            hooks.add(hook);

        if(node instanceof IElectricNode enode)
            addNode(enode);
        if(node instanceof ICouplingNode cnode)
            addNode(cnode);
    }

    private void addNode(IElectricNode node) {
        if(node instanceof VoltageSourceNode || node instanceof CurrentSourceNode)
            ++sourceCount;
    }

    private void addNode(ICouplingNode coupling) {
        couplings.add(coupling);
        if(coupling instanceof VoltageSourceCoupling)
            ++sourceCount;
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
            hooks.remove(hook);

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
            hooks.add(hook);
        warmUp = true;
    }

    private void complexAdd(int row, int column, double value) {
        conductanceMatrix.add(row, column, value);
        if(!voltageSources[column]) {
            AMatrix.add(row, column, value);
        } else {
            var U = ((VoltageSourceNode) nodes.get(column)).getVoltage();
            currentMatrix.add(row, 0, U * -value);
        }
    }

    public void updateConductance(AbstractElectricWire wire, double change) {
        if(conductanceMatrix == null || dirty || change == 0)
            return;

        conductanceDelta += Math.abs(change);
        ++conductanceUpdates;
        wire.stamp(this::complexAdd, change);
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
            hooks.remove(hook);
        warmUp = true;
    }

    public void updateResistance(AbstractElectricWire wire, double oldResistance) {
        double change = wire.conductance();
        if(oldResistance != 0)
            change -= 1 / oldResistance;
        updateConductance(wire, change);
    }

    public Collection<INode> getNodes() {
        return nodes;
    }

    public DMatrixRMaj getStateMatrix() {
        return stateMatrix;
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
            if(!Double.isFinite(G))
                continue;
            if(wire.node1 != null) {
                if(!nodes.contains(wire.node1)) {
                    if(LOGGER != null) {
                        LOGGER.warn("Dropped a stale wire (wire nodes not part of this network) between {} and {}.", wire.node1, wire.node2);
                    }
                    staleWires.add(wire);
                    continue;
                }
                if(wire.node1.getIndex() >= size) {
                    if(LOGGER != null)
                        LOGGER.warn("Node {} has an index outside of the allocated matrix size, skipping", wire.node1);
                    continue;
                }
            }
            if(wire.node2 != null) {
                if(!nodes.contains(wire.node2)) {
                    if(LOGGER != null) {
                        LOGGER.warn("Dropped a stale wire (wire nodes not part of this network) between {} and {}.", wire.node1, wire.node2);
                    }
                    staleWires.add(wire);
                    continue;
                }
                if(wire.node2.getIndex() >= size) {
                    if(LOGGER != null)
                        LOGGER.warn("Node {} has an index outside of the allocated matrix size, skipping", wire.node2);
                    continue;
                }
            }

            wire.stamp(conductanceMatrix::add, G);
        }

        staleWires.forEach(wire -> {
            if(wire instanceof ISolverHook hook)
                hooks.add(hook);
            wires.remove(wire);
        });
        for(var node : couplings) {
            try {
                node.couple(conductanceMatrix);
            } catch(IllegalArgumentException e) {
                LOGGER.error("Failed to couple {}:", node, e);
            }
        }

        AMatrix.setTo(conductanceMatrix);
    }

    private void populateCurrentMatrix() {
        currentMatrix.zero();
        boolean shouldAnchor = addGMin;
        FloatingNode anchor = null;
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
                shouldAnchor = false;
            } else if(node instanceof final CurrentSourceNode source) {
                currentMatrix.add(node.getIndex(), 0, source.getCurrent());
                voltageSources[nodeIndex] = false;
                shouldAnchor = false;
            } else if(node instanceof VoltageSourceCoupling source) {
                currentMatrix.add(node.getIndex(), 0, source.getVoltage());
                // This only applies to single terminal voltage sources
                voltageSources[nodeIndex] = false;
                if(anchor == null && source.getNegative() instanceof FloatingNode floating) {
                    anchor = floating;
                }
            } else {
                if(addGMin && node instanceof FloatingNode) {
                    conductanceMatrix.add(node.getIndex(), node.getIndex(), G_MIN);
                    AMatrix.add(node.getIndex(), node.getIndex(), G_MIN);
                }
                voltageSources[nodeIndex] = false;
            }
        }
        // Add a shunt to ground to the first floating node.
        // This ensures that the simulation is anchored to a 0V reference somewhere
        // and should improve performance and stability when there are only 2 port sources.
        if(shouldAnchor && anchor != null) {
            conductanceMatrix.add(anchor.getIndex(), anchor.getIndex(), 1000);
            AMatrix.add(anchor.getIndex(), anchor.getIndex(), 1000);
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

    private void acceptResults(DMatrixRMaj result) {
        for(var node : nodes) {
            if(node.getIndex() >= result.getNumRows()) {
                // Why is it here???
                continue;
            }
            float value = (float) result.get(node.getIndex(), 0);
            if(!Float.isFinite(value)) {
                solver.zero();
                stateMatrix.zero();
                break;
            } else {
                node.receiveResult(value);
            }
        }
    }

    private void validateJacobian() {
        var n = nodes.size();
        var v = new DMatrixRMaj(n, 1);
        RandomMatrices_DDRM.fillUniform(v, new Random());

        var epsilon = Math.sqrt(Math.ulp(1)) * (1 + NormOps_DDRM.normP1(stateMatrix));
        var left = new DMatrixRMaj(n, 1);
        AMatrix.mult(v, left);

        computeResidual();
        var residualBase = new DMatrixRMaj(residualMatrix);
        CommonOps_DDRM.add(stateMatrix, epsilon, v, stateMatrix);
        computeResidual();
        var right = new DMatrixRMaj(n, 1);
        CommonOps_DDRM.subtract(residualMatrix, residualBase, right);

        CommonOps_DDRM.scale(1 / epsilon, right);

        CommonOps_DDRM.subtract(left, right, left);
        if(LOGGER != null) {
            LOGGER.warn("Jacobian validation: {}", NormOps_DDRM.normP1(left));
        } else {
            System.out.printf("Jacobian validation: %g\n", NormOps_DDRM.normP1(left));
        }
    }

    private void prepareMatrices() {
        var nodeCount = nodes.size();
        if(conductanceMatrix == null || dirty || conductanceMatrix.getNumRows() != nodeCount) {
            var prevState = stateMatrix;
            conductanceMatrix = new DMatrixRMaj(nodeCount, nodeCount);
            AMatrix = new DynamicallyTypedMatrix(nodeCount, nodeCount);
            ScaledJ = new DynamicallyTypedMatrix(nodeCount, nodeCount);
            currentMatrix = new DMatrixRMaj(nodeCount, 1);
            residualMatrix = new DMatrixRMaj(nodeCount, 1);
            stateMatrix = new DMatrixRMaj(nodeCount, 1);
            auxMatrix = new DMatrixRMaj(nodeCount, 1);
            bufferMatrix = new DMatrixRMaj(nodeCount, 1);
            columnScale = new double[nodeCount];
            rowScale = new double[nodeCount];
            voltageSources = new boolean[nodeCount];
            solver.setStateSize(nodeCount);
            dirty = false;
            warmUp = true;

            // Use previous state matrix to accelerate warm up
            if(prevState != null) {
                for(var node : nodes) {
                    if(node.getIndex() >= prevState.getNumRows())
                        continue;
                    stateMatrix.set(node.getIndex(), 0, prevState.get(node.getIndex(), 0));
                }
            }

            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
            populateConductanceMatrix();
            populateCurrentMatrix();
        } else if(conductanceUpdates >= 20 || conductanceDelta > 1000) {
            // To prevent resistance from deviating due to floating point imprecision sometimes we rebuild
            // the matrices from scratch.
//            if(LOGGER != null && ModdedConfigs.logsEnabled())
//                LOGGER.debug("Cumulated conductance updates triggered admittance matrix recalculation");
//            populateConductanceMatrix();
//            populateCurrentMatrix();
        }
    }

    public void computeResidual() {
        AMatrix.mult(stateMatrix, residualMatrix);
        CommonOps_DDRM.subtract(residualMatrix, currentMatrix, residualMatrix);
        for(var hook : hooks) {
            hook.addResidual(residualMatrix);
        }
    }

    private void columnScales(DynamicallyTypedMatrix matrix) {
        int n = nodes.size();
        for(int i = 0; i < n; ++i) {
            double max = 0;
            for(int j = 0; j < n; ++j) {
                var v = Math.abs(matrix.get(j, i));
                max += v * v;
//                if(v > max) max = v;
            }
            if(max == 0) {
                columnScale[i] = 1;
                continue;
            }
            columnScale[i] = Math.min(1.0 / Math.sqrt(max), 2000);
        }
    }

    private void rowScales(DynamicallyTypedMatrix matrix) {
        int n = nodes.size();
        for(int i = 0; i < n; ++i) {
            double max = 0;
            for(int j = 0; j < n; ++j)  {
                var v = Math.abs(matrix.get(i, j));
                max += v * v;
//                if(v > max) max = v;
            }
            if(max == 0 || nodes.get(i) instanceof ICouplingNode) {
                rowScale[i] = 1;
                continue;
            }
            rowScale[i] = Math.min(1.0 / Math.sqrt(max), 2000);
        }
    }

    private void computeScales() {
//        for(int i = 0; i < nodes.size(); ++i) {
//            columnScale[i] = 1;
//            rowScale[i] = 1;
//        }
        columnScales(AMatrix);
        AMatrix.multColumns(columnScale, ScaledJ);
        rowScales(ScaledJ);
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

//        if(warmUp) {
//            warmUp = false;
//            warmUp();
//        }

        // Randomize the state slightly to prevent stagnation
//        for(int i = 0; i < nodes.size(); ++i) {
//            if(nodes.get(i) instanceof FloatingNode) {
//                var v = stateMatrix.get(i, 0);
//                v *= 0.95 + (random.nextDouble() * 0.1);
//                stateMatrix.set(i, 0, v);
//            }
//        }

        computeScales();

        PERF.start();
        solver.saveGuess();
        int maxAttempts = hasHooks() ? 200 : 10;
        int i;
        double norm = 0;
        boolean enteredNewton = false;
        for(i = 0; i < maxAttempts; ++i) {
            for(var hook : hooks) {
                hook.preSolve();
            }
            computeResidual();
            norm = NormOps_DDRM.normP1(residualMatrix);
            if(norm < PRECISION)
                break;
            AMatrix.multColumns(columnScale, ScaledJ);
            ScaledJ.multRows(rowScale, null);

            if(norm > 0.1 && !enteredNewton) {
                // Directly solve the system
                residualMatrix.setTo(currentMatrix);
                CommonOps_DDRM.changeSign(residualMatrix);
                for (var hook : hooks) {
                    hook.addResidual(residualMatrix);
                }
                CommonOps_DDRM.changeSign(residualMatrix);

                bufferMatrix.setTo(residualMatrix);
                CommonOps_DDRM.multRows(rowScale, bufferMatrix);

                var newState = solver.solve(ScaledJ, bufferMatrix, false);
                if (newState == null)
                    continue;
                CommonOps_DDRM.multRows(columnScale, newState);
                CommonOps_DDRM.add(0.1, stateMatrix, 0.9, newState, stateMatrix);
                acceptResults(stateMatrix);
            } else {
                // Perform Newton iterations
                enteredNewton = true;
                bufferMatrix.setTo(residualMatrix);
                CommonOps_DDRM.multRows(rowScale, bufferMatrix);

                var deltaX = solver.solve(ScaledJ, bufferMatrix, false);
                if (deltaX == null)
                    continue;

                var valid = !MatrixFeatures_DDRM.hasUncountable(deltaX);
                if(valid) {
                    double alpha = hasHooks() && i < 5 ? 0.5 : 1.0;
                    var applied = false;
                    ScaledJ.mult(deltaX, bufferMatrix);
                    CommonOps_DDRM.add(residualMatrix, -alpha, bufferMatrix, residualMatrix);
                    while(alpha > 0.0001) {
                        var newNorm = NormOps_DDRM.normP1(residualMatrix);
                        if(newNorm < norm) {
                            applied = true;
                            CommonOps_DDRM.multRows(columnScale, deltaX);
                            CommonOps_DDRM.add(stateMatrix, -alpha * 0.995, deltaX, stateMatrix);
                            acceptResults(stateMatrix);
                            break;
                        }
                        alpha *= 0.5;
                        CommonOps_DDRM.add(residualMatrix, alpha, bufferMatrix, residualMatrix);
                    }
                    if(!applied) {
                        break;
                    }
                } else {
                    solver.zero();
                }
            }
        }
        if(norm > PRECISION) {
            if(LOGGER != null) {
                if(ModdedConfigs.logsEnabled()) {
                    LOGGER.warn("Solution possibly not converged after {} Newton iterations, final norm: {}", i, norm);
                }
            } else {
                System.out.printf("Solution possibly not converged after %d Newton iterations, final norm: %g\n", i, norm);
            }
//            if(norm > 0.1) {
//                acceptResults(savedState);
//                stateMatrix.setTo(savedState);
//                warmUp = true;
//            }
        }
        if(printResult) {
            System.out.println(stateMatrix);
        }
        PERF.end();
        for(var hook : hooks) {
            hook.postUpperSolve();
        }
    }

    public void calculate() {
        calculate(false, false);
    }

    public void warmUp() {
        // Calculate initial state.
        stateMatrix.zero();
        for(var node : nodes) {
            node.receiveResult(0);
        }
        if(sourceCount == 0)
            return;
        // Zeroes things out
        for(var hook : hooks) {
            hook.preSolve();
        }

        solver.saveGuess();
        int maxAttempts = 20;
        int i;
        for(i = 0; i < maxAttempts; ++i) {
            AMatrix.mult(stateMatrix, residualMatrix);
            CommonOps_DDRM.subtract(residualMatrix, currentMatrix, residualMatrix);
            double norm = NormOps_DDRM.normP1(residualMatrix);
            if(norm < PRECISION)
                break;

            var deltaX = auxMatrix;
            AMatrix.solve(residualMatrix, deltaX);
//            var deltaX = solver.solve(AMatrix, residualMatrix, false);
//            if(deltaX == null)
//                continue;

            var valid = !MatrixFeatures_DDRM.hasUncountable(deltaX);
            if(valid) {
                double alpha = 0.5;
                var applied = false;
                AMatrix.mult(deltaX, bufferMatrix);
                CommonOps_DDRM.add(residualMatrix, alpha, bufferMatrix, residualMatrix);
                while(alpha > 0.0001) {
                    var newNorm = NormOps_DDRM.normP1(residualMatrix);
                    if(newNorm < norm) {
                        applied = true;
                        CommonOps_DDRM.add(stateMatrix, alpha * 0.995, deltaX, stateMatrix);
                        break;
                    }
                    alpha *= 0.5;
                    CommonOps_DDRM.add(residualMatrix, -alpha, bufferMatrix, residualMatrix);
                }
                if(!applied) {
                    break;
                }
            } else {
                solver.zero();
            }
        }
        acceptResults(stateMatrix);
    }
}
