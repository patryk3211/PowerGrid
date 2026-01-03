#include <jni.h>
#include <cstdint>

#include "solver.hpp"

using namespace powergrid;

#define MANGLE(methodName) Java_org_patryk3211_powergrid_electricity_sim_solver_NativeMNA_##methodName

// The code assumes that these assumptions are valid.
static_assert(sizeof(jlong) == sizeof(uintptr_t));
static_assert(sizeof(double) == 8)
static_assert(sizeof(int) == 4)
static_assert(sizeof(jdouble) == sizeof(double))
static_assert(sizeof(jint) == sizeof(int))

#define SOLVER(intptr) ((Solver *) (intptr))

extern "C" {
    JNIEXPORT jlong JNICALL MANGLE(allocateNativeObject)(JNIEnv *env, jobject obj, jobject rhsOpBuf, jobject jOpBuf, jint maxCmdCount, jobject mnaObj) {
        void *rhs = env->GetDirectBufferAddress(rhsOpBuf);
        void *j = env->GetDirectBufferAddress(jOpBuf);
        return (uintptr_t) new Solver(rhs, j, maxCmdCount, env, mnaObj);
    }

    JNIEXPORT void JNICALL MANGLE(deallocateNativeObject)(JNIEnv *env, jobject obj, jlong intptr) {
        delete SOLVER(intptr);
    }

    JNIEXPORT void JNICALL MANGLE(setStateSize)(JNIEnv *env, jobject obj, jlong intptr, jint size) {
        SOLVER(intptr)->resize(size);
    }

    JNIEXPORT void JNICALL MANGLE(zeroRHS)(JNIEnv *env, jobject obj, jlong ptr) {
        SOLVER(ptr)->zeroRHS();
    }

    JNIEXPORT void JNICALL MANGLE(zeroState)(JNIEnv *env, jobject obj, jlong ptr) {
        SOLVER(ptr)->zeroState();
    }

    JNIEXPORT void JNICALL MANGLE(zeroJacobian)(JNIEnv *env, jobject obj, jlong ptr) {
        SOLVER(ptr)->zeroJacobian();
    }

    JNIEXPORT void JNICALL MANGLE(finishJacobianWrite)(JNIEnv *env, jobject obj, jlong ptr, jint cmdCount) {
        SOLVER(ptr)->finishJacobianWrite(cmdCount);
    }

    JNIEXPORT void JNICALL MANGLE(processJacobianBuffer)(JNIEnv *env, jobject obj, jlong ptr, jint cmdCount) {
        SOLVER(ptr)->processJacobianBuffer(cmdCount);
    }

    JNIEXPORT void JNICALL MANGLE(processRHSBuffer)(JNIEnv *env, jobject obj, jlong ptr) {
        SOLVER(ptr)->processRHSBuffer();
    }

    JNIEXPORT jobject JNICALL MANGLE(singleTick)(JNIEnv *env, jobject mnaObj, jlong ptr, jint maxIters, jint jCmdCount) {
        Solver *solver = SOLVER(ptr);
        return solver->singleTick(maxIters, mnaObj, jCmdCount);
    }

    JNIEXPORT void JNICALL MANGLE(setPrecision)(JNIEnv *env, jobject obj, jlong ptr, jdouble absolute, jdouble relative, jdouble minimum) {
        SOLVER(ptr)->setPrecision(absolute, relative, minimum);
    }
}

