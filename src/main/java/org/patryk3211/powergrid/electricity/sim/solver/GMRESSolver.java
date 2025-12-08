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
import org.ejml.dense.row.NormOps_DDRM;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.PerformanceCounter;

public class GMRESSolver implements ISolver {
    private static final PerformanceCounter PERF = new PerformanceCounter("GMRES");

    private double targetPrecision;
    private final int m;

    private DMatrixRMaj guess;
    private DMatrixRMaj residual;
    private DMatrixRMaj q;
    private DMatrixRMaj w1;

    private DMatrixRMaj Q;
    private DMatrixRMaj H;
    private double[] sinm;
    private double[] cosm;
    private double[] g;
    private double[] y;

    public GMRESSolver(double targetPrecision, int m) {
        this.targetPrecision = targetPrecision;
        this.m = m;
    }

    @Override
    public ElectricalNetwork.SolverType type() {
        return ElectricalNetwork.SolverType.GMRES;
    }

    @Override
    public void setTargetPrecision(double targetPrecision) {
        this.targetPrecision = targetPrecision;
    }

    @Override
    public void setStateSize(int newSize) {
        if(guess == null || guess.getNumRows() != newSize) {
            guess = new DMatrixRMaj(newSize, 1);
            residual = new DMatrixRMaj(newSize, 1);
            q = new DMatrixRMaj(newSize, 1);
            w1 = new DMatrixRMaj(newSize, 1);

            Q = new DMatrixRMaj(newSize, m + 1);
            H = new DMatrixRMaj(m, m);

            sinm = new double[m];
            cosm = new double[m];
            g = new double[m + 1];
            y = new double[m];
        }
    }

    @Override
    public void zero() {
        if(guess != null) {
            guess.zero();
        }
    }

    private void Qn(int n) {
        for(int i = 0; i < q.getNumRows(); ++i) {
            Q.unsafe_set(i, n, q.unsafe_get(i, 0));
        }
    }

    public boolean gmres(DynamicallyTypedMatrix A, DMatrixRMaj b) {
        A.mult(guess, residual);
        CommonOps_DDRM.subtract(b, residual, residual);
        // Diagonal preconditioning
        for(int i = 0; i < A.getNumRows(); ++i) {
            var x = residual.unsafe_get(i, 0);
            var k = A.unsafe_get(i, i);
            residual.unsafe_set(i, 0, k == 0 ? x : x / k);
        }
        var beta = NormOps_DDRM.normP2(residual);
        if(beta < targetPrecision)
            return false;

        q.zero();
        w1.zero();
        Q.zero();
        H.zero();

        final double[] w1Dat = w1.getData();

        g[0] = beta;
        CommonOps_DDRM.scale(1 / beta, residual, q);
        Qn(0);
        int i;
        for(i = 0; i < m; ++i) {
            // Arnoldi
            A.mult(q, w1);
            for(int j = 0; j < A.getNumRows(); ++j) {
                var x = w1Dat[j];
                var k = A.unsafe_get(j, j);
                w1Dat[j] = k == 0 ? x : x / k;
            }

            for(int j = 0; j <= i; ++j) {
                // H(j, i) = q^T * Q(:, j)
                double sum = 0;
                for(int k = 0; k < w1Dat.length; ++k) {
                    sum += w1Dat[k] * Q.unsafe_get(k, j);
                }
                H.unsafe_set(j, i, sum);
                // q = q - H(j, i) * Q(:, j)
                for(int k = 0; k < w1Dat.length; ++k) {
                    w1Dat[k] -= sum * Q.unsafe_get(k, j);
                }
            }
            double H1 = 0;
            var qNorm = NormOps_DDRM.normP2(w1);
            if(qNorm > 0) {
                H1 = qNorm;
                CommonOps_DDRM.scale(1 / qNorm, w1, q);
                Qn(i + 1);
            }

            // Givens rotation
            for(int j = 0; j <= i - 1; ++j) {
                double h1 = H.unsafe_get(j, i), h2 = H.unsafe_get(j + 1, i);
                H.unsafe_set(j, i, cosm[j] * h1 + sinm[j] * h2);
                H.unsafe_set(j + 1, i, -sinm[j] * h1 + cosm[j] * h2);
            }
            // Next Givens rotation
            double hi = H.unsafe_get(i, i);
            var t = Math.sqrt(hi * hi + H1 * H1);
            cosm[i] = hi / t;
            sinm[i] = H1 / t;
            // Eliminate H(i + 1, i)
            H.unsafe_set(i, i, cosm[i] * hi + sinm[i] * H1);

            // Extend g
            g[i + 1] = -sinm[i] * g[i];
            g[i] *= cosm[i];

            // Compute norm
            var norm = Math.abs(g[i + 1]);
            if(norm <= targetPrecision) {
                ++i;
                break;
            }
        }
        // Calculate result (solve upper triangular system)
        for(int j = i - 1; j >= 0; --j) {
            double v = g[j];
            for(int k = j + 1; k < i; ++k) {
                v -= y[k] * H.unsafe_get(j, k);
            }
            y[j] = v / H.unsafe_get(j, j);
        }
        var x = guess.getData();
        for(int k = 0; k < b.getNumRows(); ++k) {
            for (int j = 0; j < i; ++j) {
                x[k] += Q.unsafe_get(k, j) * y[j];
            }
        }
        return true;
    }

    @Override
    public @Nullable DMatrixRMaj solve(DynamicallyTypedMatrix A, DMatrixRMaj b, boolean acceptAll) {
        PERF.start();
        for(int i = 0; i < 10; ++i) {
            if(!gmres(A, b))
                break;
        }
        PERF.end();
        return guess;
    }

    @Override
    public DMatrixRMaj getLastGuess() {
        return guess;
    }
}
