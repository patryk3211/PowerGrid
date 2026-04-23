#include <jni.h>
#include <stdint.h>

#include "solver.h"
#include "exception_handler.h"

#define MANGLE(methodName) Java_org_patryk3211_powergrid_electricity_sim_solver_NativeMNA_##methodName

// The code assumes that these assumptions are valid.
static_assert(sizeof(jlong) == sizeof(uintptr_t), "Invalid uintptr_t size");
static_assert(sizeof(double) == 8, "Invalid double size");
static_assert(sizeof(int) == 4, "Invalid int size");
static_assert(sizeof(jdouble) == sizeof(double), "Size of jdouble and double don't match");
static_assert(sizeof(jint) == sizeof(int), "Size of jint and int don't match");

#define SOLVER(intptr) ((solver_t *) (intptr))

extern int verificationFunc();

JNIEXPORT jint JNICALL MANGLE(verifySupport)(JNIEnv *env, jobject obj) {
    return powergrid_run_safely(verificationFunc);
}

JNIEXPORT jlong JNICALL MANGLE(allocateNativeObject)(JNIEnv *env, jobject mnaObj, jobject rhsOpBuf, jobject jOpBuf, jint maxCmdCount, jobject auxBuf) {
    void *rhs = (*env)->GetDirectBufferAddress(env, rhsOpBuf);
    void *j = (*env)->GetDirectBufferAddress(env, jOpBuf);
    void *aux = (*env)->GetDirectBufferAddress(env, auxBuf);

    solver_t *solver = malloc(sizeof(solver_t));
    solver_init(solver, rhs, j, maxCmdCount, aux, env, mnaObj);
    return (uintptr_t) solver;
}

JNIEXPORT void JNICALL MANGLE(deallocateNativeObject)(JNIEnv *env, jobject obj, jlong intptr) {
    solver_t *solver = SOLVER(intptr);
    solver_destroy(solver);
    free(solver);
}

JNIEXPORT void JNICALL MANGLE(setStateSize)(JNIEnv *env, jobject obj, jlong intptr, jint size) {
    solver_resize(SOLVER(intptr), size);
}

JNIEXPORT void JNICALL MANGLE(zeroRHS)(JNIEnv *env, jobject obj, jlong ptr) {
    solver_zero_rhs(SOLVER(ptr));
}

JNIEXPORT void JNICALL MANGLE(zeroState)(JNIEnv *env, jobject obj, jlong ptr) {
    solver_zero_state(SOLVER(ptr));
}

JNIEXPORT void JNICALL MANGLE(zeroJacobian)(JNIEnv *env, jobject obj, jlong ptr) {
    solver_zero_jacobian(SOLVER(ptr));
}

JNIEXPORT void JNICALL MANGLE(finishJacobianWrite)(JNIEnv *env, jobject obj, jlong ptr, jint cmdCount) {
    solver_process_jacobian_buffer(SOLVER(ptr), cmdCount);
}

JNIEXPORT void JNICALL MANGLE(processJacobianBuffer)(JNIEnv *env, jobject obj, jlong ptr, jint cmdCount) {
    solver_process_jacobian_buffer(SOLVER(ptr), cmdCount);
}

JNIEXPORT void JNICALL MANGLE(processRHSBuffer)(JNIEnv *env, jobject obj, jlong ptr) {
    solver_process_rhs_buffer(SOLVER(ptr));
}

JNIEXPORT jobject JNICALL MANGLE(singleTick)(JNIEnv *env, jobject mnaObj, jlong ptr, jint maxIters, jint jCmdCount) {
    return solver_single_tick(SOLVER(ptr), maxIters, mnaObj, jCmdCount);
}

JNIEXPORT void JNICALL MANGLE(setPrecision)(JNIEnv *env, jobject obj, jlong ptr, jdouble absolute, jdouble relative, jdouble minimum, jdouble searchAlpha) {
    solver_set_precision(SOLVER(ptr), absolute, relative, minimum, searchAlpha);
}

