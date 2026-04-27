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

import org.patryk3211.powergrid.config.CSolver;

public interface IMNA {
    CSolver.SolverBackend type();

    void cleanup();

    void setPrecision(double absoluteCriterion, double relativeCriterion, double minimumPrecision, double searchAlpha);
    void warmUp(int ticks);

    void jacobianAdd(int row, int column, double value);
    void rhsAdd(int row, double value);

    void allocate(int size);
    void singleTick();

    void hooksChanged();

    void zeroRHS();
    void zeroState();
    void jacobianPrepareForWrite();
    void finishJacobianWrite();
    void rowExchange(boolean state);
    boolean rowExchange();
    IMatrixAccess stateVector();
    boolean isConverged();
}
