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

public interface ISolverHook {
    /**
     * Called before iterative solving loop is started
     * @param A Admittance matrix
     * @param x Previous solution
     * @param b Current matrix
     */
    default void preSolve(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj b) { }

    /**
     * Called at the start of every iteration
     * @param A Admittance matrix
     * @param x Current best guess solution
     * @param residual Residual matrix
     * @param p Direction matrix
     */
    default void iteration(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj residual, DMatrixRMaj p) { }

    /**
     * Called after the initial residual has been calculated (r = b - A * x)
     * @param A Admittance matrix
     * @param x Previous solution
     * @param b Current matrix
     * @param residual Residual matrix
     */
    default void addResidual(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj b, DMatrixRMaj residual) { }
}
