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

import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.interfaces.linsol.LinearSolverSparse;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.sparse.FillReducing;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public class DynamicallyTypedMatrix {
    // This will need to be tuned, probably.
    private static final int SPARSE_THRESHOLD = 6;
    private static final double G_THRESHOLD = G_MIN / 1000;

    private LinearSolver<? extends DMatrix, DMatrixRMaj> solver;
    private DMatrix matrix;
    private DMatrix resultMatrix;
    private boolean sparse;
    private boolean solverValid;
    private Solver solverType;

    public DynamicallyTypedMatrix(int rows, int cols) {
        this(rows, cols, Solver.CHOLESKY);
    }

    public DynamicallyTypedMatrix(int rows, int cols, Solver solverType) {
        matrix = new DMatrixRMaj(rows, cols);
        sparse = false;
        resultMatrix = null;
        solver = null;
        this.solverType = solverType;
    }

    public void denseZero() {
        if(sparse) {
            matrix = new DMatrixRMaj(matrix.getNumRows(), matrix.getNumCols());
            sparse = false;
            resultMatrix = null;
            solver = null;
        } else {
            matrix.zero();
        }
    }

    public void mult(DMatrixRMaj in, DMatrixRMaj out) {
        if(sparse) {
            CommonOps_DSCC.mult((DMatrixSparseCSC) matrix, in, out);
        } else {
            CommonOps_DDRM.mult((DMatrixRMaj) matrix, in, out);
        }
    }

    public void mult(DynamicallyTypedMatrix in, DynamicallyTypedMatrix out) {
        if((sparse && in.sparse != out.sparse) || (!sparse && (in.sparse || out.sparse)))
            throw new IllegalStateException("Sparsity of matrices doesn't match");
        if(sparse) {
            if(in.sparse) {
                CommonOps_DSCC.mult((DMatrixSparseCSC) matrix, (DMatrixSparseCSC) in.matrix, (DMatrixSparseCSC) out.matrix);
            } else {
                CommonOps_DSCC.mult((DMatrixSparseCSC) matrix, (DMatrixRMaj) in.matrix, (DMatrixRMaj) out.matrix);
            }
        } else {
            CommonOps_DDRM.mult((DMatrixRMaj) matrix, (DMatrixRMaj) in.matrix, (DMatrixRMaj) out.matrix);
        }
    }

    public void set(int row, int col, double value) {
        matrix.set(row, col, value);
    }

    public double get(int row, int col) {
        return matrix.get(row, col);
    }

    public double unsafe_get(int row, int col) {
        return matrix.unsafe_get(row, col);
    }

    public void add(int row, int col, double value) {
        if(!sparse) {
            ((DMatrixRMaj) matrix).add(row, col, value);
        } else {
            var current = matrix.get(row, col);
            matrix.unsafe_set(row, col, current + value);
        }
    }

    public void setTo(DMatrixRMaj matrix) {
        if(matrix.getNumRows() > SPARSE_THRESHOLD) {
            if(sparse) {
                DConvertMatrixStruct.convert(matrix, (DMatrixSparseCSC) this.matrix, G_THRESHOLD);
            } else {
                this.matrix = DConvertMatrixStruct.convert(matrix, (DMatrixSparseCSC) null, G_THRESHOLD);
                resultMatrix = null;
                solver = null;
            }
            sparse = true;
        } else {
            if(!sparse) {
                this.matrix.setTo(matrix);
            } else {
                this.matrix = new DMatrixRMaj(matrix);
                resultMatrix = null;
                solver = null;
            }
            sparse = false;
        }
    }

    public int getNumRows() {
        return matrix.getNumRows();
    }

    public void optimize() {
        if(matrix.getNumRows() > SPARSE_THRESHOLD || matrix.getNumCols() > SPARSE_THRESHOLD) {
            convert(State.SPARSE);
        } else {
            convert(State.DENSE);
        }
    }

    public void convert(State to) {
        if(!sparse && to == State.SPARSE) {
            this.matrix = DConvertMatrixStruct.convert((DMatrixRMaj) matrix, (DMatrixSparseCSC) null, G_THRESHOLD);
            resultMatrix = null;
            solver = null;
            sparse = true;
        } else if(sparse && to == State.DENSE) {
            this.matrix = new DMatrixRMaj(matrix);
            resultMatrix = null;
            solver = null;
            sparse = false;
        }
    }

    private DMatrixSparseCSC prepareSparseA(boolean modified) {
        var a = (DMatrixSparseCSC) matrix;
        if(modified) {
            if(resultMatrix == null) {
                a = new DMatrixSparseCSC(a);
                resultMatrix = a;
            } else {
                resultMatrix.setTo(a);
                a = (DMatrixSparseCSC) resultMatrix;
            }
        }
        return a;
    }

    private DMatrixRMaj prepareDenseA(boolean modified) {
        var a = (DMatrixRMaj) matrix;
        if(modified) {
            if(resultMatrix == null) {
                a = new DMatrixRMaj(a);
                resultMatrix = a;
            } else {
                resultMatrix.setTo(a);
                a = (DMatrixRMaj) resultMatrix;
            }
        }
        return a;
    }


    private void makeSolver() {
        if(sparse) {
            var solver = solverType.sparseFactory.apply(FillReducing.NONE);
            solverValid = solver.setA(prepareSparseA(solver.modifiesA()));
            this.solver = solver;
        } else {
            var solver = solverType.denseFactory.apply(getNumRows());
            solverValid = solver.setA(prepareDenseA(solver.modifiesA()));
            this.solver = solver;
        }
    }

    @SuppressWarnings("unchecked")
    public void refactorize() {
        if(solver != null) {
            if (sparse) {
                solverValid = ((LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj>) (Object) solver).setA(prepareSparseA(solver.modifiesA()));
            } else {
                solverValid = ((LinearSolverDense<DMatrixRMaj>) (Object) solver).setA(prepareDenseA(solver.modifiesA()));
            }
        } else {
            makeSolver();
        }
        if(!solverValid && solverType == Solver.CHOLESKY) {
            // Switch to LU since matrix might not be SPD
            solverType = Solver.LU;
            makeSolver();
        }
    }

    public void solve(DMatrixRMaj b, DMatrixRMaj x) {
        if(solver == null)
            refactorize();
        if(!solverValid) {
            x.zero();
            return;
        }
        solver.solve(b, x);
    }

    @SuppressWarnings("unchecked")
    public void solve(DynamicallyTypedMatrix b, DynamicallyTypedMatrix x) {
        if((sparse && b.sparse != x.sparse) || (!sparse && (b.sparse || x.sparse)))
            throw new IllegalStateException("Sparsity of matrices doesn't match");
        if(solver == null)
            refactorize();
        if(!solverValid) {
            x.matrix.zero();
            return;
        }
        if(sparse && b.sparse) {
            var solver = ((LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj>) (Object) this.solver);
            solver.solveSparse((DMatrixSparseCSC) b.matrix, (DMatrixSparseCSC) x.matrix);
        } else {
            solver.solve((DMatrixRMaj) b.matrix, (DMatrixRMaj) x.matrix);
        }
    }

    public void reshapeTo(DynamicallyTypedMatrix target) {
        var n = target.getNumRows();
        if(target.sparse == sparse && n == getNumRows()) {
            // Correct shape
            return;
        }
        if(target.sparse) {
            matrix = new DMatrixSparseCSC(n, n);
        } else {
            matrix = new DMatrixRMaj(n, n);
        }
        sparse = target.sparse;
        solver = null;
        resultMatrix = null;
    }

    public void setTo(DynamicallyTypedMatrix target) {
        reshapeTo(target);
        matrix.setTo(target.matrix);
    }

    public void multColumns(double[] values, @Nullable DynamicallyTypedMatrix output) {
        if(output != null) {
            output.setTo(this);
        } else {
            output = this;
        }
        if(sparse) {
            CommonOps_DSCC.multColumns((DMatrixSparseCSC) output.matrix, values, 0);
        } else {
            CommonOps_DDRM.multCols((DMatrixRMaj) output.matrix, values);
        }
    }

    public void multRows(double[] values, @Nullable DynamicallyTypedMatrix output) {
        if(output != null) {
            output.setTo(this);
        } else {
            output = this;
        }
        if(sparse) {
            CommonOps_DSCC.multRows(values, 0, (DMatrixSparseCSC) output.matrix);
        } else {
            CommonOps_DDRM.multRows(values, (DMatrixRMaj) output.matrix);
        }
    }

    public void subtract(DynamicallyTypedMatrix in, DynamicallyTypedMatrix out) {
        if(out.sparse != sparse)
            out.reshapeTo(this);
        if(in.sparse != sparse || out.sparse != sparse)
            throw new IllegalStateException("Sparsity of matrices doesn't match");
        if(sparse) {
            CommonOps_DSCC.add(1.0, (DMatrixSparseCSC) matrix, -1.0, (DMatrixSparseCSC) in.matrix, (DMatrixSparseCSC) out.matrix, null, null);
        } else {
            CommonOps_DDRM.subtract((DMatrixRMaj) matrix, (DMatrixRMaj) in.matrix, (DMatrixRMaj) out.matrix);
        }
    }

    public DMatrixRMaj getDense() {
        if(sparse)
            throw new IllegalStateException("Matrix is currently sparse");
        return (DMatrixRMaj) matrix;
    }

    public State getState() {
        return sparse ? State.SPARSE : State.DENSE;
    }

    public enum State {
        DENSE, SPARSE
    }

    public enum Solver {
        LU(LinearSolverFactory_DSCC::lu, LinearSolverFactory_DDRM::lu),
        CHOLESKY(LinearSolverFactory_DSCC::cholesky, LinearSolverFactory_DDRM::chol);

        public final Function<FillReducing, LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj>> sparseFactory;
        public final Function<Integer, LinearSolverDense<DMatrixRMaj>> denseFactory;

        Solver(Function<FillReducing, LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj>> sparseFactory, Function<Integer, LinearSolverDense<DMatrixRMaj>> denseFactory) {
            this.sparseFactory = sparseFactory;
            this.denseFactory = denseFactory;
        }
    }
}
