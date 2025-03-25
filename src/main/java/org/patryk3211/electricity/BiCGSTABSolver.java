/**
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
 **/
package org.patryk3211.electricity;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;

public class BiCGSTABSolver implements ISolver {
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

    private double targetDistance;

    public BiCGSTABSolver(double targetDistance) {
        this.targetDistance = targetDistance;
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
    public DMatrixRMaj solve(DMatrixRMaj A, DMatrixRMaj b) {
        // r = b - A * x
        CommonOps_DDRM.mult(A, guess, v);
        CommonOps_DDRM.subtract(b, v, residual);

        hatResidual.setTo(residual);
        double dot = CommonOps_DDRM.dot(hatResidual, residual);
        p.setTo(residual);

        while(true) {
            // v = A * p
            CommonOps_DDRM.mult(A, p, v);
            double alpha = dot / CommonOps_DDRM.dot(hatResidual, v);
            // h = x + alpha * p
            CommonOps_DDRM.add(guess, alpha, p, h);
            // s = r - alpha * v
            CommonOps_DDRM.add(residual, -alpha, v, s);

            double norm = NormOps_DDRM.normP1(s);
            if(norm <= targetDistance) {
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

            norm = NormOps_DDRM.normP1(residual);
            if(norm <= targetDistance) {
                break;
            }

            double dotPrev = dot;
            dot = CommonOps_DDRM.dot(hatResidual, residual);
            double beta = (dot / dotPrev) * (alpha / omega);
            // p = r + β(p − ωv)
            CommonOps_DDRM.add(p, -omega, v, t);
            CommonOps_DDRM.add(residual, beta, t, p);
        }

        return guess;
    }
}
