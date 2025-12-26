#include <jni.h>
#include <cstdint>
#include <iostream>

#include "solver.hpp"

using namespace powergrid;

#define MANGLE(methodName) Java_org_patryk3211_powergrid_electricity_sim_solver_NativeMNA_##methodName

static_assert(sizeof(jlong) == sizeof(uintptr_t));

#define SOLVER(intptr) ((Solver *) (intptr))

extern "C" {
    JNIEXPORT jlong JNICALL MANGLE(allocateNativeObject)(JNIEnv *env, jobject obj, jobject rhsOpBuf, jobject jOpBuf, jint maxCmdCount) {
        void *rhs = env->GetDirectBufferAddress(rhsOpBuf);
        void *j = env->GetDirectBufferAddress(jOpBuf);
        return (uintptr_t) new Solver(rhs, j, maxCmdCount);
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

    JNIEXPORT void JNICALL MANGLE(processJacobianBuffer)(JNIEnv *env, jobject obj, jlong ptr) {
        SOLVER(ptr)->processJacobianBuffer();
    }

    JNIEXPORT void JNICALL MANGLE(processRHSBuffer)(JNIEnv *env, jobject obj, jlong ptr) {
        SOLVER(ptr)->processRHSBuffer();
    }

    JNIEXPORT jobject JNICALL MANGLE(singleTick)(JNIEnv *env, jobject obj, jlong ptr) {
        Solver *solver = SOLVER(ptr);
        void *state = solver->singleTick();
        return env->NewDirectByteBuffer(state, solver->size() * sizeof(double));
    }
}

