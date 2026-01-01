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

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.createmod.catnip.data.Pair;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.calculation.IStamped;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.solver.*;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePort;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

public class ElectricalNetwork implements IStamped {
    public static final double G_MIN = 1e-8;
    private static final PerformanceCounter PERF = new PerformanceCounter("NetSolve");
    private static final int MAX_SCALE_REUSE_COUNT = 50;
    private static final boolean SCALING = true;

    private final boolean addGMin;
    protected final Set<AbstractElectricWire> wires = new HashSet<>();
    protected final Set<ICouplingNode> couplings = new HashSet<>();
    protected final List<INode> nodes = new ArrayList<>();
    private int eliminatedStart;

    private final Set<IOuterHook> outerHooks = new HashSet<>();
    private final Set<ISolverHook> innerHooks = new HashSet<>();
    private final Set<ISolverHook> leafInnerHooks = new HashSet<>();
    private final Set<IStaticResidual> residuals = new HashSet<>();
    protected final Map<IElectricNode, IElectricNode> leafNodes = new Reference2ReferenceOpenHashMap<>();

    private final List<ExchangeRow> changedRows = new ArrayList<>();

    private double minimumAllowedPrecision = 1e-6;
    private double absoluteStoppingCriterion = 1e-7;
    private double relativeStoppingCriterion = 1e-12;

    private ISolver solver;
    private int sourceCount;
    private int groundReferenceCount;

    public DMatrixRMaj ResidualVector;

    private DynamicallyTypedMatrix JacobianKept;
    private DynamicallyTypedMatrix JacobianEliminated;
    private DynamicallyTypedMatrix JacobianRight;
    private DynamicallyTypedMatrix JacobianBottom;
    private DynamicallyTypedMatrix A0;

    private DMatrixRMaj ReducedRHSVector;
    private DMatrixRMaj EliminatedRHSVector;

    // W = J_e ^ -1 * J_b
    // J_e * W = J_b
    private DynamicallyTypedMatrix WMatrix;
    private DynamicallyTypedMatrix ReducedCorrection;
    private DynamicallyTypedMatrix ReducedJacobian;

    private DynamicallyTypedMatrix ScaledJ;
    protected DMatrixRMaj StateVector;
    protected DMatrixRMaj AuxiliaryVector;
    private DMatrixRMaj EliminatedSolved;

    private double[] columnScales;
    private double[] rowScales;

    protected boolean dirty;
    private double conductanceDelta = 0;
    private int conductanceUpdates = 0;
    private int eliminatedUpdates = 0;
    private int scalesAge = 0;
    private boolean countUpdates = true;
    private boolean lockEliminated = false;
    protected boolean converged;
    protected int warmUpTicks = 0;
    protected int stamp;
    private int currentMultiTick = 1;
    private boolean enableRowExchange = false;

    private boolean recalculateScales;
    private boolean eliminatedChanged;
    private boolean eliminatedRHSZero;
    private boolean eliminatedSolved;

    public static Logger LOGGER = null;

    public Function<Boolean, Integer> maxIterations = b -> 200;

    public ElectricalNetwork(boolean addGMin) {
        this(addGMin, SolverType.DIRECT);
    }

    public ElectricalNetwork(boolean addGMin, SolverType solver) {
        dirty = true;
        sourceCount = 0;
        this.addGMin = addGMin;
        setSolverType(solver);
    }

    public void setSolverType(SolverType type) {
        if(solver != null) {
            var currentType = solver.type();
            if (currentType == type)
                return;
        }
        solver = switch(type) {
            case DIRECT -> new DirectSolver();
            case BICGSTAB -> new BiCGSTABSolver(absoluteStoppingCriterion, 0.001f);
            case GMRES -> new GMRESSolver(1e-8, 30);
        };
        if(nodes.isEmpty())
            return;
        var eliminatedCount = eliminatedNodeCount();
        var reducedCount = nodes.size() - eliminatedCount;
        solver.setStateSize(reducedCount);
    }

    public void setPrecision(double absoluteCriterion, double relativeCriterion, double minimumPrecision) {
        this.absoluteStoppingCriterion = absoluteCriterion;
        this.relativeStoppingCriterion = relativeCriterion;
        this.minimumAllowedPrecision = minimumPrecision;
    }

    // Make sure all variables are completely rebuilt and repopulated.
    public void setDirty() {
        this.dirty = true;
    }

    public boolean hasHooks() {
        return !innerHooks.isEmpty();
    }

    public double getDeltaTime() {
        return 0.05f / currentMultiTick;
    }

    public void warmUp(int ticks) {
        if(ticks == -1 || warmUpTicks == -1) {
            warmUpTicks = -1;
            return;
        }
        if(warmUpTicks < ticks)
            warmUpTicks = ticks;
    }

    public void addSegment(Collection<INetworkElement> elements) {
        for(var element : elements) {
            if(element instanceof INode node) {
                addNode(node);
            } else if(element instanceof AbstractElectricWire wire) {
                addWire(wire);
            }
        }
    }

    public void fromElements(Collection<INetworkElement> elements) {
        var values = new ArrayList<Pair<INode, Double>>();
        for(var element : elements) {
            if(element instanceof IElectricNode node) {
                values.add(Pair.of(node, node.getStateValue()));
                addNode(node);
            }
        }
        for(var element : elements) {
            if(element instanceof ICouplingNode node) {
                values.add(Pair.of(node, node.getStateValue()));
                addNode(node);
            } else if(element instanceof AbstractElectricWire wire) {
                addWire(wire);
            }
        }
        prepareMatrices(currentMultiTick);
        for(var pair : values) {
            setValue(pair.getFirst(), pair.getSecond());
        }
    }

    public void addNode(INode node) {
        if(nodes.contains(node) || leafNodes.containsKey(node))
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

        if(node instanceof IOuterHook hook)
            outerHooks.add(hook);
        if(node instanceof ISolverHook hook)
            innerHooks.add(hook);
        if(node instanceof IStaticResidual residual)
            residuals.add(residual);
        if(node.isSource())
            ++sourceCount;

        if(node instanceof ICouplingNode cnode)
            couplings.add(cnode);
        warmUp(5);
    }

    public void addNodes(INode... nodes) {
        for(var node : nodes)
            addNode(node);
    }

    public void removeNode(INode node) {
        internalRemoveNode(node);
    }

    protected final void internalRemoveNode(INode node) {
        if(node == null)
            return;
        if(leafNodes.containsKey(node)) {
            node.setNetwork(null);
            leafNodes.remove(node);
            return;
        }
        if(node.getNetwork() != this || node.getIndex() >= nodes.size() || nodes.get(node.getIndex()) != node)
            // This node is not actually in this network.
            return;

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
        if(node.isSource())
            --sourceCount;
        if(node instanceof IOuterHook hook)
            outerHooks.remove(hook);
        if(node instanceof ISolverHook hook)
            innerHooks.remove(hook);
        if(node instanceof IStaticResidual residual)
            residuals.remove(residual);

        warmUp(3);
        node.setNetwork(null);
        setDirty();
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && leafNodes.isEmpty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isConverged() {
        return converged;
    }

    public void addWire(AbstractElectricWire wire) {
        if(wire.node1 != null && !nodes.contains(wire.node1) && !leafNodes.containsKey(wire.node1)) {
            // If node of a wire is not null it must be in the network's node set.
            var suffix = wire.node1.getNetwork() == null ? "no network" : "different network";
            throw new IllegalArgumentException("Both nodes of a wire must be part of the network (node1 " + wire.node1 + " isn't - " + suffix + ")");
        }
        if(wire.node2 != null && !nodes.contains(wire.node2) && !leafNodes.containsKey(wire.node2)) {
            // If node of a wire is not null it must be in the network's node set.
            var suffix = wire.node2.getNetwork() == null ? "no network" : "different network";
            throw new IllegalArgumentException("Both nodes of a wire must be part of the network (node2 " + wire.node2 + " isn't - " + suffix + ")");
        }
        enableRowExchange = false;
        wire.setNetwork(this);
        wires.add(wire);
        if(wire.isSource())
            ++sourceCount;
        if(wire.node1 == null || wire.node2 == null)
            ++groundReferenceCount;

        updateConductance(wire, wire.conductance());
        if(wire instanceof IOuterHook hook)
            outerHooks.add(hook);
        if(wire instanceof ISolverHook hook) {
            var isFull = true;
            for(var coupled : hook.coupledNodes()) {
                if(isLeaf(coupled)) {
                    isFull = false;
                    break;
                }
            }
            if(isFull) {
                innerHooks.add(hook);
            } else {
                leafInnerHooks.add(hook);
            }
        }
        if(wire instanceof IStaticResidual residual)
            residuals.add(residual);
        converged = false;
        warmUp(1);
    }

    public void updateConductance(AbstractElectricWire wire, double change) {
        if(JacobianKept == null || dirty || Math.abs(change) < G_MIN * 0.1)
            return;
        if(leafNodes.containsKey(wire.node1) || leafNodes.containsKey(wire.node2))
            return;

        conductanceDelta += Math.abs(change);
        if(countUpdates) {
            ++conductanceUpdates;
            if((wire.node1 != null && wire.node1.getIndex() >= eliminatedStart) || (wire.node2 != null && wire.node2.getIndex() >= eliminatedStart))
                ++eliminatedUpdates;
        }
        wire.stamp(this::jacobianAdd, change);
    }

    public void alterConductanceMatrix(int row, int column, double change) {
        if(JacobianKept == null || dirty || Math.abs(change) < G_MIN * 0.1)
            return;
        var restore = enableRowExchange;
        enableRowExchange = false;
        jacobianAdd(row, column, change);
        enableRowExchange = restore;
    }

    public void removeWire(AbstractElectricWire wire) {
        if(!wires.contains(wire))
            return;
        enableRowExchange = false;
        wires.remove(wire);
        wire.setNetwork(null);
        if(wire.isSource())
            --sourceCount;
        if(wire.node1 == null || wire.node2 == null)
            --groundReferenceCount;

        updateConductance(wire, -wire.conductance());
        if(wire instanceof IOuterHook hook)
            outerHooks.remove(hook);
        if(wire instanceof ISolverHook hook) {
            innerHooks.remove(hook);
            leafInnerHooks.remove(hook);
        }
        if(wire instanceof IStaticResidual residual)
            residuals.remove(residual);
        warmUp(1);
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

    public Collection<IElectricNode> getLeafs() {
        return leafNodes.keySet();
    }

    public DMatrixRMaj getStateVector() {
        return StateVector;
    }

    private ExchangeRow getOrCreateRow(int row) {
        for(var rowObj : changedRows) {
            if(rowObj.index == row)
                return rowObj;
        }
        var rowObj = new ExchangeRow(row, SCALING ? ScaledJ : ReducedJacobian != null ? ReducedJacobian : JacobianKept);
        changedRows.add(rowObj);
        return rowObj;
    }

    protected void jacobianAdd(int row, int column, double value) {
        if(value == 0)
            return;
        if(row >= nodes.size() || column >= nodes.size())
            throw new IllegalArgumentException("Provided entry lays outside of the allocated matrices.");
        if(row < eliminatedStart && column < eliminatedStart) {
            if(SCALING) {
                var scaledValue = value * columnScales[column] * rowScales[row];
                if (enableRowExchange) {
                    var e = getOrCreateRow(row);
                    e.update(column, scaledValue);
                } else {
                    A0.add(row, column, scaledValue);
                    A0.markRefactorize();
                }
                ScaledJ.add(row, column, scaledValue);
                ScaledJ.markRefactorize();
            } else {
                if (enableRowExchange) {
                    var e = getOrCreateRow(row);
                    e.update(column, value);
                } else {
                    A0.add(row, column, value);
                    A0.markRefactorize();
                }
            }
            JacobianKept.add(row, column, value);
            JacobianKept.markRefactorize();
            if(ReducedJacobian != null) {
                ReducedJacobian.add(row, column, value);
                ReducedJacobian.markRefactorize();
            }
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
        if(row >= nodes.size())
            throw new IllegalArgumentException("Provided entry lays outside of the allocated matrices.");
        if(row < eliminatedStart) {
            ReducedRHSVector.add(row, 0, value);
        } else {
            EliminatedRHSVector.add(row - eliminatedStart, 0, value);
            eliminatedRHSZero = false;
        }
    }

    protected void populateConductanceMatrix(boolean withEliminated) {
        enableRowExchange = false;
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
            var skip = false;
            for(var node : wire.coupledNodes()) {
                if(leafNodes.containsKey(node)) {
                    skip = true;
                    break;
                }
                if(node != null) {
                    if(nodes.contains(node)) {
                        if(node.getIndex() >= nodes.size()) {
                            if(LOGGER != null)
                                LOGGER.warn("Node {} has an index outside of the allocated matrix size, skipping", node);
                            skip = true;
                            break;
                        }
                    } else if(!leafNodes.containsKey(node)) {
                        if(LOGGER != null) {
                            LOGGER.warn("Dropped a stale wire (wire nodes not part of this network) between {}.", wire.coupledNodes());
                        }
                        staleWires.add(wire);
                        skip = true;
                        break;
                    }
                }
            }
            if(skip) continue;
            wire.stamp(this::jacobianAdd, G);
        }

        staleWires.forEach(wire -> {
            if(wire instanceof IOuterHook hook)
                outerHooks.remove(hook);
            if(wire instanceof ISolverHook hook) {
                innerHooks.remove(hook);
                leafInnerHooks.remove(hook);
            }
            if(wire instanceof IStaticResidual residual)
                residuals.remove(residual);
            wires.remove(wire);
        });
        for(var node : couplings) {
            try {
                // This is using the same method as wires but coupling nodes,
                // probably shouldn't target eliminated nodes.
                node.couple(this::jacobianAdd);
            } catch(IllegalArgumentException | NullPointerException e) {
                LOGGER.error("Failed to couple {}:", node, e);
            }
        }
        lockEliminated = false;

        if(addGMin) {
            boolean shouldAnchor = true;
            FloatingNode anchor = null;
            for (var node : nodes) {
                if (node instanceof CurrentSourceNode) {
                    shouldAnchor = false;
                } else if (node instanceof TransmissionLinePort) {
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

        changedRows.clear();
        enableRowExchange = true;
        A0.setTo(getWorkMatrix());
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
            JacobianEliminated.markRefactorize();
            JacobianEliminated.solve(JacobianBottom, WMatrix);
            JacobianRight.mult(WMatrix, ReducedCorrection);

            JacobianKept.subtract(ReducedCorrection, ReducedJacobian);
            ReducedJacobian.optimize();

            enableRowExchange = false;
            recalculateScales = true;
        }
        eliminatedChanged = false;
    }

    public void merge(ElectricalNetwork other) {
        if(other == this)
            return;
        other.leafNodes.forEach((node, tracked) -> {
            node.setNetwork(this);
            leafNodes.put(node, tracked);
        });
        other.nodes.forEach(this::addNode);
        other.wires.forEach(this::addWire);
        // Make the other network empty.
        other.clear();
    }

    public void clear() {
        nodes.forEach(node -> {
            if(node.getNetwork() == this)
                node.setNetwork(null);
        });
        wires.forEach(wire -> {
            if(wire.getNetwork() == this)
                wire.setNetwork(null);
        });
        leafNodes.forEach((node, $) -> {
            if(node.getNetwork() == this)
                node.setNetwork(null);
        });
        nodes.clear();
        wires.clear();
        couplings.clear();
        innerHooks.clear();
        residuals.clear();
        leafNodes.clear();
        eliminatedStart = 0;
    }

    public double getValue(INode node) {
        if(StateVector == null)
            return 0;
        if(leafNodes.containsKey(node)) {
            var tracked = leafNodes.get(node);
            if(tracked != null)
                return getValue(tracked);
            return 0;
        }
        var index = node.getIndex();
        if(index < 0 || index >= nodes.size())
            return 0;
        if(index >= eliminatedStart) {
            if(WMatrix == null || index - eliminatedStart >= WMatrix.getNumRows())
                return 0;
            double value = eliminatedSolved ? EliminatedSolved.unsafe_get(index - eliminatedStart, 0) : 0;
            for(int i = 0; i < StateVector.getNumRows(); ++i) {
                value -= WMatrix.unsafe_get(index - eliminatedStart, i) * StateVector.unsafe_get(i, 0);
            }
            return value;
        }
        if(index >= StateVector.getNumRows())
            return node.getSavedValue();
        var value = StateVector.unsafe_get(index, 0);
        return Double.isFinite(value) ? value : 0;
    }

    public void setValue(INode node, double value) {
        if(StateVector == null || dirty)
            return;
        if(leafNodes.containsKey(node))
            return;
        var index = node.getIndex();
        if(index < 0 || index >= nodes.size())
            return;
        if(index >= eliminatedStart)
            return;
        if(index >= StateVector.getNumRows())
            return;
        StateVector.unsafe_set(index, 0, value);
    }

    private void validateJacobian(DynamicallyTypedMatrix jacobian, DMatrixRMaj residual) {
        var n = jacobian.getNumRows();
        var v = new DMatrixRMaj(n, 1);
        RandomMatrices_DDRM.fillUniform(v, new Random());

        var epsilon = Math.sqrt(Math.ulp(1)) * (1 + NormOps_DDRM.normP1(StateVector));
        var left = new DMatrixRMaj(n, 1);
        jacobian.mult(v, left);

        computeResidual(jacobian, StateVector);
        var residualBase = new DMatrixRMaj(residual);
        var state2 = new DMatrixRMaj(n, 1);
        CommonOps_DDRM.add(StateVector, epsilon, v, state2);
        computeResidual(jacobian, state2);
        var right = new DMatrixRMaj(n, 1);
        CommonOps_DDRM.subtract(residual, residualBase, right);

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

    public void optimizeNode(@NotNull INode node) {
        if(leafNodes.containsKey(node))
            return;
        assert node.getNetwork() == this : "Node is not part of this network";
        if(node.getIndex() >= eliminatedStart) {
            // Already in the correct spot.
            return;
        }
        if(node.getIndex() == eliminatedStart - 1) {
            // Simply move pointer back by one to include this node.
            --eliminatedStart;
            dirty = true;
        } else {
            // Swap nodes and move eliminated pointer
            var other = nodes.get(--eliminatedStart);
            swapNodes(node, other);
        }
    }

    public void unoptimizeNode(@NotNull INode node) {
        if(leafNodes.containsKey(node))
            return;
        assert node.getNetwork() == this : "Node is not part of this network";
        if(node.getIndex() < eliminatedStart) {
            // Already in the correct spot.
            return;
        }
        if(node.getIndex() == eliminatedStart) {
            // Simply move pointer forward by one to include this node.
            ++eliminatedStart;
            dirty = true;
        } else {
            // Swap nodes and move eliminated pointer
            var other = nodes.get(++eliminatedStart);
            swapNodes(node, other);
        }
    }

    public void makeLeaf(IElectricNode node, IElectricNode tracked) {
        assert node != tracked;
        if(nodes.contains(node)) {
            internalRemoveNode(node);
            leafNodes.put(node, tracked);
            node.setNetwork(this);

            var iter = innerHooks.iterator();
            while(iter.hasNext()) {
                var hook = iter.next();
                for(var coupled : hook.coupledNodes()) {
                    if(node == coupled) {
                        // Disable hook because this node is not simulated anymore
                        leafInnerHooks.add(hook);
                        iter.remove();
                    }
                }
            }
        } else if(leafNodes.containsKey(node)) {
            leafNodes.put(node, tracked);
        }
    }

    public void removeLeaf(IElectricNode node) {
        if(!nodes.contains(node)) {
            if(leafNodes.containsKey(node)) {
                leafNodes.remove(node);
                addNode(node);

                var iter = leafInnerHooks.iterator();
                while(iter.hasNext()) {
                    var hook = iter.next();
                    var isFull = true;
                    for(var coupled : hook.coupledNodes()) {
                        if(isLeaf(coupled)) {
                            isFull = false;
                            break;
                        }
                    }
                    if(isFull) {
                        // Enable hook since all nodes are simulate
                        innerHooks.add(hook);
                        iter.remove();
                    }
                }
            }
        }
    }

    public boolean isLeaf(IElectricNode node) {
        return leafNodes.containsKey(node);
    }

    public void unoptimizeAll() {
        if(eliminatedStart != nodes.size()) {
            eliminatedStart = nodes.size();
            dirty = true;
        }
    }

    public boolean isOptimized(INode node) {
        return node.getIndex() >= eliminatedStart;
    }

    private int eliminatedNodeCount() {
        return nodes.size() - eliminatedStart;
    }

    protected void prepareMatrices(int multiTicks) {
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
            var NewState = new DMatrixRMaj(reducedCount, 1);
            // Use previous state matrix to accelerate warm up
            if(StateVector != null) {
                for(int i = 0; i < reducedCount; ++i) {
                    NewState.unsafe_set(i, 0, getValue(nodes.get(i)));
                }
            }

            // Kept Jacobian
            JacobianKept = new DynamicallyTypedMatrix(reducedCount, reducedCount, DynamicallyTypedMatrix.Solver.LU);
            A0 = new DynamicallyTypedMatrix(reducedCount, reducedCount, DynamicallyTypedMatrix.Solver.LU);
            if(eliminatedCount != 0) {
                // Eliminated Jacobian
                JacobianEliminated = new DynamicallyTypedMatrix(eliminatedCount, eliminatedCount);
                // Eliminated x Reduced Jacobian
                JacobianBottom = new DynamicallyTypedMatrix(eliminatedCount, reducedCount);
                // Reduced x Eliminated Jacobian
                JacobianRight = new DynamicallyTypedMatrix(reducedCount, eliminatedCount);

                WMatrix = new DynamicallyTypedMatrix(eliminatedCount, reducedCount);
                ReducedCorrection = new DynamicallyTypedMatrix(reducedCount, reducedCount);
                ReducedJacobian = new DynamicallyTypedMatrix(reducedCount, reducedCount, DynamicallyTypedMatrix.Solver.LU);

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

            if(SCALING)
                ScaledJ = new DynamicallyTypedMatrix(reducedCount, reducedCount, DynamicallyTypedMatrix.Solver.LU);
            ResidualVector = new DMatrixRMaj(reducedCount, 1);
            AuxiliaryVector = new DMatrixRMaj(reducedCount, 1);
            StateVector = NewState;

            if(SCALING) {
                columnScales = new double[reducedCount];
                rowScales = new double[reducedCount];
            }
            solver.setStateSize(reducedCount);
            dirty = false;

            currentMultiTick = multiTicks;
            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
            populateConductanceMatrix(true);
            scalesAge = MAX_SCALE_REUSE_COUNT + 1;
        } else if(conductanceUpdates >= reducedCount * 40 || conductanceDelta > 1000 || (eliminatedUpdates >= eliminatedCount * 40 && eliminatedCount != 0)) {
            // To prevent resistance from deviating due to floating point imprecision sometimes we rebuild
            // the matrices from scratch.
            currentMultiTick = multiTicks;
            if(LOGGER != null && ModdedConfigs.logsEnabled())
                LOGGER.debug("Cumulated conductance updates triggered admittance matrix recalculation ({} {} {})",
                        conductanceUpdates, conductanceDelta, eliminatedUpdates);
            populateConductanceMatrix(eliminatedUpdates >= eliminatedCount * 40);
        } else if(currentMultiTick != multiTicks) {
            var old = currentMultiTick;
            for(var wire : wires) {
                if(wire instanceof ITimeAwareWire) {
                    var Gold = wire.conductance();
                    currentMultiTick = multiTicks;
                    var Gnew = wire.conductance();
                    currentMultiTick = old;
                    updateConductance(wire, Gnew - Gold);
                }
            }
            currentMultiTick = multiTicks;
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
        if(scalesAge >= MAX_SCALE_REUSE_COUNT) {
            computeScales(workMatrix);
            recalculateScales = true;
        }
        if(recalculateScales) {
            workMatrix.multColumns(columnScales, ScaledJ);
            ScaledJ.multRows(rowScales, null);
            ScaledJ.markRefactorize();
            // Make sure to drop all exchanged rows
            enableRowExchange = false;
            recalculateScales = false;
        }
    }

    private void computeRHS() {
        ReducedRHSVector.zero();
        if(EliminatedRHSVector != null)
            EliminatedRHSVector.zero();
        eliminatedRHSZero = true;
        for(var residual : residuals) {
            var skip = false;
            for(var node : residual.affectedNodes()) {
                if(leafNodes.containsKey(node)) {
                    skip = true;
                    break;
                }
            }
            if(!skip)
                residual.addStaticResidual(this::rhsAdd);
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
//        workMatrix.mult(state, ResidualVector);
        ResidualVector.zero();
        CommonOps_DDRM.subtract(ResidualVector, ReducedRHSVector, ResidualVector);
        for(var hook : innerHooks) {
            hook.addResidual(this::residualAdd);
        }
        CommonOps_DDRM.changeSign(ResidualVector);
    }

    private void columnScales(DynamicallyTypedMatrix matrix) {
        int n = matrix.getNumRows();
        for(int i = 0; i < n; ++i) {
            double max = 0;
            for(int j = 0; j < n; ++j) {
                var v = Math.abs(matrix.unsafe_get(j, i));
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
                var v = Math.abs(matrix.unsafe_get(i, j));
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
        int n = workMatrix.getNumRows();
        for(int i = 0; i < n; ++i) {
            if(nodes.get(i) instanceof ICouplingNode) {
                columnScales[i] = rowScales[i] = 1;
                continue;
            }
            double max = 0;
            for(int j = 0; j < n; ++j)  {
                var v = Math.abs(workMatrix.unsafe_get(i, j));
                max += v * v;
            }
            if(max == 0) {
                columnScales[i] = rowScales[i] = 1;
                continue;
            }
            columnScales[i] = rowScales[i] = Math.sqrt(Math.min(1.0 / Math.sqrt(max), 2000));
        }
//        columnScales(workMatrix);
//        workMatrix.multColumns(columnScales, ScaledJ);
//        rowScales(ScaledJ);
        scalesAge = 0;
    }

    public List<INode> findProblematicNodes(DMatrixRMaj residual, double threshold) {
        var nodes = new ArrayList<INode>();
        for(var node : this.nodes) {
            if(node.getIndex() >= eliminatedStart)
                continue;
            var x = residual.get(node.getIndex(), 0);
            if(Math.abs(x) > threshold) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private void iterHooks(int i, int max, double norm) {
        if(hasHooks() && i < max - 10) {
            countUpdates = false;
            for(var hook : innerHooks) {
                hook.startIteration();
            }
            countUpdates = true;
        }
    }

    protected void convergenceProblems(double residual, DMatrixRMaj ResidualVector) {

    }

    private void verifyConvergence(double norm, int i, int maxIterations, DMatrixRMaj ResidualVector) {
        if (norm > minimumAllowedPrecision) {
            if(converged)
                convergenceProblems(norm, ResidualVector);
            converged = false;
            // Drop exchanged rows since they might reduce precision
            enableRowExchange = false;
            if (LOGGER != null) {
                if (ModdedConfigs.logsEnabled()) {
                    LOGGER.warn("Solution possibly not converged after {} Newton iterations, final norm: {}", i, norm);
                }
            } else {
                System.out.printf("Solution possibly not converged after %d Newton iterations, final norm: %g\n", i, norm);
            }
        } else {
            converged = i < maxIterations - 10;
            if (!converged) {
                if (LOGGER != null) {
                    LOGGER.debug("Dirty converge at {} iterations", i);
                } else {
                    System.out.printf("Solution possibly not converged (residual recalculation was disabled) after %d Newton iterations\n", i);
                }
            } else {
                if (LOGGER == null) {
                    System.out.printf("Converged after %d iterations\n", i);
                }
            }
            if (converged && warmUpTicks > 0) {
                // This effectively freezes component states and allows the network
                // to settle completely after a structure change (or world load).
                --warmUpTicks;
                converged = false;
            }
        }
    }

    @Override
    public int getStamp() {
        return stamp;
    }

    public void prepare(int multiTicks) {
        if(sourceCount == 0) {
            converged = true;
            for(var hook : outerHooks)
                hook.preSolve();
            for(var hook : innerHooks)
                hook.startIteration();
            if(StateVector != null)
                StateVector.zero();
            for(var hook : outerHooks)
                hook.postUpperSolve();
            return;
        }

        // Verify possession of nodes
        for(var node : nodes) {
            if(node.getNetwork() != this) {
                PowerGrid.LOGGER.warn("INVALID NODE POSSESSION {}", node);
            }
        }

        prepareMatrices(multiTicks);
    }

    public void singleTick() {
        ++stamp;
        if(sourceCount == 0)
            return;
        PERF.start();
        for (var hook : outerHooks)
            hook.preSolve();
        if(eliminatedChanged) {
            // Make sure eliminated Jacobian is factorized for the RHS compute
            calculateEliminatedMatrices();
        }
        computeRHS();

        int maxIterations = this.maxIterations.apply(hasHooks());
        int i;
        double norm = 0;
        for (i = 0; i < maxIterations; ++i) {
            iterHooks(i, maxIterations, norm);
            var workMatrix = getWorkMatrix();
            computeResidual(workMatrix, StateVector);

            workMatrix.mult(StateVector, AuxiliaryVector);
            CommonOps_DDRM.subtract(AuxiliaryVector, ResidualVector, AuxiliaryVector);
            var nextNorm = NormOps_DDRM.normP1(AuxiliaryVector);
            var dNorm = Math.abs(nextNorm - norm);
            norm = nextNorm;
            if (norm < absoluteStoppingCriterion || dNorm < relativeStoppingCriterion)
                break;
            if (converged && i >= maxIterations - 12) {
                // Right before non-linear devices are disabled.
                // Only append new problem frames if the network has been converging before.
                converged = norm < minimumAllowedPrecision;
                if(!converged)
                    convergenceProblems(norm, AuxiliaryVector);
            }

            if(SCALING) {
                prepareScaled(workMatrix);
                CommonOps_DDRM.multRows(rowScales, ResidualVector);
                workMatrix = ScaledJ;
            }

            if(enableRowExchange) {
                var valid = true;
                var rowRecalc = A0.isMarked();
                for (var row : changedRows) {
                    var status = row.solveRow(A0, rowRecalc, changedRows);
                    if(status == 2) {
                        // Drop changed rows
                        valid = false;
                        break;
                    } else if(status == 1) {
                        rowRecalc = true;
                    }
                }
                if(valid) {
                    for (int j = changedRows.size() - 1; j >= 0; --j) {
                        changedRows.get(j).apply(ResidualVector);
                    }
                    workMatrix = A0;
                } else {
                    changedRows.clear();
                    A0.setTo(workMatrix);
                    A0.markRefactorize();
                }
            } else {
                changedRows.clear();
                A0.setTo(workMatrix);
                A0.markRefactorize();
                enableRowExchange = true;
            }

            var deltaX = solver.solve(workMatrix, ResidualVector, false);
            if (deltaX == null)
                continue;

            var valid = !MatrixFeatures_DDRM.hasUncountable(deltaX);
            if (valid) {
                if(SCALING)
                    CommonOps_DDRM.multRows(columnScales, deltaX);
                StateVector.setTo(deltaX);
            } else {
                StateVector.zero();
                solver.zero();
            }
        }
        verifyConvergence(norm, i, maxIterations, AuxiliaryVector);
        for (var hook : outerHooks)
            hook.postUpperSolve();
        PERF.end();
    }

    public void calculate(int multiTicks) {
        prepare(multiTicks);
        for(int t = 0; t < multiTicks; ++t) {
            singleTick();
        }
    }

    public enum SolverType {
        DIRECT, BICGSTAB, GMRES
    }

    private static class ExchangeRow {
        private final int index;
        private final DMatrixRMaj changedRow;
        private final DMatrixRMaj solvedCoefficients;
        private boolean recalculate;
        private int unmodifiedFor;

        public ExchangeRow(int index, DynamicallyTypedMatrix rowSource) {
            this.index = index;
            int size = rowSource.getNumRows();
            changedRow = new DMatrixRMaj(1, size);
            for(int i = 0; i < size; ++i) {
                changedRow.unsafe_set(0, i, rowSource.get(index, i));
            }
            solvedCoefficients = new DMatrixRMaj(1, size);
            recalculate = true;
        }

        public void update(int index, double change) {
            changedRow.add(0, index, change);
            recalculate = true;
            unmodifiedFor = 0;
        }

        public int solveRow(DynamicallyTypedMatrix A0, boolean force, List<ExchangeRow> allRows) {
            if(!recalculate && !force) {
                ++unmodifiedFor;
                return 0;
            }
            A0.solveRow(changedRow, solvedCoefficients);
            // Apply rows up to this row (assumes that the collection has fixed ordering)
            var vec = solvedCoefficients.getData();
            for(var row : allRows) {
                if(row == this)
                    break;
                var x = vec[row.index] / row.solvedCoefficients.unsafe_get(0, row.index);
                vec[row.index] = x;
                for(int i = 0; i < changedRow.getNumCols(); ++i) {
                    if(i == row.index)
                        continue;
                    vec[i] -= x * row.solvedCoefficients.unsafe_get(0, i);
                    if(Math.abs(vec[i]) > 1e+6 || !Double.isFinite(vec[i])) {
                        // The numerical errors might cause convergence issues
                        return 2;
                    }
                }
            }
            recalculate = false;
            return 1;
        }

        public void apply(DMatrixRMaj residuals) {
            double x = residuals.unsafe_get(index, 0);
            for(int i = 0; i < solvedCoefficients.getNumCols(); ++i) {
                if(i != index)
                    x -= residuals.unsafe_get(i, 0) * solvedCoefficients.unsafe_get(0, i);
            }
            residuals.unsafe_set(index, 0, x / solvedCoefficients.unsafe_get(0, index));
        }
    }
}
