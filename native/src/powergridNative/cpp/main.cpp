#include <jni.h>
#include <iostream>

extern "C" JNIEXPORT void JNICALL Java_org_patryk3211_powergrid_PowerGridNative_print(JNIEnv* env, jobject obj) {
    std::cout << "Hello from C++!" << std::endl;
}
