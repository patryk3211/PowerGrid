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

import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.LOGGER;

public class NativeMNA implements IMNA {
    private static final int OPERATION_BUFFER_COMMAND_LENGTH = 128;

    public final ElectricalNetwork network;

    private final long nativePtr;

    private boolean hooksChanged = true;
    protected boolean converged;
    protected int warmUpTicks = 0;
    private int size;

    private final ByteBuffer rhsOps;
    private final ByteBuffer jacobianOps;

    private ByteBuffer stateBuffer;
    private final StateAccess stateAccess = new StateAccess();

    static {
        System.loadLibrary("powergridNative");
    }

    public NativeMNA(ElectricalNetwork network) {
        this.network = network;
        // RHS operation buffer (int + double -> row + change)
        rhsOps = ByteBuffer.allocateDirect((4 + 8) * OPERATION_BUFFER_COMMAND_LENGTH);
        rhsOps.order(ByteOrder.LITTLE_ENDIAN);
        // Jacobian operation buffer = (int + int + double -> row + column + change)
        jacobianOps = ByteBuffer.allocateDirect((4 + 4 + 8) * OPERATION_BUFFER_COMMAND_LENGTH);
        jacobianOps.order(ByteOrder.LITTLE_ENDIAN);
        nativePtr = allocateNativeObject(rhsOps, jacobianOps, OPERATION_BUFFER_COMMAND_LENGTH, this);
    }

    @Override
    public void cleanup() {
        deallocateNativeObject(nativePtr);
    }

    @Override
    public void setPrecision(double absoluteCriterion, double relativeCriterion, double minimumPrecision) {
        setPrecision(nativePtr, absoluteCriterion, relativeCriterion, minimumPrecision);
    }

    @Override
    public void hooksChanged() {
        hooksChanged = true;
    }

    @Override
    public void warmUp(int ticks) {
        if(ticks == -1 || warmUpTicks == -1) {
            warmUpTicks = -1;
            return;
        }
        converged = false;
        if(warmUpTicks < ticks)
            warmUpTicks = ticks;
    }

    @Override
    public void allocate(int size) {
        this.size = size;
        setStateSize(nativePtr, size);
    }

    @Override
    public void jacobianAdd(int row, int column, double value) {
        jacobianOps.putInt(row);
        jacobianOps.putInt(column);
        jacobianOps.putDouble(value);

        if(jacobianOps.position() == jacobianOps.capacity()) {
            // Ran out of space, send buffer to native for processing.
            processJacobianBuffer(nativePtr);
            jacobianOps.position(0);
        }
    }

    @Override
    public void rhsAdd(int row, double value) {
        rhsOps.putInt(row);
        rhsOps.putDouble(value);

        if(rhsOps.position() == rhsOps.capacity()) {
            // Ran out of space, send buffer to native for processing.
            processRHSBuffer(nativePtr);
            rhsOps.position(0);
        }
    }

    @Override
    public IMatrixAccess stateVector() {
        return stateAccess;
    }

    private void runIterHooks(ByteBuffer state) {
        // Run hooks
        this.stateBuffer = state;
        this.stateBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for(var hook : network.innerHooks) {
            hook.startIteration();
        }

        if(jacobianOps.position() != 0) {
            // Finalize buffer and send it.
            jacobianOps.putInt(-1);
            processJacobianBuffer(nativePtr);
        }

        // Rewind modification buffer.
        jacobianOps.position(0);
    }

    private void runAddResidual(ByteBuffer residualBuffer) {
        // Run hooks
        residualBuffer.order(ByteOrder.LITTLE_ENDIAN);
        for(var hook : network.innerHooks) {
            hook.addResidual((row, change) -> {
                var offset = row * 8;
                double current = residualBuffer.getDouble(offset);
                residualBuffer.putDouble(offset, current - change);
            });
        }
    }

    private void reportConvergenceProblems(double norm, int i, ByteBuffer residualBuffer) {
        if (LOGGER != null) {
            if (ModdedConfigs.logsEnabled()) {
                LOGGER.warn("Convergence problems after {} solver iterations, final norm: {}", i, norm);
            }
        } else {
            System.out.printf("Convergence problems after %d solver iterations, final norm: %g\n", i, norm);
        }
        network.convergenceProblems(norm, new IMatrixAccess() {
            @Override
            public void set(int row, int column, double value) {
                throw new IllegalCallerException("Cannot modify residual");
            }

            @Override
            public double get(int row, int column) {
                return residualBuffer.getDouble(row * 8);
            }

            @Override
            public int numRows() {
                return size;
            }

            @Override
            public int numCols() {
                return 1;
            }
        });
    }

    @Override
    public void singleTick() {
        // Finalize modification buffers.
        rhsOps.putInt(-1);
        jacobianOps.putInt(-1);

        // Rewind modification buffers.
        rhsOps.position(0);
        jacobianOps.position(0);

        if(hooksChanged) {
            // Negotiate native accelerated hook implementations.
            hooksChanged = false;
        }

        // Run native solve routine.
        int maxIterations = network.maxIterations.apply(network.hasHooks());
        var buffer = singleTick(nativePtr, maxIterations, this);
        if(buffer != null) {
            stateBuffer = buffer;
            stateBuffer.order(ByteOrder.LITTLE_ENDIAN);
            converged = true;
        } else {
            converged = false;
        }

        if(converged && warmUpTicks > 0) {
            --warmUpTicks;
            converged = false;
        }
    }

    @Override
    public void zeroRHS() {
        zeroRHS(nativePtr);
    }

    @Override
    public void zeroState() {
        zeroState(nativePtr);
    }

    @Override
    public void jacobianPrepareForWrite() {
        zeroJacobian(nativePtr);
    }

    @Override
    public void finishJacobianWrite() {
        finishJacobianWrite(nativePtr);
    }

    @Override
    public void rowExchange(boolean state) {

    }

    @Override
    public boolean rowExchange() {
        return false;
    }

    @Override
    public boolean isConverged() {
        return converged;
    }

    private class StateAccess implements IMatrixAccess {
        @Override
        public void set(int row, int column, double value) {
            stateBuffer.putDouble(row * 8, value);
        }

        @Override
        public double get(int row, int column) {
            // TODO: This seems slow for some reason?
            return stateBuffer.getDouble(row * 8);
        }

        @Override
        public double safe_get(int row, int column) {
            if(stateBuffer == null)
                return 0;
            return IMatrixAccess.super.safe_get(row, column);
        }

        @Override
        public void safe_set(int row, int column, double value) {
            if(stateBuffer == null)
                return;
            IMatrixAccess.super.safe_set(row, column, value);
        }

        @Override
        public int numRows() {
            return size;
        }

        @Override
        public int numCols() {
            return 1;
        }
    }

    private static native long allocateNativeObject(Buffer rhsOpBuffer, Buffer jOpBuffer, int cmdCount, NativeMNA javaObj);
    private static native void setStateSize(long ptr, int size);
    private static native void deallocateNativeObject(long ptr);
    private static native void zeroRHS(long ptr);
    private static native void zeroState(long ptr);
    private static native void zeroJacobian(long ptr);
    private static native void finishJacobianWrite(long ptr);
    private static native void processJacobianBuffer(long ptr);
    private static native void processRHSBuffer(long ptr);
    private static native void setPrecision(long ptr, double absoluteCriterion, double relativeCriterion, double minimumPrecision);
    private native ByteBuffer singleTick(long ptr, int maxIters, NativeMNA javaObj);
}
