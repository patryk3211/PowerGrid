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

import net.minecraft.Util;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.CSolver;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.patryk3211.powergrid.electricity.sim.ElectricalNetwork.LOGGER;

public class NativeMNA implements IMNA {
    private static final String EXPECTED_VERSION = "1";

    private static final int OPERATION_BUFFER_COMMAND_LENGTH = 256;
    private static final int JACOBIAN_COMMAND_SIZE = Integer.BYTES * 2 + Double.BYTES;
    private static final int RHS_COMMAND_SIZE = Integer.BYTES + Double.BYTES;
    private static boolean supported = false;

    private static final String MESSAGE_OK = "Native backend loaded successfully";
    private static final String MESSAGE_FAIL = "Native backend failed to load. Accelerated solver will not be available!";

    private static final Path NATIVES_DIR = Paths.get(".pg-native");

    public final ElectricalNetwork network;

    private final long nativePtr;

    private boolean hooksChanged = true;
    protected boolean converged;
    protected int warmUpTicks = 0;
    private int size;

    private final ByteBuffer auxBuf;
    private final ByteBuffer rhsOps;
    private final ByteBuffer jacobianOps;

    private ByteBuffer stateBuffer;
    private final StateAccess stateAccess = new StateAccess();

    public static boolean isSupported() {
        return supported;
    }

    public static void tryLoad() {
        try {
            // Try load from path first
            System.loadLibrary("powergridNative" + EXPECTED_VERSION);
            if (verifySupport() == 0) {
                PowerGrid.LOGGER.info(MESSAGE_OK);
                supported = true;
            } else {
                PowerGrid.LOGGER.error(MESSAGE_FAIL);
            }
        } catch (Throwable e) {
            try {
                Files.createDirectories(NATIVES_DIR);
                var libName = switch (Util.getPlatform()) {
                    case LINUX -> "libpowergridNative" + EXPECTED_VERSION + ".so";
                    case WINDOWS -> "libpowergridNative" + EXPECTED_VERSION + ".dll";
                    default -> throw new IllegalArgumentException("Unsupported platform!");
                };
                var libPath = NATIVES_DIR.resolve(libName);
                var loaded = true;
                if (!Files.exists(libPath)) {
                    // Try extracting the embedded files
                    var platformFolder = switch (Util.getPlatform()) {
                        case LINUX -> "linux";
                        case WINDOWS -> "windows";
                        default -> null;
                    };
                    if (platformFolder == null) {
                        // Platform not available in jar
                        PowerGrid.LOGGER.error(MESSAGE_FAIL, e);
                        return;
                    }
                    var loader = NativeMNA.class.getClassLoader();
                    var stream = loader.getResourceAsStream("native/" + platformFolder + "/" + libName);
                    if (stream != null) {
                        Files.copy(stream, libPath);
                        PowerGrid.LOGGER.info("Extracted native binary '{}' from jar", libName);
                    } else {
                        PowerGrid.LOGGER.error(MESSAGE_FAIL, e);
                        loaded = false;
                    }
                }
                if (loaded) {
                    System.load(libPath.toAbsolutePath().toString());
                    if (verifySupport() == 0) {
                        PowerGrid.LOGGER.info(MESSAGE_OK);
                        supported = true;
                    } else {
                        PowerGrid.LOGGER.error(MESSAGE_FAIL);
                    }
                }
            } catch (Throwable e2) {
                PowerGrid.LOGGER.error(MESSAGE_FAIL, e2);
            }
        }
    }

    private static void loadBinary() {

    }

    public NativeMNA(ElectricalNetwork network) {
        this.network = network;
        // Auxiliary status buffer.
        auxBuf = ByteBuffer.allocateDirect(1);
        // RHS operation buffer (int + double -> row + change)
        rhsOps = ByteBuffer.allocateDirect(RHS_COMMAND_SIZE * OPERATION_BUFFER_COMMAND_LENGTH);
        rhsOps.order(ByteOrder.nativeOrder());
        // Jacobian operation buffer = (int + int + double -> row + column + change)
        jacobianOps = ByteBuffer.allocateDirect(JACOBIAN_COMMAND_SIZE * OPERATION_BUFFER_COMMAND_LENGTH);
        jacobianOps.order(ByteOrder.nativeOrder());
        nativePtr = allocateNativeObject(rhsOps, jacobianOps, OPERATION_BUFFER_COMMAND_LENGTH, auxBuf);
    }

    @Override
    public CSolver.SolverBackend type() {
        return CSolver.SolverBackend.NATIVE;
    }

    @Override
    public void cleanup() {
        deallocateNativeObject(nativePtr);
        // State buffer is NOT valid after destructors of native solver have been called.
        stateBuffer = null;
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
        // We need to wait until the backend gives us a handle to the newly allocated buffer.
        stateBuffer = null;

        // Reset modification buffers
        jacobianOps.position(0);
        rhsOps.position(0);
    }

    @Override
    public void jacobianAdd(int row, int column, double value) {
        if(value == 0)
            return;
        jacobianOps.putInt(row);
        jacobianOps.putInt(column);
        jacobianOps.putDouble(value);

        if(jacobianOps.position() == jacobianOps.capacity()) {
            // Ran out of space, send buffer to native for processing.
            processJacobianBuffer(nativePtr, OPERATION_BUFFER_COMMAND_LENGTH);
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

    @SuppressWarnings("unused")
    private int runIterHooks(ByteBuffer state) {
        // Run hooks
        network.countUpdates = false;
        this.stateBuffer = state;
        this.stateBuffer.order(ByteOrder.nativeOrder());
        for(var hook : network.innerHooks) {
            hook.startIteration();
        }
        network.countUpdates = true;

        int pos = jacobianOps.position();
        // Rewind modification buffer.
        jacobianOps.position(0);
        return pos / JACOBIAN_COMMAND_SIZE;
    }

    @SuppressWarnings("unused")
    private void runAddResidual(ByteBuffer residualBuffer) {
        // Run hooks
        residualBuffer.order(ByteOrder.nativeOrder());
        for(var hook : network.innerHooks) {
            hook.addResidual((row, change) -> {
                var offset = row * Double.BYTES;
                double current = residualBuffer.getDouble(offset);
                residualBuffer.putDouble(offset, current - change);
            });
        }
    }

    @SuppressWarnings("unused")
    private void reportConvergenceProblems(double norm, int i, ByteBuffer residualBuffer) {
        if (LOGGER != null) {
            if (ModdedConfigs.logsEnabled()) {
                LOGGER.warn("Convergence problems after {} solver iterations, final norm: {}", i, norm);
            }
        } else {
            System.out.printf("Convergence problems after %d solver iterations, final norm: %g\n", i, norm);
        }
        residualBuffer.order(ByteOrder.nativeOrder());
        network.convergenceProblems(norm, new IMatrixAccess() {
            @Override
            public void set(int row, int column, double value) {
                throw new IllegalCallerException("Cannot modify residual");
            }

            @Override
            public double get(int row, int column) {
                return residualBuffer.getDouble(row * Double.BYTES);
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

        // Rewind modification buffers.
        rhsOps.position(0);
        int ops = jacobianOps.position() / JACOBIAN_COMMAND_SIZE;
        jacobianOps.position(0);

        if(hooksChanged) {
            // Negotiate native accelerated hook implementations.
            hooksChanged = false;
        }

        // Run native solve routine.
        int maxIterations = network.maxIterations.apply(network.hasHooks());
        stateBuffer = singleTick(nativePtr, maxIterations, ops);
        stateBuffer.order(ByteOrder.nativeOrder());
        byte status = auxBuf.get(0);
        converged = (status & 1) != 0;

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
        converged = true;
    }

    @Override
    public void jacobianPrepareForWrite() {
        jacobianOps.position(0);
        zeroJacobian(nativePtr);
    }

    @Override
    public void finishJacobianWrite() {
        finishJacobianWrite(nativePtr, jacobianOps.position() / JACOBIAN_COMMAND_SIZE);
        jacobianOps.position(0);
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
            stateBuffer.putDouble(row * Double.BYTES, value);
        }

        @Override
        public double get(int row, int column) {
            // TODO: This seems slow for some reason?
            return stateBuffer.getDouble(row * Double.BYTES);
        }

        @Override
        public double safe_get(int row, int column) {
            if(stateBuffer == null || row * Double.BYTES >= stateBuffer.capacity())
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

    private native long allocateNativeObject(Buffer rhsOpBuffer, Buffer jOpBuffer, int cmdCount, Buffer auxBuffer);
    private static native void setStateSize(long ptr, int size);
    private static native void deallocateNativeObject(long ptr);
    private static native void zeroRHS(long ptr);
    private static native void zeroState(long ptr);
    private static native void zeroJacobian(long ptr);
    private static native void finishJacobianWrite(long ptr, int cmdCount);
    private static native void processJacobianBuffer(long ptr, int cmdCount);
    private static native void processRHSBuffer(long ptr);
    private static native void setPrecision(long ptr, double absoluteCriterion, double relativeCriterion, double minimumPrecision);
    private native ByteBuffer singleTick(long ptr, int maxIters, int jacobianCmdCount);
    private static native int verifySupport();
}
