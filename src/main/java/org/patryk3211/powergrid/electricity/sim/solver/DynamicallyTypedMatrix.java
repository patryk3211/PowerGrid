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
import org.ejml.sparse.csc.CommonOps_DSCC;

public class DynamicallyTypedMatrix {
    private DMatrix matrix;
    private boolean sparse;

    public DynamicallyTypedMatrix(int rows, int cols) {
        matrix = new DMatrixRMaj(rows, cols);
        sparse = false;
    }

    public void mult(DMatrixRMaj in, DMatrixRMaj out) {
        if(sparse) {
            CommonOps_DSCC.mult((DMatrixSparseCSC) matrix, in, out);
        } else {
            CommonOps_DDRM.mult((DMatrixRMaj) matrix, in, out);
        }
    }
}
