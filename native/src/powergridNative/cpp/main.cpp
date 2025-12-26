#include <jni.h>
#include <iostream>

#include "slu_ddefs.h"
#include "sparse.hpp"

using namespace powergrid;

extern "C" JNIEXPORT void JNICALL Java_org_patryk3211_powergrid_PowerGridNative_factorize(JNIEnv* env, jobject obj, jdoubleArray A, jdoubleArray LU, jintArray pvt) {
    jboolean isCopy;
    jdouble *body = env->GetDoubleArrayElements(A, &isCopy);

    SparseMatrix matrix;
    matrix.resize(3);
    matrix.set(0, 0, 2);
    matrix.set(0, 1, -1);
    matrix.set(1, 1, 3);
    matrix.set(1, 0, -1);
    matrix.set(1, 2, -2);
    matrix.set(2, 1, -2);
    matrix.set(2, 2, 3);

    SuperMatrix B;
    double bData[] = { 4, 2, 1 };
    dCreate_Dense_Matrix(&B, 3, 1, bData, 3, SLU_DN, SLU_D, SLU_GE);

    // pg_number_t data[9];
    // for(int i = 0; i < 9; ++i) {
    //     data[i] = body[i];
    // }

    // if(isCopy) {
    //     env->ReleaseDoubleArrayElements(A, body, JNI_ABORT);
    // }

    double m_lu[9];
    // pg_size_t m_pvt[3];

    matrix.factorize();
    matrix.solve(&B);

    // factorizeDense(data, 3, m_lu, m_pvt);

    env->SetDoubleArrayRegion(LU, 0, 3, (jdouble *) bData);
    // env->SetIntArrayRegion(pvt, 0, 3, (jint *) m_pvt);
}

