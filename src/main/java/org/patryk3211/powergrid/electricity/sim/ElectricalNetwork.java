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
    private static final int MAX_SCALE_REUSE_COUNT = 20;

    private final boolean addGMin;
    private final Set<AbstractElectricWire> wires = new HashSet<>();
    private final Set<ICouplingNode> couplings = new HashSet<>();
    protected final List<INode> nodes = new ArrayList<>();
    private int eliminatedStart;

    private final Set<ISolverHook> hooks = new HashSet<>();
    private final Set<IStaticResidual> residuals = new HashSet<>();

    private final ISolver solver;
    private int sourceCount;
    private int groundReferenceCount;

    private DMatrixRMaj ResidualVector;

    private DynamicallyTypedMatrix JacobianKept;
    private DynamicallyTypedMatrix JacobianEliminated;
    private DynamicallyTypedMatrix JacobianRight;
    private DynamicallyTypedMatrix JacobianBottom;

    private DMatrixRMaj ReducedRHSVector;
    private DMatrixRMaj EliminatedRHSVector;

    // W = J_e ^ -1 * J_b
    // J_e * W = J_b
    private DynamicallyTypedMatrix WMatrix;
    private DynamicallyTypedMatrix ReducedCorrection;
    private DynamicallyTypedMatrix ReducedJacobian;

    private DynamicallyTypedMatrix ScaledJ;
    private DMatrixRMaj StateVector;
    private DMatrixRMaj AuxiliaryVector;
    private DMatrixRMaj EliminatedSolved;

    private double[] columnScales;
    private double[] rowScales;

    private boolean dirty;
    private double conductanceDelta = 0;
    private int conductanceUpdates = 0;
    private int eliminatedUpdates = 0;
    private int scalesAge = 0;
    private boolean countUpdates = true;
    private boolean lockEliminated = false;

    private boolean recalculateScales;
    private boolean eliminatedChanged;
    private boolean eliminatedRHSZero;
    private boolean eliminatedSolved;

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
        node.assignIndex(eliminatedStart);
        node.setNetwork(this);
        for(int i = eliminatedStart; i < nodes.size(); ++i) {
            // Move indices forward by one.
            nodes.get(i).assignIndex(i + 1);
        }
        // Insert before eliminated nodes.
        nodes.add(eliminatedStart++, node);
        setDirty();

        if(node instanceof ISolverHook hook)
            hooks.add(hook);
        if(node instanceof IStaticResidual residual)
            residuals.add(residual);

        if(node instanceof IElectricNode enode)
            addNode(enode);
        if(node instanceof ICouplingNode cnode)
            addNode(cnode);
    }

    private void addNode(IElectricNode node) {
        if(node instanceof CurrentSourceNode)
            ++sourceCount;
    }

    private void addNode(ICouplingNode coupling) {
        couplings.add(coupling);
        if(coupling instanceof VoltageSourceCoupling)
            ++sourceCount;
    }

    public void addNodes(INode... nodes) {
        for(var node : nodes)
            addNode(node);
    }

    public void removeNode(INode node) {
        if(node == null)
            return;
        if(node.getNetwork() != this || node.getIndex() >= nodes.size() || nodes.get(node.getIndex()) != node)
            // This node is not actually in this network.
            return;

        // Node part of the kept group
        for(int i = node.getIndex() + 1; i < nodes.size(); ++i) {
            // Move back all nodes by one.
            nodes.get(i).assignIndex(i - 1);
            if(StateVector != null && i < StateVector.getNumRows())
                StateVector.set(i - 1, 0, StateVector.get(i, 0));
        }
        nodes.remove(node.getIndex());
        if(node.getIndex() < eliminatedStart) {
            // Node not part of the eliminated group, starting node index has moved
            --eliminatedStart;
        }

        if(node instanceof ICouplingNode)
            couplings.remove(node);
        if(node instanceof CurrentSourceNode || node instanceof VoltageSourceCoupling)
            --sourceCount;
        if(node instanceof ISolverHook hook)
            hooks.remove(hook);
        if(node instanceof IStaticResidual residual)
            residuals.remove(residual);

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
        if(wire.node1 == null || wire.node2 == null)
            ++groundReferenceCount;

        updateConductance(wire, wire.conductance());
        if(wire instanceof ISolverHook hook)
            hooks.add(hook);
        if(wire instanceof IStaticResidual residual)
            residuals.add(residual);
    }

    public void updateConductance(AbstractElectricWire wire, double change) {
        if(JacobianKept == null || dirty || change == 0)
            return;

        conductanceDelta += Math.abs(change);
        if(countUpdates) {
            ++conductanceUpdates;
            if(wire.node1.getIndex() >= eliminatedStart && wire.node2.getIndex() >= eliminatedStart)
                ++eliminatedUpdates;
        }
        wire.stamp(this::jacobianAdd, change);
    }

    public void alterConductanceMatrix(int row, int column, double change) {
        if(JacobianKept == null || dirty)
            return;
        jacobianAdd(row, column, change);
    }

    public void removeWire(AbstractElectricWire wire) {
        if(!wires.contains(wire))
            return;
        wires.remove(wire);
        wire.setNetwork(null);
        if(wire instanceof CapacitorWire)
            --sourceCount;
        if(wire.node1 == null || wire.node2 == null)
            --groundReferenceCount;

        updateConductance(wire, -wire.conductance());
        if(wire instanceof ISolverHook hook)
            hooks.remove(hook);
        if(wire instanceof IStaticResidual residual)
            residuals.remove(residual);
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

    public DMatrixRMaj getStateVector() {
        return StateVector;
    }

    protected void jacobianAdd(int row, int column, double value) {
        if(value == 0)
            return;
        recalculateScales = true;
        if(row < eliminatedStart && column < eliminatedStart) {
            JacobianKept.add(row, column, value);
            if(ReducedJacobian != null)
                ReducedJacobian.add(row, column, value);
        } else if(!lockEliminated) {
            if(row < eliminatedStart) {
                JacobianRight.add(row, column - eliminatedStart, value);
                eliminatedChanged = true;
            } else if(column < eliminatedStart) {
                JacobianBottom.add(row - eliminatedStart, column, value);
                eliminatedChanged = true;
            } else {
                JacobianEliminated.add(row - eliminatedStart, column - eliminatedStart, value);
                eliminatedChanged = true;
            }
        }
    }

    protected void rhsAdd(int row, double value) {
        if(value == 0)
            return;
        if(row < eliminatedStart) {
            ReducedRHSVector.add(row, 0, value);
        } else {
            EliminatedRHSVector.add(row - eliminatedStart, 0, value);
            eliminatedRHSZero = false;
        }
    }

    protected void populateConductanceMatrix(boolean withEliminated) {
        conductanceDelta = 0;
        conductanceUpdates = 0;

        JacobianKept.denseZero();
        if(JacobianEliminated != null && withEliminated) {
            JacobianEliminated.denseZero();
            JacobianBottom.denseZero();
            JacobianRight.denseZero();
        }

        lockEliminated = !withEliminated;
        List<AbstractElectricWire> staleWires = new ArrayList<>();
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
                if(wire.node1.getIndex() >= nodes.size()) {
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
                if(wire.node2.getIndex() >= nodes.size()) {
                    if(LOGGER != null)
                        LOGGER.warn("Node {} has an index outside of the allocated matrix size, skipping", wire.node2);
                    continue;
                }
            }

            wire.stamp(this::jacobianAdd, G);
        }

        staleWires.forEach(wire -> {
            if(wire instanceof ISolverHook hook)
                hooks.remove(hook);
            if(wire instanceof IStaticResidual residual)
                residuals.remove(residual);
            wires.remove(wire);
        });
        var badCouplings = new ArrayList<ICouplingNode>();
        for(var node : couplings) {
            try {
                // This is using the same method as wires but coupling nodes,
                // probably shouldn't target eliminated nodes.
                node.couple(this::jacobianAdd);
            } catch(IllegalArgumentException e) {
                LOGGER.error("Failed to couple {}:", node, e);
                badCouplings.add(node);
            }
        }
        badCouplings.forEach(this::removeNode);
        lockEliminated = false;

        if(addGMin) {
            boolean shouldAnchor = true;
            FloatingNode anchor = null;
            for (var node : nodes) {
                if (node instanceof CurrentSourceNode) {
                    shouldAnchor = false;
                } else if (node instanceof VoltageSourceCoupling source) {
                    if (anchor == null && source.getNegative() instanceof FloatingNode floating && floating.getIndex() < eliminatedStart) {
                        anchor = floating;
                    }
                } else if (node instanceof FloatingNode) {
                    if(node.getIndex() < eliminatedStart) {
                        JacobianKept.add(node.getIndex(), node.getIndex(), G_MIN);
                    } else if(withEliminated) {
                        JacobianEliminated.add(node.getIndex() - eliminatedStart, node.getIndex() - eliminatedStart, G_MIN);
                    }
                }
            }
            if(groundReferenceCount == 0) {
                // Add a shunt to ground to the first floating node.
                // This ensures that the simulation is anchored to a 0V reference somewhere
                // and should improve performance and stability when there are only 2 port sources.
                if (shouldAnchor && anchor != null) {
                    JacobianKept.add(anchor.getIndex(), anchor.getIndex(), 1000);
                }
            }
        }

        JacobianKept.optimize();

        if(withEliminated) {
            eliminatedUpdates = 0;
            calculateEliminatedMatrices();
        } else if(ReducedCorrection != null) {
            // Subtract the already calculated correction matrix
            JacobianKept.subtract(ReducedCorrection, ReducedJacobian);
            ReducedJacobian.optimize();
        }
        recalculateScales = true;
    }

    private void calculateEliminatedMatrices() {
        if(JacobianEliminated != null) {
            // Match sparsity state for all matrices
            ReducedCorrection.optimize();
            JacobianEliminated.convert(ReducedCorrection.getState());
            JacobianBottom.convert(ReducedCorrection.getState());
            JacobianRight.convert(ReducedCorrection.getState());
            WMatrix.convert(ReducedCorrection.getState());

            // Prepare reduced jacobian correction matrix.
            JacobianEliminated.refactorize();
            JacobianEliminated.solve(JacobianBottom, WMatrix);
            JacobianRight.mult(WMatrix, ReducedCorrection);

            JacobianKept.subtract(ReducedCorrection, ReducedJacobian);
            ReducedJacobian.optimize();

            recalculateScales = true;
        }
        eliminatedChanged = false;
    }

    public void merge(ElectricalNetwork other) {
        other.nodes.forEach(this::addNode);
        other.wires.forEach(this::addWire);
        // Make the other network empty.
        other.nodes.clear();
        other.wires.clear();
        other.couplings.clear();
    }

    public double getValue(INode node) {
        if(StateVector == null)
            return 0;
        var index = node.getIndex();
        if(index < 0 || index >= nodes.size())
            return 0;
        if(index >= eliminatedStart) {
            if(WMatrix == null || index - eliminatedStart >= WMatrix.getNumRows())
                return 0;
            double value = eliminatedSolved ? EliminatedSolved.get(index - eliminatedStart, 0) : 0;
            for(int i = 0; i < StateVector.getNumRows(); ++i) {
                value -= WMatrix.get(index - eliminatedStart, i) * StateVector.get(i, 0);
            }
            return value;
        }
        if(index >= StateVector.getNumRows())
            return 0;
        var value = StateVector.get(index);
        return Double.isFinite(value) ? value : 0;
    }

    private void validateJacobian() {
        var n = nodes.size();
        var v = new DMatrixRMaj(n, 1);
        RandomMatrices_DDRM.fillUniform(v, new Random());

        var epsilon = Math.sqrt(Math.ulp(1)) * (1 + NormOps_DDRM.normP1(StateVector));
        var left = new DMatrixRMaj(n, 1);
        JacobianKept.mult(v, left);

        computeResidual(JacobianKept, StateVector);
        var residualBase = new DMatrixRMaj(ResidualVector);
        CommonOps_DDRM.add(StateVector, epsilon, v, StateVector);
        computeResidual(JacobianKept, StateVector);
        var right = new DMatrixRMaj(n, 1);
        CommonOps_DDRM.subtract(ResidualVector, residualBase, right);

        CommonOps_DDRM.scale(1 / epsilon, right);

        CommonOps_DDRM.subtract(left, right, left);
        if(LOGGER != null) {
            LOGGER.warn("Jacobian validation: {}", NormOps_DDRM.normP1(left));
        } else {
            System.out.printf("Jacobian validation: %g\n", NormOps_DDRM.normP1(left));
        }
    }

    private void swapNodes(INode node1, INode node2) {
        var index1 = node1.getIndex();
        var index2 = node2.getIndex();

        nodes.set(index1, node2);
        nodes.set(index2, node1);

        if(StateVector != null) {
            // Swap state vector values to allow for correct warm start
            // We must check size since eliminated node values are resolved on-demand
            var size = StateVector.getNumRows();
            if(index1 < size && index2 < size) {
                var node2Value = StateVector.get(index2, 0);
                StateVector.set(index2, 0, StateVector.get(index1, 0));
                StateVector.set(index1, 0, node2Value);
            } else if(index1 < size) {
                StateVector.set(index1, 0, 0);
            } else if(index2 < size) {
                StateVector.set(index2, 0, 0);
            }
        }

        node1.assignIndex(index2);
        node2.assignIndex(index1);
        // Jacobian needs a rebuild
        dirty = true;
    }

    public void optimizeNode(INode node) {
        assert node.getNetwork() == this : "Node is not part of this network";
        if(node.getIndex() >= eliminatedStart) {
            // Already in the correct spot.
            return;
        }
        if(node.getIndex() == eliminatedStart - 1) {
            // Simply move pointer back by one to include this node.
            --eliminatedStart;
        } else {
            // Swap nodes and move eliminated pointer
            var other = nodes.get(--eliminatedStart);
            swapNodes(node, other);
        }
    }

    public void unoptimizeNode(INode node) {
        assert node.getNetwork() == this : "Node is not part of this network";
        if(node.getIndex() < eliminatedStart) {
            // Already in the correct spot.
            return;
        }
        if(node.getIndex() == eliminatedStart) {
            // Simply move pointer forward by one to include this node.
            ++eliminatedStart;
        } else {
            // Swap nodes and move eliminated pointer
            var other = nodes.get(++eliminatedStart);
            swapNodes(node, other);
        }
    }

    public void unoptimizeAll() {
        if(eliminatedStart != nodes.size()) {
            eliminatedStart = nodes.size();
            dirty = true;
        }
    }

    private int eliminatedNodeCount() {
        return nodes.size() - eliminatedStart;
    }

    private void prepareMatrices() {
        ++scalesAge;
        var nodeCount = nodes.size();
        var eliminatedCount = eliminatedNodeCount();
        var reducedCount = nodeCount - eliminatedCount;
        var shouldReallocate =
                JacobianKept == null
                || dirty
                || JacobianKept.getNumRows() != reducedCount
                || (eliminatedCount != 0
                        && (JacobianEliminated == null || JacobianEliminated.getNumRows() != eliminatedCount));
        if(shouldReallocate) {
            var prevState = StateVector;
            // Kept Jacobian
            JacobianKept = new DynamicallyTypedMatrix(reducedCount, reducedCount);
            if(eliminatedCount != 0) {
                // Eliminated Jacobian
                JacobianEliminated = new DynamicallyTypedMatrix(eliminatedCount, eliminatedCount);
                // Eliminated x Reduced Jacobian
                JacobianBottom = new DynamicallyTypedMatrix(eliminatedCount, reducedCount);
                // Reduced x Eliminated Jacobian
                JacobianRight = new DynamicallyTypedMatrix(reducedCount, eliminatedCount);

                WMatrix = new DynamicallyTypedMatrix(eliminatedCount, reducedCount);
                ReducedCorrection = new DynamicallyTypedMatrix(reducedCount, reducedCount);
                ReducedJacobian = new DynamicallyTypedMatrix(reducedCount, reducedCount);

                EliminatedRHSVector = new DMatrixRMaj(eliminatedCount, 1);
                EliminatedSolved = new DMatrixRMaj(eliminatedCount, 1);
            } else {
                // Drop matrices
                JacobianEliminated = null;
                JacobianBottom = null;
                JacobianRight = null;
                WMatrix = null;
                ReducedCorrection = null;
                ReducedJacobian = null;
                EliminatedRHSVector = null;
                EliminatedSolved = null;
            }

            ReducedRHSVector = new DMatrixRMaj(reducedCount, 1);

            ScaledJ = new DynamicallyTypedMatrix(reducedCount, reducedCount, DynamicallyTypedMatrix.Solver.LU);
            ResidualVector = new DMatrixRMaj(reducedCount, 1);
            StateVector = new DMatrixRMaj(reducedCount, 1);
            AuxiliaryVector = new DMatrixRMaj(reducedCount, 1);

            columnScales = new double[reducedCount];
            rowScales = new double[reducedCount];
            solver.setStateSize(reducedCount);
            dirty = false;

            // Use previous state matrix to accelerate warm up
            if(prevState != null) {
                for(int i = 0; i < reducedCount; ++i) {
                    if(i >= prevState.getNumRows())
                        continue;
                    StateVector.set(i, 0, prevState.get(i, 0));
                }
            }

            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
            populateConductanceMatrix(true);
            scalesAge = MAX_SCALE_REUSE_COUNT + 1;
        } else if(conductanceUpdates >= 500 || conductanceDelta > 1000 || eliminatedUpdates >= 100) {
            // To prevent resistance from deviating due to floating point imprecision sometimes we rebuild
            // the matrices from scratch.
            if(LOGGER != null && ModdedConfigs.logsEnabled())
                LOGGER.debug("Cumulated conductance updates triggered admittance matrix recalculation");
            populateConductanceMatrix(eliminatedUpdates >= 100);
        }
    }

    private DynamicallyTypedMatrix getWorkMatrix() {
        DynamicallyTypedMatrix workMatrix;
        if(ReducedJacobian != null) {
            workMatrix = ReducedJacobian;
            if(eliminatedChanged) {
                // This is not efficient at all, if a node has dynamic conductance it should not be eliminated
                calculateEliminatedMatrices();
                computeRHS();
            }
        } else {
            workMatrix = JacobianKept;
        }
        return workMatrix;
    }

    private void prepareScaled(DynamicallyTypedMatrix workMatrix) {
        if(scalesAge >= 20) {
            computeScales(workMatrix);
            recalculateScales = true;
        }
        if(recalculateScales) {
            workMatrix.multColumns(columnScales, ScaledJ);
            ScaledJ.multRows(rowScales, null);
            ScaledJ.refactorize();
            recalculateScales = false;
        }
    }

    private void computeRHS() {
        ReducedRHSVector.zero();
        if(EliminatedRHSVector != null)
            EliminatedRHSVector.zero();
        eliminatedRHSZero = true;
        for(var residual : residuals) {
            residual.addResidual(this::rhsAdd);
        }
        if(EliminatedRHSVector != null && !eliminatedRHSZero) {
            JacobianEliminated.solve(EliminatedRHSVector, EliminatedSolved);
            JacobianRight.mult(EliminatedSolved, AuxiliaryVector);
            CommonOps_DDRM.subtract(ReducedRHSVector, AuxiliaryVector, ReducedRHSVector);
            eliminatedSolved = true;
        } else {
            eliminatedSolved = false;
        }
    }

    protected void residualAdd(int row, double value) {
        if(row >= ResidualVector.getNumRows())
            return;
        ResidualVector.add(row, 0, value);
    }

    public void computeResidual(DynamicallyTypedMatrix workMatrix, DMatrixRMaj state) {
        workMatrix.mult(state, ResidualVector);
        CommonOps_DDRM.subtract(ResidualVector, ReducedRHSVector, ResidualVector);
        for(var hook : hooks) {
            hook.addResidual(this::residualAdd);
        }
    }

    private void columnScales(DynamicallyTypedMatrix matrix) {
        int n = matrix.getNumRows();
        for(int i = 0; i < n; ++i) {
            double max = 0;
            for(int j = 0; j < n; ++j) {
                var v = Math.abs(matrix.get(j, i));
                max += v * v;
            }
            if(max == 0) {
                columnScales[i] = 1;
                continue;
            }
            columnScales[i] = Math.min(1.0 / Math.sqrt(max), 2000);
        }
    }

    private void rowScales(DynamicallyTypedMatrix matrix) {
        int n = matrix.getNumRows();
        for(int i = 0; i < n; ++i) {
            if(nodes.get(i) instanceof ICouplingNode) {
                rowScales[i] = 1;
                continue;
            }
            double max = 0;
            for(int j = 0; j < n; ++j)  {
                var v = Math.abs(matrix.get(i, j));
                max += v * v;
            }
            if(max == 0) {
                rowScales[i] = 1;
                continue;
            }
            rowScales[i] = Math.min(1.0 / Math.sqrt(max), 2000);
        }
    }

    private void computeScales(DynamicallyTypedMatrix workMatrix) {
        columnScales(workMatrix);
        workMatrix.multColumns(columnScales, ScaledJ);
        rowScales(ScaledJ);
        scalesAge = 0;
    }

    public void calculate() {
        if(sourceCount == 0) {
            if(StateVector != null)
                StateVector.zero();
            return;
        }

        prepareMatrices();
        if(eliminatedChanged) {
            // Make sure eliminated Jacobian is factorized for RHS compute
            calculateEliminatedMatrices();
        }
        computeRHS();

        PERF.start();
        int maxAttempts = hasHooks() ? 200 : 10;
        int i;
        double norm = 0;
        for(i = 0; i < maxAttempts; ++i) {
            if(hasHooks() && i < maxAttempts - 20 && i % 2 == 0) {
                countUpdates = false;
                for(var hook : hooks) {
                    hook.preSolve();
                }
                countUpdates = true;
            }
            var workMatrix = getWorkMatrix();
            computeResidual(workMatrix, StateVector);
            norm = NormOps_DDRM.normP1(ResidualVector);
            if(norm < PRECISION)
                break;
            prepareScaled(workMatrix);

            // Perform Newton iterations
            AuxiliaryVector.setTo(ResidualVector);
            CommonOps_DDRM.multRows(rowScales, AuxiliaryVector);

            var deltaX = solver.solve(ScaledJ, AuxiliaryVector, false);
            if (deltaX == null)
                continue;

            var valid = !MatrixFeatures_DDRM.hasUncountable(deltaX);
            if(valid) {
                double alpha = hasHooks() && i < 5 ? 0.5 : 1.0;
                var applied = false;
                ScaledJ.mult(deltaX, AuxiliaryVector);
                CommonOps_DDRM.add(ResidualVector, -alpha, AuxiliaryVector, ResidualVector);
                while(alpha > 0.0001) {
                    var newNorm = NormOps_DDRM.normP1(ResidualVector);
                    if(newNorm < norm) {
                        applied = true;
                        CommonOps_DDRM.multRows(columnScales, deltaX);
                        CommonOps_DDRM.add(StateVector, -alpha * 0.995, deltaX, StateVector);
                        break;
                    }
                    alpha *= 0.5;
                    CommonOps_DDRM.add(ResidualVector, alpha, AuxiliaryVector, ResidualVector);
                }
                if(!applied) {
                    break;
                }
            } else {
                solver.zero();
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
        }
        PERF.end();
        for(var hook : hooks) {
            hook.postUpperSolve();
        }
    }
}
