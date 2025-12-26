#include "solver.hpp"
#include "util.hpp"

using namespace powergrid;

Solver::Solver(void *rhsOpBuf, void *jacobianOpBuf, int cmdCount)
    : m_vec1()
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

void *Solver::singleTick() {
    processJacobianBuffer();
    processRHSBuffer();

    // Compute residual vector
    memcpy(m_residual, m_rhs.data(), sizeof(double) * m_size);

    // Solve A * x = b
    m_A.solve(&m_B);
    // B is now the state vector
    swapBuffers();

    return m_state;
}

