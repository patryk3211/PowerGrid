#include "solver.h"
#include "jni.h"
#include "sparse.h"
#include "util.h"
#include "blas.h"

void solver_init(solver_t *solver, void *rhsOpBuf, void *jacobianOpBuf, int cmdCount, void *auxBuf, JNIEnv *env, jobject mnaObj) {
    PG_TRACE("[Solver::Solver] entering");
    memset(solver, 0, sizeof(solver_t));
    solver->m_env = env;
    solver->m_mnaObject = mnaObj;
    solver->m_maxCmdCount = cmdCount;
    solver->m_rhsOpBuffer = (rhsop_t *) rhsOpBuf;
    solver->m_jacobianOpBuffer = (jacobianop_t *) jacobianOpBuf;
    solver->m_aux = (auxbuf_t *) auxBuf;

    sparsematrix_init(&solver->m_A);

    solver->m_X.ncol = 1;
    solver->m_X.Stype = SLU_DN;
    solver->m_X.Dtype = SLU_D;
    solver->m_X.Mtype = SLU_GE;
    solver->m_X.Store = &solver->m_Xstore;

    solver->m_B.ncol = 1;
    solver->m_B.Stype = SLU_DN;
    solver->m_B.Dtype = SLU_D;
    solver->m_B.Mtype = SLU_GE;
    solver->m_B.Store = &solver->m_Bstore;
    
    solver->m_minimumAllowedPrecision = 1e-6;
    solver->m_absoluteStoppingCriterion = 1e-7;
    solver->m_relativeStoppingCriterion = 1e-12;

    jclass clazz = (*env)->GetObjectClass(env, mnaObj);
    solver->m_iterHookMethod = (*env)->GetMethodID(env, clazz, "runIterHooks", "(ILjava/nio/ByteBuffer;)I");
    PG_ASSERT_(solver->m_iterHookMethod != 0, "runIterHooks method not found!");
    solver->m_residualAddMethod = (*env)->GetMethodID(env, clazz, "runAddResidual", "(Ljava/nio/ByteBuffer;)V");
    PG_ASSERT_(solver->m_residualAddMethod != 0, "runAddResidual method not found!");
    solver->m_reportProblemsMethod = (*env)->GetMethodID(env, clazz, "reportConvergenceProblems", "(DILjava/nio/ByteBuffer;)V");
    PG_ASSERT_(solver->m_reportProblemsMethod != 0, "reportConvergenceProblems method not found!");

    PG_TRACE("[Solver::Solver] returning");
}

static void solver_free_bufs(solver_t *solver) {
    free(solver->m_b);
    free(solver->m_rhs);
    free(solver->m_state);
    free(solver->m_residual);
    free(solver->m_stateDelta);
    solver->m_b = 0;
    solver->m_rhs = 0;
    solver->m_state = 0;
    solver->m_residual = 0;
    solver->m_stateDelta = 0;
}

void solver_destroy(solver_t *solver) {
    PG_TRACE("[Solver::~Solver] entering");
    sparsematrix_destroy(&solver->m_A);
    solver_free_bufs(solver);
    if(solver->m_stateBuffer != 0)
        (*solver->m_env)->DeleteGlobalRef(solver->m_env, solver->m_stateBuffer);
    if(solver->m_bBuffer != 0)
        (*solver->m_env)->DeleteGlobalRef(solver->m_env, solver->m_bBuffer);
    PG_TRACE("[Solver::~Solver] returning");
}

void solver_resize(solver_t *solver, int size) {
    if(solver->m_size == size)
        return;
    PG_TRACE("[Solver::resize] Resizing solver state to %d", size);
    solver_free_bufs(solver);
    solver->m_size = size;
    sparsematrix_resize(&solver->m_A, size);
    if(size == 0)
        return;
    
    solver->m_b = ALLOC(double, size);
    solver->m_rhs = ALLOC(double, size);
    solver->m_state = ALLOC(double, size);
    solver->m_residual = ALLOC(double, size);
    solver->m_stateDelta = ALLOC(double, size);
    memset(solver->m_rhs, 0, sizeof(double) * size);
    memset(solver->m_state, 0, sizeof(double) * size);

    PG_TRACE("[Solver::resize] Assigning new pointers");
    solver->m_Xstore.lda = solver->m_X.nrow = size;
    solver->m_Bstore.lda = solver->m_B.nrow = size;
    solver->m_Xstore.nzval = solver->m_state;
    solver->m_Bstore.nzval = solver->m_b;

    PG_TRACE("[Solver::resize] Deleting old jBuffers");
    if(solver->m_stateBuffer != 0)
        (*solver->m_env)->DeleteGlobalRef(solver->m_env, solver->m_stateBuffer);
    if(solver->m_bBuffer != 0)
        (*solver->m_env)->DeleteGlobalRef(solver->m_env, solver->m_bBuffer);

    PG_TRACE("[Solver::resize] Allocating new jBuffers");
    solver->m_stateBuffer = (*solver->m_env)->NewDirectByteBuffer(solver->m_env, solver->m_state, size * sizeof(double));
    solver->m_bBuffer = (*solver->m_env)->NewDirectByteBuffer(solver->m_env, solver->m_b, size * sizeof(double));

    solver->m_stateBuffer = (*solver->m_env)->NewGlobalRef(solver->m_env, solver->m_stateBuffer);
    solver->m_bBuffer = (*solver->m_env)->NewGlobalRef(solver->m_env, solver->m_bBuffer);

    PG_TRACE("[Solver::resize] returning");
}

void solver_zero_state(solver_t *solver) {
    if(solver->m_state == 0)
        return;
    memset(solver->m_state, 0, sizeof(double) * solver->m_size);
}

void solver_zero_rhs(solver_t *solver) {
    if(solver->m_rhs == 0)
        return;
    memset(solver->m_rhs, 0, sizeof(double) * solver->m_size);
}

void solver_zero_jacobian(solver_t *solver) {
    if(solver->m_size == 0)
        return;
    PG_TRACE("[Solver::zeroJacobian] zeroing");
    sparsematrix_zero(&solver->m_A);
}

static void solver_swap_buffers(solver_t *solver) {
    double *buf = solver->m_state;
    solver->m_state = solver->m_b;
    solver->m_b = buf;

    solver->m_Xstore.nzval = solver->m_state;
    solver->m_Bstore.nzval = solver->m_b;

    jobject jbuf = solver->m_stateBuffer;
    solver->m_stateBuffer = solver->m_bBuffer;
    solver->m_bBuffer = jbuf;
}

void solver_process_jacobian_buffer(solver_t *solver, int cmdCount) {
    if(solver->m_size == 0)
        return;
    PG_TRACE("[Solver::processJacobianBuffer] entering");
    PG_ASSERT_(cmdCount <= solver->m_maxCmdCount, "Command buffer overrun");
    for(int i = 0; i < cmdCount; ++i) {
        jacobianop_t *op = solver->m_jacobianOpBuffer + i;
        PG_ASSERT(op->row >= 0 && op->column >= 0 && op->row < solver->m_size && op->column < solver->m_size, "Out of bounds Jacobian write (%d, %d)", op->row, op->column);
        PG_TRACE("[Solver::processJacobianBuffer] adding %lf to J(%d, %d)", op->change, op->row, op->column);
        sparsematrix_add(&solver->m_A, op->row, op->column, op->change);
    }
    PG_TRACE("[Solver::processJacobianBuffer] returning");
}

void solver_process_rhs_buffer(solver_t *solver) {
    if(solver->m_size == 0)
        return;
    PG_TRACE("[Solver::processRHSBuffer] entering");
    for(int i = 0; i < solver->m_maxCmdCount; ++i) {
        rhsop_t *op = solver->m_rhsOpBuffer + i;
        if(op->row == -1)
            break;
        PG_ASSERT(op->row >= 0 && op->row < solver->m_size, "Out of bounds RHS write (%d)", op->row);
        PG_TRACE("[Solver::processRHSBuffer] adding %lf to RHS(%d)", op->change, op->row);
        solver->m_rhs[op->row] += op->change;
    }
    PG_TRACE("[Solver::processRHSBuffer] returning");
}

static void solver_convergence_problems(solver_t *solver, jobject mnaObj, double norm, int i) {
    jobject buf = (*solver->m_env)->NewDirectByteBuffer(solver->m_env, solver->m_residual, solver->m_size * sizeof(double));
    (*solver->m_env)->CallVoidMethod(solver->m_env, mnaObj, solver->m_reportProblemsMethod, norm, i, buf);
}

jobject solver_single_tick(solver_t *solver, int maxIters, jobject mnaObj, int cmdCount) {
    if(solver->m_size == 0)
        return 0;
    PG_TRACE("[Solver::singleTick] entering");
    solver_process_jacobian_buffer(solver, cmdCount);
    solver_process_rhs_buffer(solver);
    PG_TRACE("[Solver::singleTick] RHS buffer processed");

    int i;
    double norm = 0;
    char skipped = FALSE;
    double beta = 0;
    int normDown = 0, normUp = 0;
    for(i = 0; i < maxIters; ++i) {
        if(!skipped) {
            // Run inner hooks
            int cmdCount = 0;
            if(i < maxIters - 10)
                cmdCount = (*solver->m_env)->CallIntMethod(solver->m_env, mnaObj, solver->m_iterHookMethod, i, solver->m_stateBuffer);
            if(cmdCount != 0)
                solver_process_jacobian_buffer(solver, cmdCount);
        }

        // Compute residual vector
        memcpy(solver->m_b, solver->m_rhs, solver->m_size * sizeof(double));
        (*solver->m_env)->CallVoidMethod(solver->m_env, mnaObj, solver->m_residualAddMethod, solver->m_bBuffer);
        memcpy(solver->m_residual, solver->m_b, solver->m_size * sizeof(double));
        int inc = 1;
        
        char trans = 'N';
        // R = A * x - R
        SuperMatrix* A = sparsematrix_supermatrix(&solver->m_A);
        sp_dgemv(&trans, 1.0, A, solver->m_state, 1, -1.0, solver->m_residual, 1);
        int idxMax = idamax_(&solver->m_size, solver->m_residual, &inc);
        double nextNorm = fabs(solver->m_residual[idxMax - 1]);
        if(i != 0 && nextNorm > norm) {
            if(!skipped) {
                normDown = 0;
                ++normUp;
                double alpha = -0.5;
                daxpy_(&solver->m_size, &alpha, solver->m_stateDelta, &inc, solver->m_state, &inc);
                skipped = TRUE;
                --i;
//                beta = -0.5;
                continue;
            }
        } else {
            normUp = 0;
            ++normDown;
        }
        if(normUp >= 4) {
            beta = -0.5;
        } else if(normDown >= 4) {
            beta *= 0.5;
        }
        skipped = FALSE;
        double dNorm = fabs(nextNorm - norm);
        norm = nextNorm;
        if(norm < solver->m_absoluteStoppingCriterion || dNorm < solver->m_relativeStoppingCriterion)
            break;
        if(solver->m_converged && i >= maxIters - 11) {
            // Right before non-linear devices are disabled.
            // Only append new problem frames if the network has been converging before.
            solver->m_converged = FALSE;
            solver_convergence_problems(solver, mnaObj, norm, i);
        }

        // Solve A * x = b
        sparsematrix_solve(&solver->m_A, &solver->m_B);
        // B is now the state vector
        solver_swap_buffers(solver);
        // x = new state, b = old state
        {
            double alpha = -1.0;
            memcpy(solver->m_stateDelta, solver->m_state, solver->m_size * sizeof(double));
            daxpy_(&solver->m_size, &alpha, solver->m_b, &inc, solver->m_stateDelta, &inc);
            if(beta < 0)
                daxpy_(&solver->m_size, &beta, solver->m_stateDelta, &inc, solver->m_state, &inc);
        }

        sparsematrix_same_pattern(&solver->m_A, 1);
    }
    PG_TRACE("[Solver::singleTick] post loop");

    if(norm > solver->m_minimumAllowedPrecision) {
        if(solver->m_converged)
            solver_convergence_problems(solver, mnaObj, norm, i);
        solver->m_converged = FALSE;
    } else {
        solver->m_converged = i < maxIters - 10;
    }

    solver->m_aux->status = solver->m_converged ? 1 : 0;
    PG_TRACE("[Solver::singleTick] returning");
    return solver->m_stateBuffer;
}

void solver_set_precision(solver_t *solver, double absolute, double relative, double minimum) {
    solver->m_absoluteStoppingCriterion = absolute;
    solver->m_relativeStoppingCriterion = relative;
    solver->m_minimumAllowedPrecision = minimum;
}

