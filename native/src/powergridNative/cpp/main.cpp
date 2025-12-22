#include <jni.h>
#include <iostream>

#include "dense.hpp"
#include "operations.hpp"

using namespace powergrid;

extern "C" JNIEXPORT void JNICALL Java_org_patryk3211_powergrid_PowerGridNative_print(JNIEnv* env, jobject obj) {
    std::cout << "Hello from C++!" << std::endl;
}

extern "C" JNIEXPORT void JNICALL Java_org_patryk3211_powergrid_PowerGridNative_factorize(JNIEnv* env, jobject obj, jdoubleArray A, jdoubleArray LU, jintArray pvt) {
    jboolean isCopy;
    jdouble *body = env->GetDoubleArrayElements(A, &isCopy);

    pg_number_t data[9];
    for(int i = 0; i < 9; ++i) {
        data[i] = body[i];
    }

    if(isCopy) {
        env->ReleaseDoubleArrayElements(A, body, JNI_ABORT);
    }

    pg_number_t m_lu[9];
    pg_size_t m_pvt[3];

    factorizeDense(data, 3, m_lu, m_pvt);

    env->SetDoubleArrayRegion(LU, 0, 9, (jdouble *) m_lu);
    env->SetIntArrayRegion(pvt, 0, 3, (jint *) m_pvt);
}

