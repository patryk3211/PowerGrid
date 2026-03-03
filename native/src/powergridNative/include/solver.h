#pragma once

#include "sparse.h"
#include <jni.h>

typedef struct RHSOp {
    int row;
    double change;
}__attribute__((packed)) rhsop_t;

typedef struct JacobianOp {
    int row;
    int column;
    double change;
}__attribute__((packed)) jacobianop_t;

typedef struct AuxBuf {
    uint8_t status;
}__attribute((packed)) auxbuf_t;

typedef struct Solver {
    JNIEnv *m_env;
    jobject m_mnaObject;

    jmethodID m_iterHookMethod;
    jmethodID m_residualAddMethod;
    jmethodID m_reportProblemsMethod;
    
    sparsematrix_t m_A;
    int m_size;

    double *m_state;
    double *m_b;

    double *m_rhs;
    double *m_residual;
    double *m_stateDelta;

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
    rhsop_t *m_rhsOpBuffer;
    jacobianop_t *m_jacobianOpBuffer;

    auxbuf_t *m_aux;

    char m_converged;
} solver_t;

void solver_init(solver_t *solver, void *rhsOpBuf, void *jacobianOpBuf, int cmdCount, void *auxBuf, JNIEnv *env, jobject mnaObj);
void solver_destroy(solver_t *solver);

void solver_resize(solver_t *solver, int size);

void solver_zero_state(solver_t *solver);
void solver_zero_rhs(solver_t *solver);
void solver_zero_jacobian(solver_t *solver);

void solver_process_jacobian_buffer(solver_t *solver, int cmdCount);
void solver_process_rhs_buffer(solver_t *solver);

void solver_set_precision(solver_t *solver, double absolute, double relative, double minimum);

jobject solver_single_tick(solver_t *solver, int maxIters, jobject mnaObj, int cmdCount);


// namespace powergrid {

    // class Solver {
    //     JNIEnv *m_env;
    //     jobject m_mnaObject;

    //     jmethodID m_iterHookMethod;
    //     jmethodID m_residualAddMethod;
    //     jmethodID m_reportProblemsMethod;

    //     SparseMatrix m_A;
    //     int m_size;

    //     std::vector<double> m_vec1;
    //     std::vector<double> m_vec2;

    //     std::vector<double> m_rhs;
    //     std::vector<double> m_residual;
    //     std::vector<double> m_stateDelta;

    //     double *m_state;
    //     double *m_b;

    //     jobject m_stateBuffer;
    //     jobject m_bBuffer;

    //     SuperMatrix m_X;
    //     DNformat m_Xstore;
    //     SuperMatrix m_B;
    //     DNformat m_Bstore;

    //     double m_minimumAllowedPrecision;
    //     double m_absoluteStoppingCriterion;
    //     double m_relativeStoppingCriterion;

    //     int m_maxCmdCount;
    //     RHSOp *m_rhsOpBuffer;
    //     JacobianOp *m_jacobianOpBuffer;

    //     AuxBuf *m_aux;

    //     bool m_converged;

    // public:
    //     Solver(void *rhsOpBuf, void *jacobianOpBuf, int cmdCount, void *auxBuf, JNIEnv *env, jobject mnaObj);
    //     ~Solver();

    //     void resize(int size);

    //     void zeroState();
    //     void zeroRHS();
    //     void zeroJacobian();

    //     void finishJacobianWrite(int cmdCount);
    //     void processJacobianBuffer(int cmdCount);
    //     void processRHSBuffer();

    //     jobject singleTick(int maxIters, jobject mnaObj, int cmdCount);
    //     void swapBuffers();

    //     void setPrecision(double absolute, double relative, double minimum);

    //     void convergenceProblems(jobject mnaObj, double norm, int i);

    //     int size() const {
    //         return m_size;
    //     }
    // };
// }

