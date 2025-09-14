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
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.G_MIN;

public class DynamicallyTypedMatrix {
    // This will need to be tuned, probably.
    private static final int SPARSE_THRESHOLD = 6;

    private DMatrix matrix;
    private boolean sparse;

    public DynamicallyTypedMatrix(int rows, int cols) {
        matrix = new DMatrixRMaj(rows, cols);
        sparse = false;
    }

    public void denseZero() {
        if(sparse) {
            matrix = new DMatrixRMaj(matrix.getNumRows(), matrix.getNumCols());
            sparse = false;
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
                DConvertMatrixStruct.convert(matrix, (DMatrixSparseCSC) this.matrix, G_MIN);
            } else {
                this.matrix = DConvertMatrixStruct.convert(matrix, (DMatrixSparseCSC) null, G_MIN);
            }
            sparse = true;
        } else {
            if(!sparse) {
                this.matrix.setTo(matrix);
            } else {
                this.matrix = new DMatrixRMaj(matrix);
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
                this.matrix = DConvertMatrixStruct.convert((DMatrixRMaj) matrix, (DMatrixSparseCSC) null, G_MIN);
            }
            sparse = true;
        } else {
            if (sparse) {
                this.matrix = new DMatrixRMaj(matrix);
            }
            sparse = false;
        }
    }
}
