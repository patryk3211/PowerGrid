#include "solver.hpp"
#include "util.hpp"

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

    jclass clazz = env->GetObjectClass(mnaObj);
    m_iterHookMethod = env->GetMethodID(clazz, "runIterHooks", "(Ljava/nio/ByteBuffer;)V");
    m_residualAddMethod = env->GetMethodID(clazz, "runAddResidual", "(Ljava/nio/ByteBuffer;)V");
}

Solver::~Solver() {

}

void Solver::resize(int size) {
    if(m_size == size)
        return;

    m_A.resize(size);
    m_vec1.resize(size);
    m_vec2.resize(size);
    m_rhs.resize(size);

    m_Xstore.lda = m_X.nrow = size;
    m_Bstore.lda = m_B.nrow = size;
    m_Xstore.nzval = m_state = m_vec1.data();
    m_Bstore.nzval = m_residual = m_vec2.data();
    m_size = size;
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
    m_state = m_residual;
    m_residual = buf;

    m_Xstore.nzval = m_state;
    m_Bstore.nzval = m_residual;
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

void *Solver::singleTick(int maxIters, jobject mnaObj) {
    processJacobianBuffer();
    processRHSBuffer();

    std::cout << mnaObj << std::endl;
    for(int i = 0; i < maxIters; ++i) {
        // TODO: Improve this naive approach
        jobject stateBuffer = m_env->NewDirectByteBuffer(m_state, m_size * sizeof(double));
        jobject residualBuffer = m_env->NewDirectByteBuffer(m_residual, m_size * sizeof(double));

        // Run inner hooks
        m_env->CallVoidMethod(mnaObj, m_iterHookMethod, stateBuffer);

        // Compute residual vector
        for(int i = 0; i < m_size; ++i) {
            m_residual[i] = -m_rhs[i];
        }
        m_env->CallVoidMethod(mnaObj, m_residualAddMethod, residualBuffer);
        for(int i = 0; i < m_size; ++i) {
            m_residual[i] = -m_residual[i];
        }

        // Solve A * x = b
        m_A.solve(&m_B);
        // B is now the state vector
        swapBuffers();
    }

    return m_state;
}

