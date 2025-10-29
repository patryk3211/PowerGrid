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
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.PerformanceCounter;

public class DirectSolver implements ISolver {
    private static final PerformanceCounter PERF = new PerformanceCounter("DirectSolve");

    private DMatrixRMaj x;

    @Override
    public void setStateSize(int size) {
        if(x == null || x.getNumRows() != size) {
            x = new DMatrixRMaj(size, 1);
        }
    }

    @Override
    public @Nullable DMatrixRMaj solve(DynamicallyTypedMatrix A, DMatrixRMaj b, boolean acceptAll) {
        PERF.start();
        A.solve(b, x);
        PERF.end();
        return x;
    }

    @Override
    public DMatrixRMaj getLastGuess() {
        return x;
    }

    @Override
    public void zero() {
        if(x != null) {
            x.zero();
        }
    }

    @Override
    public void setTargetPrecision(double targetPrecision) {

    }
}
