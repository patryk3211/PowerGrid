#pragma once

#include "sparse.hpp"

namespace powergrid {
    struct RHSOp {
        int row;
        double change;
    }__attribute__((packed));

    struct JacobianOp {
        int row;
        int column;
        double change;
    }__attribute__((packed));
        
    class Solver {
        SparseMatrix m_A;
        int m_size;

        std::vector<double> m_vec1;
        std::vector<double> m_vec2;

        std::vector<double> m_rhs;

        double *m_residual;
        double *m_state;

        SuperMatrix m_X;
        DNformat m_Xstore;
        SuperMatrix m_B;
        DNformat m_Bstore;

        int m_maxCmdCount;
        RHSOp *m_rhsOpBuffer;
        JacobianOp *m_jacobianOpBuffer;

    public:
        Solver(void *rhsOpBuf, void *jacobianOpBuf, int cmdCount);
        ~Solver();

        void resize(int size);

        void zeroState();
        void zeroRHS();
        void zeroJacobian();

        void processJacobianBuffer();
        void processRHSBuffer();

        void *singleTick();
        void swapBuffers();

        int size() const {
            return m_size;
        }
    };
}

