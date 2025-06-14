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

import java.util.Random;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.LOGGER;

/*
 * Biconjugate Gradient Stabilized method
 * algorithm implemented according to https://en.wikipedia.org/wiki/Biconjugate_gradient_stabilized_method
 */
public class BiCGSTABSolver implements ISolver {
    private static final boolean USE_RANDOM_HAT_RESIDUAL = true;
    private static final int MAX_ITERATIONS = 200;

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

    private final double targetPrecision;

    public BiCGSTABSolver(double targetPrecision) {
        this.targetPrecision = targetPrecision;
        this.random = new Random();
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
        }
    }

    @Override
    public DMatrixRMaj solve(DMatrixRMaj A, DMatrixRMaj b) {
        if(b.getNumRows() == 0)
            return guess;
        boolean zeroResult = true;
        for(int i = 0; i < b.getNumRows(); ++i) {
            if(b.get(i, 0) != 0) {
                zeroResult = false;
                break;
            }
        }
        if(zeroResult) {
            zero();
            return guess;
        }

        // r = b - A * x
        CommonOps_DDRM.mult(A, guess, v);
        CommonOps_DDRM.subtract(b, v, residual);

        if(USE_RANDOM_HAT_RESIDUAL) {
            RandomMatrices_DDRM.fillUniform(hatResidual, random);
        } else {
            hatResidual.setTo(residual);
        }
        double dot = CommonOps_DDRM.dot(hatResidual, residual);
        if(USE_RANDOM_HAT_RESIDUAL) {
            if(dot == 0)
                hatResidual.setTo(residual);
        }
        p.setTo(residual);

        int iters = 0;
        double norm = 0;
        while(iters++ < MAX_ITERATIONS) {
            // v = A * p
            CommonOps_DDRM.mult(A, p, v);

            double alpha = dot / CommonOps_DDRM.dot(hatResidual, v);
            // h = x + alpha * p
            CommonOps_DDRM.add(guess, alpha, p, h);
            // s = r - alpha * v
            CommonOps_DDRM.add(residual, -alpha, v, s);

            norm = NormOps_DDRM.normP2(s);
            if(norm <= targetPrecision) {
                guess.setTo(h);
                break;
            }

            // t = A * s
            CommonOps_DDRM.mult(A, s, t);
            double omega = CommonOps_DDRM.dot(t, s) / CommonOps_DDRM.dot(t, t);

            // x = h + omega * s
            CommonOps_DDRM.add(h, omega, s, guess);
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

        if(iters >= MAX_ITERATIONS && LOGGER != null) {
            LOGGER.warn("Solver iteration limit, final precision: {}", norm);
        }

        return guess;
    }
}
