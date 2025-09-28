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

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public class DynamicallyTypedMatrix {
    // This will need to be tuned, probably.
    private static final int SPARSE_THRESHOLD = 6;
    private static final double G_THRESHOLD = G_MIN / 1000;

    private LinearSolver<? extends DMatrix, DMatrixRMaj> solver;
    private DMatrix matrix;
    private boolean sparse;

    public DynamicallyTypedMatrix(int rows, int cols) {
        matrix = new DMatrixRMaj(rows, cols);
        sparse = false;
        solver = null;
    }

    public void denseZero() {
        if(sparse) {
            matrix = new DMatrixRMaj(matrix.getNumRows(), matrix.getNumCols());
            sparse = false;
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

    public void set(int row, int col, double value) {
        matrix.set(row, col, value);
    }

    public double get(int row, int col) {
        return matrix.get(row, col);
    }

    public void add(int row, int col, double value) {
        if(!sparse) {
            ((DMatrixRMaj) matrix).add(row, col, value);
        } else {
            var current = matrix.get(row, col);
            matrix.set(row, col, current + value);
        }
    }

    public void setTo(DMatrixRMaj matrix) {
        if(matrix.getNumRows() > SPARSE_THRESHOLD) {
            if(sparse) {
                DConvertMatrixStruct.convert(matrix, (DMatrixSparseCSC) this.matrix, G_THRESHOLD);
            } else {
                this.matrix = DConvertMatrixStruct.convert(matrix, (DMatrixSparseCSC) null, G_THRESHOLD);
                solver = null;
            }
            sparse = true;
        } else {
            if(!sparse) {
                this.matrix.setTo(matrix);
            } else {
                this.matrix = new DMatrixRMaj(matrix);
                solver = null;
            }
            sparse = false;
        }
    }

    public int getNumRows() {
        return matrix.getNumRows();
    }

    public void optimize() {
        if(matrix.getNumRows() > SPARSE_THRESHOLD) {
            if (!sparse) {
                this.matrix = DConvertMatrixStruct.convert((DMatrixRMaj) matrix, (DMatrixSparseCSC) null, G_THRESHOLD);
                solver = null;
            }
            sparse = true;
        } else {
            if (sparse) {
                this.matrix = new DMatrixRMaj(matrix);
                solver = null;
            }
            sparse = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void solve(DMatrixRMaj b, DMatrixRMaj x) {
        if(solver == null) {
            if(sparse) {
                solver = LinearSolverFactory_DSCC.lu(FillReducing.NONE);
            } else {
                solver = LinearSolverFactory_DDRM.lu(getNumRows());
            }
        }
        boolean valid;
        if(sparse) {
            valid = ((LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj>) (Object) solver).setA((DMatrixSparseCSC) matrix);
        } else {
            valid = ((LinearSolverDense<DMatrixRMaj>) (Object) solver).setA((DMatrixRMaj) matrix);
        }
        if(valid)
            solver.solve(b, x);
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
}
