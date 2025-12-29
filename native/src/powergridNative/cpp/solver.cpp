#include "solver.hpp"
#include "util.hpp"
#include "blas.h"

using namespace powergrid;

Solver::Solver(void *rhsOpBuf, void *jacobianOpBuf, int cmdCount, JNIEnv *env, jobject mnaObj)
    : m_env(env)
    , m_mnaObject(mnaObj)
    , m_maxCmdCount(cmdCount)
    , m_rhsOpBuffer((RHSOp *) rhsOpBuf)
    , m_jacobianOpBuffer((JacobianOp *) jacobianOpBuf) {
    PG_TRACE("[Solver::Solver] entering");
    m_X.ncol = 1;
    m_X.Stype = SLU_DN;
    m_X.Dtype = SLU_D;
    m_X.Mtype = SLU_GE;
    m_X.Store = &m_Xstore;

    m_B.ncol = 1;
    m_B.Stype = SLU_DN;
    m_B.Dtype = SLU_D;
    m_B.Mtype = SLU_GE;
    m_B.Store = &m_Bstore;
    
    m_minimumAllowedPrecision = 1e-6;
    m_absoluteStoppingCriterion = 1e-7;
    m_relativeStoppingCriterion = 1e-12;

    jclass clazz = env->GetObjectClass(mnaObj);
    m_iterHookMethod = env->GetMethodID(clazz, "runIterHooks", "(Ljava/nio/ByteBuffer;)I");
    PG_ASSERT(m_iterHookMethod != nullptr, "runIterHooks method not found!");
    m_residualAddMethod = env->GetMethodID(clazz, "runAddResidual", "(Ljava/nio/ByteBuffer;)V");
    PG_ASSERT(m_residualAddMethod != nullptr, "runAddResidual method not found!");
    m_reportProblemsMethod = env->GetMethodID(clazz, "reportConvergenceProblems", "(DILjava/nio/ByteBuffer;)V");
    PG_ASSERT(m_reportProblemsMethod != nullptr, "reportConvergenceProblems method not found!");

    m_size = 0;
    m_state = nullptr;
    m_b = nullptr;
    m_stateBuffer = nullptr;
    m_bBuffer = nullptr;
    PG_TRACE("[Solver::Solver] returning");
}

Solver::~Solver() {
    PG_TRACE("[Solver::~Solver] entering");
    if(m_stateBuffer != nullptr)
        m_env->DeleteGlobalRef(m_stateBuffer);
    if(m_bBuffer != nullptr)
        m_env->DeleteGlobalRef(m_bBuffer);
    PG_TRACE("[Solver::~Solver] returning");
}

void Solver::resize(int size) {
    if(m_size == size)
        return;

    PG_TRACE("[Solver::resize] Resizing solver state to {}", size);
    m_A.resize(size);
    m_vec1.resize(size);
    m_vec2.resize(size);
    m_residual.resize(size);
    m_rhs.resize(size);

    PG_TRACE("[Solver::resize] Assigning new pointers", size);
    m_Xstore.lda = m_X.nrow = size;
    m_Bstore.lda = m_B.nrow = size;
    m_Xstore.nzval = m_state = m_vec1.data();
    m_Bstore.nzval = m_b = m_vec2.data();
    m_size = size;

    PG_TRACE("[Solver::resize] Deleting old jBuffers", size);
    if(m_stateBuffer != nullptr)
        m_env->DeleteGlobalRef(m_stateBuffer);
    if(m_bBuffer != nullptr)
        m_env->DeleteGlobalRef(m_bBuffer);

    PG_TRACE("[Solver::resize] Allocating new jBuffers", size);
    m_stateBuffer = m_env->NewDirectByteBuffer(m_state, m_size * sizeof(double));
    m_bBuffer = m_env->NewDirectByteBuffer(m_b, m_size * sizeof(double));

    m_stateBuffer = m_env->NewGlobalRef(m_stateBuffer);
    m_bBuffer = m_env->NewGlobalRef(m_bBuffer);

    PG_TRACE("[Solver::resize] returning", size);
}

void Solver::zeroState() {
    if(m_state == nullptr)
        return;
    memset(m_state, 0, sizeof(*m_state) * m_size);
}

void Solver::zeroRHS() {
    if(m_size == 0)
        return;
    std::fill(m_rhs.begin(), m_rhs.end(), 0);
}

void Solver::zeroJacobian() {
    if(m_size == 0)
        return;
    PG_TRACE("[Solver::zeroJacobian] zeroing");
    m_A.zero();
}

void Solver::swapBuffers() {
    double *buf = m_state;
    m_state = m_b;
    m_b = buf;

    m_Xstore.nzval = m_state;
    m_Bstore.nzval = m_b;

    jobject jbuf = m_stateBuffer;
    m_stateBuffer = m_bBuffer;
    m_bBuffer = jbuf;
}

void Solver::finishJacobianWrite(int cmdCount) {
    if(m_size == 0)
        return;
    PG_TRACE("[Solver::finishJacobianWrite] finishing Jacobian write");
    processJacobianBuffer(cmdCount);
    m_A.sortRows();
    PG_TRACE("[Solver::finishJacobianWrite] Jacobian sorted");
}

void Solver::processJacobianBuffer(int cmdCount) {
    PG_TRACE("[Solver::processJacobianBuffer] entering");
    PG_ASSERT(cmdCount < m_maxCmdCount, "Command buffer overrun");
    for(int i = 0; i < cmdCount; ++i) {
        JacobianOp& op = m_jacobianOpBuffer[i];
        if(op.row == -1)
            break;
        PG_ASSERT(op.row >= 0 && op.column >= 0 && op.row < m_size && op.column < m_size, "Out of bounds Jacobian write ({}, {})", op.row, op.column);
        PG_TRACE("[Solver::processJacobianBuffer] adding {} to J({}, {})", op.change, op.row, op.column);
        m_A.add(op.row, op.column, op.change);
    }
    PG_TRACE("[Solver::processJacobianBuffer] returning");
}

void Solver::processRHSBuffer() {
    PG_TRACE("[Solver::processRHSBuffer] entering");
    for(int i = 0; i < m_maxCmdCount; ++i) {
        RHSOp& op = m_rhsOpBuffer[i];
        if(op.row == -1)
            break;
        PG_ASSERT(op.row >= 0 && op.row < m_size, "Out of bounds RHS write ({})", op.row);
        PG_TRACE("[Solver::processRHSBuffer] adding {} to RHS({})", op.change, op.row);
        m_rhs[op.row] += op.change;
    }
    PG_TRACE("[Solver::processRHSBuffer] returning");
}

void Solver::convergenceProblems(jobject mnaObj, double norm, int i) {
    jobject buf = m_env->NewDirectByteBuffer(m_residual.data(), m_size * sizeof(double));
    m_env->CallVoidMethod(mnaObj, m_reportProblemsMethod, norm, i, buf);
}

jobject Solver::singleTick(int maxIters, jobject mnaObj, int cmdCount) {
    PG_TRACE("[Solver::singleTick] entering");
    processJacobianBuffer(cmdCount);
    processRHSBuffer();
    PG_TRACE("[Solver::singleTick] RHS buffer processed");

    int i;
    double norm = 0;
    for(i = 0; i < maxIters; ++i) {
        // Run inner hooks
        int cmdCount = m_env->CallIntMethod(mnaObj, m_iterHookMethod, m_stateBuffer);
        if(cmdCount != 0)
            processJacobianBuffer(cmdCount);

        // Compute residual vector
        memcpy(m_b, m_rhs.data(), m_size * sizeof(double));
        m_env->CallVoidMethod(mnaObj, m_residualAddMethod, m_bBuffer);
        memcpy(m_residual.data(), m_b, m_size * sizeof(double));
        int inc = 1;
        
        char trans = 'N';
        // R = A * x - R
        sp_dgemv(&trans, 1.0, m_A.superMatrix(), m_state, 1, -1.0, m_residual.data(), 1);
        double nextNorm = dasum_(&m_size, m_residual.data(), &inc);
        double dNorm = abs(nextNorm - norm);
        norm = nextNorm;
        if(norm < m_absoluteStoppingCriterion || dNorm < m_relativeStoppingCriterion)
            break;
        if(m_converged && i >= maxIters - 12) {
            // Right before non-linear devices are disabled.
            // Only append new problem frames if the network has been converging before.
            m_converged = norm < m_minimumAllowedPrecision;
            if(!m_converged)
                convergenceProblems(mnaObj, norm, i);
        }

        // Solve A * x = b
        m_A.solve(&m_B);
        // B is now the state vector
        swapBuffers();

        // m_A.samePattern(true);
    }
    PG_TRACE("[Solver::singleTick] post loop");

    if(norm > m_minimumAllowedPrecision) {
        if(m_converged)
            convergenceProblems(mnaObj, norm, i);
        m_converged = false;
    } else {
        m_converged = i < maxIters - 10;
    }

    PG_TRACE("[Solver::singleTick] returning");
    return m_converged ? m_stateBuffer : nullptr;
}

void Solver::setPrecision(double absolute, double relative, double minimum) {
    m_absoluteStoppingCriterion = absolute;
    m_relativeStoppingCriterion = relative;
    m_minimumAllowedPrecision = minimum;
}

