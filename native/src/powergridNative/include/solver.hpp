#pragma once

#include "sparse.hpp"
#include <jni.h>

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

    struct AuxBuf {
        uint8_t status;
    }__attribute((packed));
        
    class Solver {
        JNIEnv *m_env;
        jobject m_mnaObject;

        jmethodID m_iterHookMethod;
        jmethodID m_residualAddMethod;
        jmethodID m_reportProblemsMethod;

        SparseMatrix m_A;
        int m_size;

        std::vector<double> m_vec1;
        std::vector<double> m_vec2;

        std::vector<double> m_rhs;
        std::vector<double> m_residual;
        std::vector<double> m_stateDelta;

        double *m_state;
        double *m_b;

        jobject m_stateBuffer;
        jobject m_bBuffer;

        SuperMatrix m_X;
        DNformat m_Xstore;
        SuperMatrix m_B;
        DNformat m_Bstore;

        double m_minimumAllowedPrecision;
        double m_absoluteStoppingCriterion;
        double m_relativeStoppingCriterion;

        int m_maxCmdCount;
        RHSOp *m_rhsOpBuffer;
        JacobianOp *m_jacobianOpBuffer;

        AuxBuf *m_aux;

        bool m_converged;

    public:
        Solver(void *rhsOpBuf, void *jacobianOpBuf, int cmdCount, void *auxBuf, JNIEnv *env, jobject mnaObj);
        ~Solver();

        void resize(int size);

        void zeroState();
        void zeroRHS();
        void zeroJacobian();

        void finishJacobianWrite(int cmdCount);
        void processJacobianBuffer(int cmdCount);
        void processRHSBuffer();

        jobject singleTick(int maxIters, jobject mnaObj, int cmdCount);
        void swapBuffers();

        void setPrecision(double absolute, double relative, double minimum);

        void convergenceProblems(jobject mnaObj, double norm, int i);

        int size() const {
            return m_size;
        }
    };
}

