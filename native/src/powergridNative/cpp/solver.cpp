#include "solver.hpp"
#include "util.hpp"
#include "blas.h"

using namespace powergrid;

Solver::Solver(void *rhsOpBuf, void *jacobianOpBuf, int cmdCount, JNIEnv *env, jobject mnaObj)
    : m_env(env)
    , m_mnaObject(mnaObj)
    , m_vec1()
    , m_vec2()
    , m_rhs()
    , m_maxCmdCount(cmdCount)
    , m_rhsOpBuffer((RHSOp *) rhsOpBuf)
    , m_jacobianOpBuffer((JacobianOp *) jacobianOpBuf) {
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
    m_iterHookMethod = env->GetMethodID(clazz, "runIterHooks", "(Ljava/nio/ByteBuffer;)V");
    m_residualAddMethod = env->GetMethodID(clazz, "runAddResidual", "(Ljava/nio/ByteBuffer;)V");
    m_reportProblemsMethod = env->GetMethodID(clazz, "reportConvergenceProblems", "(DILjava/nio/ByteBuffer;)V");

    m_stateBuffer = nullptr;
    m_bBuffer = nullptr;
}

Solver::~Solver() {
    if(m_stateBuffer != nullptr)
        m_env->DeleteGlobalRef(m_stateBuffer);
    if(m_bBuffer != nullptr)
        m_env->DeleteGlobalRef(m_bBuffer);
}

void Solver::resize(int size) {
    if(m_size == size)
        return;

    m_A.resize(size);
    m_vec1.resize(size);
    m_vec2.resize(size);
    m_residual.resize(size);
    m_rhs.resize(size);

    m_Xstore.lda = m_X.nrow = size;
    m_Bstore.lda = m_B.nrow = size;
    m_Xstore.nzval = m_state = m_vec1.data();
    m_Bstore.nzval = m_b = m_vec2.data();
    m_size = size;

    if(m_stateBuffer != nullptr)
        m_env->DeleteGlobalRef(m_stateBuffer);
    if(m_bBuffer != nullptr)
        m_env->DeleteGlobalRef(m_bBuffer);

    m_stateBuffer = m_env->NewDirectByteBuffer(m_state, m_size * sizeof(double));
    m_bBuffer = m_env->NewDirectByteBuffer(m_b, m_size * sizeof(double));

    m_stateBuffer = m_env->NewGlobalRef(m_stateBuffer);
    m_bBuffer = m_env->NewGlobalRef(m_bBuffer);
}

void Solver::zeroState() {
    memset(m_state, 0, sizeof(double) * m_size);
}

void Solver::zeroRHS() {
    memset(m_rhs.data(), 0, sizeof(double) * m_size);
}

void Solver::zeroJacobian() {
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

void Solver::finishJacobianWrite() {
    m_A.sortRows();
}

void Solver::processJacobianBuffer() {
    for(int i = 0; i < m_maxCmdCount; ++i) {
        JacobianOp& op = m_jacobianOpBuffer[i];
        if(op.row == -1)
            break;
        PG_ASSERT(op.row >= 0 && op.column >= 0 && op.row < m_size && op.column < m_size, "Out of bounds write");
        m_A.add(op.row, op.column, op.change);
    }
}

void Solver::processRHSBuffer() {
    for(int i = 0; i < m_maxCmdCount; ++i) {
        RHSOp& op = m_rhsOpBuffer[i];
        if(op.row == -1)
            break;
        PG_ASSERT(op.row >= 0 && op.row < m_size, "Out of bounds write");
        m_rhs[op.row] += op.change;
    }
}

void Solver::convergenceProblems(jobject mnaObj, double norm, int i) {
    jobject buf = m_env->NewDirectByteBuffer(m_residual.data(), m_size * sizeof(double));
    m_env->CallVoidMethod(mnaObj, m_reportProblemsMethod, norm, i, buf);
}

jobject Solver::singleTick(int maxIters, jobject mnaObj) {
    processJacobianBuffer();
    processRHSBuffer();

    int i;
    double norm = 0;
    for(i = 0; i < maxIters; ++i) {
        // Run inner hooks
        m_env->CallVoidMethod(mnaObj, m_iterHookMethod, m_stateBuffer);

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
    }

    if(norm > m_minimumAllowedPrecision) {
        if(m_converged)
            convergenceProblems(mnaObj, norm, i);
        m_converged = false;
    } else {
        m_converged = i < maxIters - 10;
    }

    return m_converged ? m_stateBuffer : nullptr;
}

void Solver::setPrecision(double absolute, double relative, double minimum) {
    m_absoluteStoppingCriterion = absolute;
    m_relativeStoppingCriterion = relative;
    m_minimumAllowedPrecision = minimum;
}

