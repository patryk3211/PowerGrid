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
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.PerformanceCounter;

import java.util.Random;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.LOGGER;

/*
 * Biconjugate Gradient Stabilized method
 * algorithm implemented according to https://en.wikipedia.org/wiki/Biconjugate_gradient_stabilized_method
 */
public class BiCGSTABSolver implements ISolver {
    private static final boolean USE_RANDOM_HAT_RESIDUAL = true;
    private static final boolean DIAGONAL_PRECONDITIONER = true;
    private static final int MAX_ITERATIONS = 300;

    private static final PerformanceCounter PERF = new PerformanceCounter("BiCGStab");

    private final Random random;

    // Solved vector
    private DMatrixRMaj guess;

    // Intermediate vectors used in the solver
    private DMatrixRMaj residual;
    private DMatrixRMaj hatResidual;
    private DMatrixRMaj p;
    private DMatrixRMaj v;
    private DMatrixRMaj h;
    private DMatrixRMaj s;
    private DMatrixRMaj t;

    private DMatrixRMaj y;
    private DMatrixRMaj z;

    private int solveCount = 0;
    private DMatrixRMaj L;
    private DMatrixRMaj U;
    private boolean shouldCalculateLU;

    private double targetPrecision;
    private final double maxAllowed;

    public BiCGSTABSolver(double targetPrecision, double maxAllowed) {
        this.targetPrecision = targetPrecision;
        this.maxAllowed = maxAllowed;
        this.random = new Random();
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
            hatResidual = new DMatrixRMaj(newSize, 1);
            p = new DMatrixRMaj(newSize, 1);
            v = new DMatrixRMaj(newSize, 1);
            h = new DMatrixRMaj(newSize, 1);
            s = new DMatrixRMaj(newSize, 1);
            t = new DMatrixRMaj(newSize, 1);

            y = new DMatrixRMaj(newSize, 1);
            z = new DMatrixRMaj(newSize, 1);

            shouldCalculateLU = true;
            L = new DMatrixRMaj(newSize, newSize);
            U = new DMatrixRMaj(newSize, newSize);
        }
    }

    @Override
    public void zero() {
        if(guess != null) {
            guess.zero();
            residual.zero();
            hatResidual.zero();
            p.zero();
            v.zero();
            h.zero();
            s.zero();
            t.zero();
            shouldCalculateLU = true;
        }
    }

    private void prepareILU(DynamicallyTypedMatrix A) {
        shouldCalculateLU = false;
        L.zero();
        U.zero();
        int n = A.getNumRows();
        for(int j = 0; j < n; ++j) {
            U.set(0, j, A.get(0, j));
        }
        for(int i = 1; i < n; ++i) {
            for(int k = 0; k < i - 1; ++k) {
                var Ukk = U.get(k, k);
                if(Ukk == 0)
                    continue;
                var Lik = A.get(i, k) / Ukk;
                L.set(i, k, Lik);
                for(int j = k + 1; j < i - 1; ++j) {
                    var Aij = A.get(i, j);
                    if(Aij == 0)
                        continue;
                    L.set(i, j, Aij - Lik * U.get(k, j));
                }
                for(int j = i; j < n; ++j) {
                    var Aij = A.get(i, j);
                    if(Aij == 0)
                        continue;
                    U.set(i, j, Aij - Lik * U.get(k, j));
                }
            }
        }
//        L.optimize();
//        U.optimize();
    }

    private void preconditioned(DynamicallyTypedMatrix K, DMatrixRMaj input, DMatrixRMaj output) {
        if(DIAGONAL_PRECONDITIONER) {
            for(int i = 0; i < input.getNumRows(); ++i) {
                var k = K.get(i, i);
                if(k == 0)
                    k = 1;
                var x = input.get(i, 0);
                output.set(i, 0, x / k);
            }
        } else {
            // Forward solve using L matrix, we assume that diagonal is identity so we don't have to divide by it.
            for (int i = 0; i < input.getNumRows(); ++i) {
                var b = input.get(i, 0);
                for (int j = 0; j < i; ++j) {
                    b -= L.get(i, j) * output.get(j);
                }
                output.set(i, 0, b);
            }
            // Back solve using U matrix.
            for (int i = input.getNumRows() - 1; i >= 0; --i) {
                var b = output.get(i, 0);
                for (int j = i + 1; j < input.getNumRows(); ++j) {
                    b -= U.get(i, j) * output.get(j);
                }
                if (U.get(i, i) != 0)
                    output.set(i, 0, b / U.get(i, i));
            }
        }
    }

    private double pickHatResidual() {
        if(USE_RANDOM_HAT_RESIDUAL) {
            RandomMatrices_DDRM.fillUniform(hatResidual, random);
        } else {
            hatResidual.setTo(residual);
            return 1;
        }
        double dot = CommonOps_DDRM.dot(hatResidual, residual);
        if(USE_RANDOM_HAT_RESIDUAL) {
            if(dot == 0) {
                hatResidual.setTo(residual);
                return 1;
            }
        }
        return dot;
    }

    @Override
    @Nullable
    public DMatrixRMaj solve(DynamicallyTypedMatrix A, DMatrixRMaj b, boolean acceptAll) {
        if(b.getNumRows() == 0)
            return guess;

        PERF.start();
        if(!DIAGONAL_PRECONDITIONER) {
            if (shouldCalculateLU || solveCount++ >= 20) {
                prepareILU(A);
                solveCount = 0;
            }
        }

        // r = b - A * x
        A.mult(guess, v);
        CommonOps_DDRM.subtract(b, v, residual);

        // Check if result is already good enough.
        double norm = NormOps_DDRM.normP2(residual);
        double initialDistance = norm;
        if(norm <= targetPrecision) {
            return guess;
        }

        double dot = pickHatResidual();
        p.setTo(residual);

        int iters = 0;
        while(iters++ < MAX_ITERATIONS) {
            preconditioned(A, p, y);

            // v = A * y
            A.mult(y, v);

            double alpha = dot / CommonOps_DDRM.dot(hatResidual, v);
            // h = x + alpha * y
            CommonOps_DDRM.add(guess, alpha, y, h);
            // s = r - alpha * v
            CommonOps_DDRM.add(residual, -alpha, v, s);

            norm = NormOps_DDRM.normP2(s);
            if(norm <= targetPrecision) {
                guess.setTo(h);
                break;
            }

            preconditioned(A, s, z);

            // t = A * z
            A.mult(z, t);
            double omega = CommonOps_DDRM.dot(t, s) / CommonOps_DDRM.dot(t, t);

            // x = h + omega * z
            CommonOps_DDRM.add(h, omega, z, guess);
            // r = s - omega * t
            CommonOps_DDRM.add(s, -omega, t, residual);

            norm = NormOps_DDRM.normP2(residual);
            if(norm <= targetPrecision) {
                break;
            }

            double dotPrev = dot;
            dot = CommonOps_DDRM.dot(hatResidual, residual);
            double beta = (dot / dotPrev) * (alpha / omega);
            // p = r + β(p − ωv)
            CommonOps_DDRM.add(p, -omega, v, t);
            CommonOps_DDRM.add(residual, beta, t, p);
        }

        PERF.end();
        if(iters >= MAX_ITERATIONS) {
            var prefix = acceptAll ? "(AcceptAll) " : "";
            if (LOGGER != null) {
                if(ModdedConfigs.logsEnabled()) {
                    LOGGER.warn("{}Solver iteration limit, final precision: {}", prefix, norm);
                }
            } else {
                System.out.printf("%sSolver iteration limit, final precision: %g\n", prefix, norm);
            }

            if(!acceptAll) {
                if (norm > maxAllowed && (initialDistance / norm) < 10) {
                    if(LOGGER != null && ModdedConfigs.logsEnabled()) {
                        LOGGER.warn("Large imprecision, dropping iterative result");
                    }
                    A.solve(b, guess);
                    return guess;
                }
            } else {
                // Drop if guess is less precise then the previous guess.
                if(initialDistance / norm < 1) {
                    if (LOGGER != null && ModdedConfigs.logsEnabled()) {
                        LOGGER.info("(AcceptAll) Large imprecision, dropping iterative result");
                    }
                    A.solve(b, guess);
                    return guess;
                }
            }
        }

        return guess;
    }

    @Override
    public DMatrixRMaj getLastGuess() {
        return guess;
    }
}
