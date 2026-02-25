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
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.CSolver;
import org.patryk3211.powergrid.electricity.sim.calculation.IStamped;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.solver.*;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePort;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

public class ElectricalNetwork implements IStamped {
    public static final double G_MIN = 1e-8;

    public double bjtSmoothAlpha = 0.5;
    public double diodeSmoothAlpha = 0.025;

    private static final PerformanceCounter PERF = new PerformanceCounter("NetSolve");

    private final boolean addGMin;
    protected final Set<AbstractElectricWire> wires = new HashSet<>();
    protected final Set<ICouplingNode> couplings = new HashSet<>();
    protected final List<INode> nodes = new ArrayList<>();

    protected final Set<IOuterHook> outerHooks = new HashSet<>();
    public final Set<ISolverHook> innerHooks = new HashSet<>();
    protected final Set<ISolverHook> leafInnerHooks = new HashSet<>();
    protected final Set<IStaticResidual> residuals = new HashSet<>();
    protected final Map<IElectricNode, IElectricNode> leafNodes = new Reference2ReferenceOpenHashMap<>();

    private int sourceCount;
    private int groundReferenceCount;

    protected boolean dirty;
    private double conductanceDelta = 0;
    private int conductanceUpdates = 0;
    public boolean countUpdates = true;
    protected int stamp;
    private int currentMultiTick = 1;

    public static Logger LOGGER = null;

    public Function<Boolean, Integer> maxIterations = b -> 200;

    private IMNA mna;

    public ElectricalNetwork(boolean addGMin) {
        dirty = true;
        sourceCount = 0;
        this.addGMin = addGMin;
        this.mna = new JavaMNA(this);
    }

    public ElectricalNetwork(boolean addGMin, Function<ElectricalNetwork, IMNA> mna) {
        dirty = true;
        sourceCount = 0;
        this.addGMin = addGMin;
        this.mna = mna != null ? mna.apply(this) : null;
    }

    public void switchBackend(CSolver.SolverBackend backend) {
        if(mna == null)
            return;
        if(mna.type() == backend)
            return;
        if(!backend.isSupported())
            return;
        mna.cleanup();
        dirty = true;
        mna = backend.create(this);
        warmUp(1);
    }

    public void cleanup() {
        mna.cleanup();
        // We must not use the backend after it has been cleaned up.
        mna = null;
    }

    public void setPrecision(double absoluteCriterion, double relativeCriterion, double minimumPrecision) {
        mna.setPrecision(absoluteCriterion, relativeCriterion, minimumPrecision);
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
        if(mna != null)
            mna.warmUp(ticks > 0 ? 1 : ticks);
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

    // This method should be much faster than ArrayList.contains()
    protected boolean hasNode(INode node) {
        if(node == null)
            return false;
        int index = node.getIndex();
        if(index < 0 || index >= nodes.size())
            return false;
        // If this network owns this node it will be at the recorded index.
        return nodes.get(index) == node;
    }

    public void addNode(INode node) {
        if(hasNode(node) || leafNodes.containsKey(node))
            return;
        node.assignIndex(nodes.size());
        node.setNetwork(this);
        nodes.add(node);
        setDirty();

        if(node instanceof IOuterHook hook)
            outerHooks.add(hook);
        if(node instanceof ISolverHook hook) {
            innerHooks.add(hook);
            if(mna != null)
                mna.hooksChanged();
        }
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
        if(node.getIndex() == -1 && node.getNetwork() == this)
            return;
        if(node.getNetwork() != this || node.getIndex() >= nodes.size() || nodes.get(node.getIndex()) != node)
            // This node is not actually in this network.
            return;

        if(mna != null) {
            var StateVector = mna.stateVector();
            for (int i = node.getIndex() + 1; i < nodes.size(); ++i) {
                // Move back all nodes by one.
                nodes.get(i).assignIndex(i - 1);
                StateVector.safe_set(i - 1, 0, StateVector.safe_get(i, 0));
            }
        } else {
            for (int i = node.getIndex() + 1; i < nodes.size(); ++i) {
                // Move back all nodes by one.
                nodes.get(i).assignIndex(i - 1);
            }
        }
        nodes.remove(node.getIndex());

        if(node instanceof ICouplingNode)
            couplings.remove(node);
        if(node.isSource())
            --sourceCount;
        if(node instanceof IOuterHook hook)
            outerHooks.remove(hook);
        if(node instanceof ISolverHook hook) {
            innerHooks.remove(hook);
            if(mna != null)
                mna.hooksChanged();
        }
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
        return mna != null ? mna.isConverged() : false;
    }

    public void addWire(AbstractElectricWire wire) {
        if(wire.node1 != null && !hasNode(wire.node1) && !leafNodes.containsKey(wire.node1)) {
            // If node of a wire is not null it must be in the network's node set.
            var suffix = wire.node1.getNetwork() == null ? "no network" : "different network";
            if(LOGGER == null) {
                throw new IllegalArgumentException("Both nodes of a wire must be part of the network (node1 " + wire.node1 + " isn't - " + suffix + ")");
            } else {
                LOGGER.error("Both nodes of a wire must be part of the network (node1 {} isn't - {})", wire.node1, suffix);
                LOGGER.error("Stack trace", new Throwable());
            }
        }
        if(wire.node2 != null && !hasNode(wire.node2) && !leafNodes.containsKey(wire.node2)) {
            // If node of a wire is not null it must be in the network's node set.
            var suffix = wire.node2.getNetwork() == null ? "no network" : "different network";
            if(LOGGER == null) {
                throw new IllegalArgumentException("Both nodes of a wire must be part of the network (node2 " + wire.node2 + " isn't - " + suffix + ")");
            } else {
                LOGGER.error("Both nodes of a wire must be part of the network (node2 {} isn't - {})", wire.node2, suffix);
                LOGGER.error("Stack trace", new Throwable());
            }
        }
        if(!wires.add(wire))
            return;
        if(mna != null)
            mna.rowExchange(false);
        wire.setNetwork(this);
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
                if(mna != null)
                    mna.hooksChanged();
            } else {
                leafInnerHooks.add(hook);
            }
        }
        if(wire instanceof IStaticResidual residual)
            residuals.add(residual);
        warmUp(1);
    }

    public void updateConductance(AbstractElectricWire wire, double change) {
        if(dirty || Math.abs(change) < G_MIN * 0.1)
            return;
        if(leafNodes.containsKey(wire.node1) || leafNodes.containsKey(wire.node2))
            return;
        if(!hasNode(wire.node1) || !hasNode(wire.node2))
            return;
        if(wire.node1.getIndex() == -1 || wire.node2.getIndex() == -1) {
            PowerGrid.LOGGER.error("Node index negative even though it shouldn't be?", new Throwable());
            return;
        }

        conductanceDelta += Math.abs(change);
        if(countUpdates) {
            ++conductanceUpdates;
        }
        wire.stamp(mna::jacobianAdd, change);
    }

    public void alterConductanceMatrix(int row, int column, double change) {
        if(dirty || Math.abs(change) < G_MIN * 0.1)
            return;
        var restore = mna.rowExchange();
        mna.rowExchange(false);
        mna.jacobianAdd(row, column, change);
        mna.rowExchange(restore);
    }

    public void removeWire(AbstractElectricWire wire) {
        if(!wires.contains(wire))
            return;
        if(mna != null)
            mna.rowExchange(false);
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
            if(mna != null)
                mna.hooksChanged();
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

    public List<INode> getNodes() {
        return nodes;
    }

    public Collection<IElectricNode> getLeafs() {
        return leafNodes.keySet();
    }

    protected void populateConductanceMatrix() {
        mna.jacobianPrepareForWrite();
        conductanceDelta = 0;
        conductanceUpdates = 0;

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
                    if(hasNode(node)) {
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
            wire.stamp(mna::jacobianAdd, G);
        }

        staleWires.forEach(wire -> {
            if(wire instanceof IOuterHook hook)
                outerHooks.remove(hook);
            if(wire instanceof ISolverHook hook) {
                innerHooks.remove(hook);
                leafInnerHooks.remove(hook);
                mna.hooksChanged();
            }
            if(wire instanceof IStaticResidual residual)
                residuals.remove(residual);
            wires.remove(wire);
        });
        for(var node : couplings) {
            try {
                // This is using the same method as wires but coupling nodes,
                // probably shouldn't target eliminated nodes.
                node.couple(mna::jacobianAdd);
            } catch(IllegalArgumentException | NullPointerException e) {
                LOGGER.error("Failed to couple {}:", node, e);
            }
        }

        if(addGMin) {
            boolean shouldAnchor = true;
            FloatingNode anchor = null;
            for (var node : nodes) {
                if (node instanceof CurrentSourceNode) {
                    shouldAnchor = false;
                } else if (node instanceof TransmissionLinePort) {
                    shouldAnchor = false;
                } else if (node instanceof VoltageSourceCoupling source) {
                    if (anchor == null && source.getNegative() instanceof FloatingNode floating) {
                        anchor = floating;
                    }
                } else if (node instanceof FloatingNode) {
                    mna.jacobianAdd(node.getIndex(), node.getIndex(), G_MIN);
                }
            }
            if(groundReferenceCount == 0) {
                // Add a shunt to ground to the first floating node.
                // This ensures that the simulation is anchored to a 0V reference somewhere
                // and should improve performance and stability when there are only 2 port sources.
                if (shouldAnchor && anchor != null) {
                    mna.jacobianAdd(anchor.getIndex(), anchor.getIndex(), 1000);
                }
            }
        }

        mna.finishJacobianWrite();
    }

    public void merge(ElectricalNetwork other) {
        if(other == this)
            return;
        other.leafNodes.forEach((node, tracked) -> {
            node.setNetwork(this);
            leafNodes.put(node, tracked);
        });
        other.nodes.forEach(this::addNode);
        other.wires.forEach(wire -> {
            // Drop stale wires
            if((wire.node1 == null || hasNode(wire.node1) || leafNodes.containsKey(wire.node1)) &&
                    (wire.node2 == null || hasNode(wire.node2) || leafNodes.containsKey(wire.node2))) {
                addWire(wire);
            } else if(LOGGER != null) {
                LOGGER.warn("Dropped stale wire {} between {} and {}", wire, wire.node1, wire.node2);
            }
        });
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
        if(mna != null)
            mna.hooksChanged();
    }

    protected Double tryGetValue(INode node) {
        if(leafNodes.containsKey(node)) {
            var tracked = leafNodes.get(node);
            if(tracked != null)
                return getValue(tracked);
            return 0.0;
        }
        var index = node.getIndex();
        if(index == -1)
            return null;
        if(index < 0 || index >= nodes.size())
            return 0.0;
        var value = mna.stateVector().safe_get(index, 0);
        return Double.isFinite(value) ? value : 0.0;
    }

    public double getValue(INode node) {
        var val = tryGetValue(node);
        return val == null ? 0 : val;
    }

    public void setValue(INode node, double value) {
        if(dirty)
            return;
        if(leafNodes.containsKey(node))
            return;
        var index = node.getIndex();
        if(index < 0 || index >= nodes.size())
            return;
        var StateVector = mna.stateVector();
        if(index >= StateVector.numRows())
            return;
        StateVector.safe_set(index, 0, value);
    }

    public void swapNodes(INode node1, INode node2) {
        var index1 = node1.getIndex();
        var index2 = node2.getIndex();

        nodes.set(index1, node2);
        nodes.set(index2, node1);

        var StateVector = mna.stateVector();
        var node2Value = StateVector.safe_get(index2, 0);
        StateVector.safe_set(index2, 0, StateVector.safe_get(index1, 0));
        StateVector.safe_set(index1, 0, node2Value);

        node1.assignIndex(index2);
        node2.assignIndex(index1);
        // Jacobian needs a rebuild
        dirty = true;
    }

    public void makeLeaf(IElectricNode node, IElectricNode tracked) {
        assert node != tracked;
        if(hasNode(node)) {
            internalRemoveNode(node);
            leafNodes.put(node, tracked);
            node.assignIndex(-1);
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
        if(!hasNode(node)) {
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
                        mna.hooksChanged();
                        iter.remove();
                    }
                }
            }
        }
    }

    public boolean isLeaf(IElectricNode node) {
        return leafNodes.containsKey(node);
    }

    protected void prepareMatrices(int multiTicks) {
        var nodeCount = nodes.size();
        if(dirty) {
            mna.allocate(nodeCount);
            dirty = false;

            currentMultiTick = multiTicks;
            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
            populateConductanceMatrix();
        } else if(conductanceUpdates >= nodeCount * 40 || conductanceDelta > 1000) {
            // To prevent resistance from deviating due to floating point imprecision sometimes we rebuild
            // the matrices from scratch.
            currentMultiTick = multiTicks;
            if(LOGGER != null && ModdedConfigs.logsEnabled())
                LOGGER.debug("Cumulated conductance updates triggered admittance matrix recalculation ({} {})",
                        conductanceUpdates, conductanceDelta);
            populateConductanceMatrix();
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

    private void computeRHS() {
        mna.zeroRHS();
        for(var residual : residuals) {
            var skip = false;
            for(var node : residual.affectedNodes()) {
                if(leafNodes.containsKey(node)) {
                    skip = true;
                    break;
                }
            }
            if(!skip)
                residual.addStaticResidual(mna::rhsAdd);
        }
    }

    public List<INode> findProblematicNodes(IMatrixAccess residual, double threshold) {
        var nodes = new ArrayList<INode>();
        for(var node : this.nodes) {
            var x = residual.get(node.getIndex(), 0);
            if(Math.abs(x) > threshold) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public void convergenceProblems(double residual, IMatrixAccess ResidualVector) {

    }

    @Override
    public int getStamp() {
        return stamp;
    }

    public void prepare(int multiTicks) {
        if(sourceCount == 0) {
            for(var hook : outerHooks)
                hook.preSolve();
            for(var hook : innerHooks)
                hook.startIteration(0);
            mna.zeroState();
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
        computeRHS();

        mna.singleTick();

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
}
