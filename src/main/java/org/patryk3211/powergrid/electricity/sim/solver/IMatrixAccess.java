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

public interface IMatrixAccess {
    void set(int row, int column, double value);
    double get(int row, int column);

    default void add(int row, int column, double value) {
        set(row, column, get(row, column) + value);
    }

    int numRows();
    int numCols();

    default double safe_get(int row, int column) {
        if(row >= numRows() || column >= numCols())
            return 0;
        return get(row, column);
    }

    default void safe_set(int row, int column, double value) {
        if(row >= numRows() || column >= numCols())
            return;
        set(row, column, value);
    }
}
