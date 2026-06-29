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
package org.patryk3211.powergrid.electricity.sim.solver;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.CSolver;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.PerformanceCounter;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;

import java.util.ArrayList;
import java.util.List;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.LOGGER;

public class JavaMNA implements IMNA {
    private static final PerformanceCounter PERF = new PerformanceCounter("JavaMNA");
    private static final int MAX_SCALE_REUSE_COUNT = 50;
    private static final boolean SCALING = true;
    private static final boolean ROW_EXCHANGE = false;

    public final ElectricalNetwork network;

    private DynamicallyTypedMatrix Jacobian;
    private DynamicallyTypedMatrix A0;

    public DMatrixRMaj ResidualVector;
    private DMatrixRMaj RHSVector;

    private DynamicallyTypedMatrix ScaledJ;
    protected DMatrixRMaj StateVector;
    protected DMatrixRMaj ErrorVector;
    protected DMatrixRMaj StateDelta;

    private double[] columnScales;
    private double[] rowScales;

    private final StateAccess stateAccess = new StateAccess();
    private final ResidualAccess residualAccess = new ResidualAccess();

    private int scalesAge = 0;
    protected boolean converged;
    protected int warmUpTicks = 0;

    private boolean enableRowExchange = false;
    private boolean recalculateScales = false;

    private double minimumAllowedPrecision = 1e-6;
    private double absoluteStoppingCriterion = 1e-7;
    private double relativeStoppingCriterion = 1e-14;
    private double maxSearchAlpha = 0.99;

    private final List<ExchangeRow> changedRows = new ArrayList<>();

    public JavaMNA(ElectricalNetwork network) {
        this.network = network;
    }

    @Override
    public CSolver.SolverBackend type() {
        return CSolver.SolverBackend.JAVA;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void hooksChanged() {

    }

    @Override
    public void setPrecision(double absoluteCriterion, double relativeCriterion, double minimumPrecision, double searchAlpha) {
        this.absoluteStoppingCriterion = absoluteCriterion;
        this.relativeStoppingCriterion = relativeCriterion;
        this.minimumAllowedPrecision = minimumPrecision;
        this.maxSearchAlpha = searchAlpha;
    }

    @Override
    public void warmUp(int ticks) {
        if(ticks == -1 || warmUpTicks == -1) {
            warmUpTicks = -1;
            return;
        }
        converged = false;
        if(warmUpTicks < ticks)
            warmUpTicks = ticks;
    }

    private ExchangeRow getOrCreateRow(int row) {
        for(var rowObj : changedRows) {
            if(rowObj.index == row)
                return rowObj;
        }
        var rowObj = new ExchangeRow(row, SCALING ? ScaledJ : Jacobian);
        changedRows.add(rowObj);
        return rowObj;
    }

    @Override
    public void jacobianAdd(int row, int column, double value) {
        if(value == 0)
            return;
        var nodes = network.getNodes();
        if(row >= nodes.size() || column >= nodes.size())
            throw new IllegalArgumentException("Provided entry lays outside of the allocated matrices.");
        if(SCALING) {
            var scaledValue = value * columnScales[column] * rowScales[row];
            if(ROW_EXCHANGE) {
                if (enableRowExchange) {
                    var e = getOrCreateRow(row);
                    e.update(column, scaledValue);
                } else {
                    A0.add(row, column, scaledValue);
                    A0.markRefactorize();
                }
            }
            ScaledJ.add(row, column, scaledValue);
            ScaledJ.markRefactorize();
        } else {
            if(ROW_EXCHANGE) {
                if (enableRowExchange) {
                    var e = getOrCreateRow(row);
                    e.update(column, value);
                } else {
                    A0.add(row, column, value);
                    A0.markRefactorize();
                }
            }
        }
        Jacobian.add(row, column, value);
        Jacobian.markRefactorize();
    }

    @Override
    public void rhsAdd(int row, double value) {
        if(value == 0)
            return;
        var nodes = network.getNodes();
        if(row >= nodes.size())
            throw new IllegalArgumentException("Provided entry lays outside of the allocated matrices.");
        RHSVector.add(row, 0, value);
    }

    private void computeScales(DynamicallyTypedMatrix workMatrix) {
        var nodes = network.getNodes();
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
        scalesAge = 0;
    }

    @Override
    public void allocate(int size) {
        var NewState = new DMatrixRMaj(size, 1);
        // Use previous state matrix to accelerate warm up
        if(StateVector != null) {
            var nodes = network.getNodes();
            for(int i = 0; i < size; ++i) {
                NewState.unsafe_set(i, 0, network.getValue(nodes.get(i)));
            }
        }

        Jacobian = new DynamicallyTypedMatrix(size, size, DynamicallyTypedMatrix.Solver.LU);
        if(ROW_EXCHANGE)
            A0 = new DynamicallyTypedMatrix(size, size, DynamicallyTypedMatrix.Solver.LU);
        RHSVector = new DMatrixRMaj(size, 1);

        if(SCALING)
            ScaledJ = new DynamicallyTypedMatrix(size, size, DynamicallyTypedMatrix.Solver.LU);
        ResidualVector = new DMatrixRMaj(size, 1);
        ErrorVector = new DMatrixRMaj(size, 1);
        StateDelta = new DMatrixRMaj(size, 1);
        StateVector = NewState;

        if(SCALING) {
            columnScales = new double[size];
            rowScales = new double[size];
        }

        // Invalidate scales
        scalesAge = MAX_SCALE_REUSE_COUNT + 1;
    }

    private void iterHooks(int i, int max) {
        network.countUpdates = false;
        for(var hook : network.innerHooks) {
            hook.startIteration(i);
        }
        network.countUpdates = true;
    }

    private void residualAdd(int row, double value) {
        if(row >= ResidualVector.getNumRows())
            return;
        ResidualVector.add(row, 0, value);
    }

    private void computeResidual() {
        ResidualVector.zero();
        CommonOps_DDRM.subtract(ResidualVector, RHSVector, ResidualVector);
        for(var hook : network.innerHooks) {
            hook.addResidual(this::residualAdd);
        }
        CommonOps_DDRM.changeSign(ResidualVector);
    }

    private void verifyConvergence(double norm, int i, int maxIterations) {
        if (norm > minimumAllowedPrecision) {
            if(converged)
                network.convergenceProblems(norm, residualAccess);
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
            converged = true;
            if(LOGGER == null) {
                System.out.printf("Converged after %d iterations\n", i);
            }
            if(warmUpTicks > 0) {
                // This effectively freezes component states and allows the network
                // to settle completely after a structure change (or world load).
                --warmUpTicks;
                converged = false;
            }
        }
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

    @Override
    public void singleTick() {
        PERF.start();
        int maxIterations = network.maxIterations.apply(network.hasHooks());
        int i;
        double norm = 0;
        for (i = 0; i < maxIterations; ++i) {
            if(i == 0)
                iterHooks(i, maxIterations);
            var workMatrix = Jacobian;
            computeResidual();

            workMatrix.mult(StateVector, ErrorVector);
            CommonOps_DDRM.subtract(ErrorVector, ResidualVector, ErrorVector);
            var nextNorm = CommonOps_DDRM.elementMaxAbs(ErrorVector);
            var dNorm = Math.abs(nextNorm - norm);
            norm = nextNorm;
            if (norm < absoluteStoppingCriterion || dNorm < relativeStoppingCriterion)
                break;

            if(SCALING) {
                prepareScaled(workMatrix);
                CommonOps_DDRM.multRows(rowScales, ResidualVector);
                workMatrix = ScaledJ;
            }

            if(ROW_EXCHANGE) {
                if (enableRowExchange) {
                    var valid = true;
                    var rowRecalc = A0.isMarked();
                    for (var row : changedRows) {
                        var status = row.solveRow(A0, rowRecalc, changedRows);
                        if (status == 2) {
                            // Drop changed rows
                            valid = false;
                            break;
                        } else if (status == 1) {
                            rowRecalc = true;
                        }
                    }
                    if (valid) {
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
            }

            StateDelta.setTo(StateVector);
            workMatrix.solve(ResidualVector, StateVector);
            var valid = !MatrixFeatures_DDRM.hasUncountable(StateVector);
            if (valid) {
                if(SCALING)
                    CommonOps_DDRM.multRows(columnScales, StateVector);
                CommonOps_DDRM.subtract(StateVector, StateDelta, StateDelta);
                // Perform solution fitting
                double alpha = 0;
                workMatrix = Jacobian;
                while(alpha < maxSearchAlpha) {
                    iterHooks(i, maxIterations);
                    computeResidual();
                    workMatrix.mult(StateVector, ErrorVector);
                    CommonOps_DDRM.subtract(ErrorVector, ResidualVector, ErrorVector);
                    double testNorm = CommonOps_DDRM.elementMaxAbs(ErrorVector);
                    if(testNorm < norm)
                        break;
                    double deltaAlpha = (1 - alpha) * 0.5;
                    alpha += deltaAlpha;
                    CommonOps_DDRM.add(StateVector, -deltaAlpha, StateDelta, StateVector);
                }
            } else {
                StateVector.zero();
                StateDelta.zero();
            }
        }
        verifyConvergence(norm, i, maxIterations);
        PERF.end();
    }

    @Override
    public void zeroRHS() {
        if(RHSVector != null)
            RHSVector.zero();
    }

    @Override
    public void zeroState() {
        converged = true;
        if(StateVector != null)
            StateVector.zero();
    }

    @Override
    public void jacobianPrepareForWrite() {
        enableRowExchange = false;
        Jacobian.denseZero();
    }

    @Override
    public void finishJacobianWrite() {
        recalculateScales = true;
        Jacobian.optimize();

        changedRows.clear();
        enableRowExchange = true;
        if(ROW_EXCHANGE && !SCALING) {
            A0.setTo(Jacobian);
        }
    }

    @Override
    public void rowExchange(boolean state) {
        enableRowExchange = state;
    }

    @Override
    public boolean rowExchange() {
        return enableRowExchange;
    }

    @Override
    public IMatrixAccess stateVector() {
        return stateAccess;
    }

    @Override
    public boolean isConverged() {
        return converged;
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
            if(solvedCoefficients.unsafe_get(0, index) == 0)
                return 2;
            // Apply rows up to this row (assumes that the collection has fixed ordering)
            var vec = solvedCoefficients.getData();
            for(var row : allRows) {
                if(row == this)
                    break;
                var x = vec[row.index] / row.solvedCoefficients.unsafe_get(0, row.index);
                if(!Double.isFinite(x))
                    return 2;
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

    private class StateAccess implements IMatrixAccess {
        @Override
        public void set(int row, int column, double value) {
            StateVector.data[row] = value;
        }

        @Override
        public double get(int row, int column) {
            return StateVector.data[row];
        }

        @Override
        public void add(int row, int column, double value) {
            StateVector.data[row] += value;
        }

        @Override
        public int numRows() {
            return StateVector.getNumRows();
        }

        @Override
        public int numCols() {
            return StateVector.getNumCols();
        }

        @Override
        public double safe_get(int row, int column) {
            if(StateVector == null)
                return 0;
            return IMatrixAccess.super.safe_get(row, column);
        }

        @Override
        public void safe_set(int row, int column, double value) {
            if(StateVector == null)
                return;
            IMatrixAccess.super.safe_set(row, column, value);
        }
    }

    private class ResidualAccess implements IMatrixAccess {
        @Override
        public void set(int row, int column, double value) {
            throw new IllegalCallerException("Cannot write to residual vector");
        }

        @Override
        public double get(int row, int column) {
            return ErrorVector.data[row];
        }

        @Override
        public int numRows() {
            return ErrorVector.getNumRows();
        }

        @Override
        public int numCols() {
            return ErrorVector.getNumCols();
        }
    }
}
