#include <jni.h>

#define INVOKE_NONVIRT_SUPER_TYPE_METHOD(_jtype, _jname)                                            \
    extern "C" JNIEXPORT _jtype JNICALL                                                             \
    Java_com_yifeplayte_wommo_utils_Object_invokeSuper##_jname##Method(JNIEnv *env,                 \
                                                                          jobject /* this */,       \
                                                                          jobject obj,              \
                                                                          jstring methodName,       \
                                                                          jstring methodSignature,  \
                                                                          jobjectArray args) {      \
    jclass superClazz = env->GetSuperclass(env->GetObjectClass(obj));                               \
    jmethodID superMethod = env->GetMethodID(superClazz,                                            \
                                             env->GetStringUTFChars(methodName, nullptr),           \
                                             env->GetStringUTFChars(methodSignature, nullptr));     \
    env->ReleaseStringUTFChars(methodName, nullptr);                                                \
    env->ReleaseStringUTFChars(methodSignature, nullptr);                                           \
    return env->CallNonvirtual##_jname##MethodA(obj, superClazz, superMethod, getArgs(env, args));  \
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_6;
}

jvalue *getArgs(JNIEnv *env, jobjectArray args) {
    auto *argsArray = new jvalue[env->GetArrayLength(args)];
    for (int i = 0; i < env->GetArrayLength(args); ++i) {
        argsArray[i].l = env->GetObjectArrayElement(args, i);
    }
    return argsArray;
}

INVOKE_NONVIRT_SUPER_TYPE_METHOD(jobject, Object)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jboolean, Boolean)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jbyte, Byte)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jchar, Char)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jshort, Short)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jint, Int)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jlong, Long)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jfloat, Float)
INVOKE_NONVIRT_SUPER_TYPE_METHOD(jdouble, Double)

extern "C" JNIEXPORT void JNICALL
Java_com_yifeplayte_wommo_utils_Object_invokeSuperVoidMethod(JNIEnv *env,
                                                             jobject /* this */,
                                                             jobject obj,
                                                             jstring methodName,
                                                             jstring methodSignature,
                                                             jobjectArray args) {
    jclass superClazz = env->GetSuperclass(env->GetObjectClass(obj));
    jmethodID superMethod = env->GetMethodID(superClazz,
                                             env->GetStringUTFChars(methodName, nullptr),
                                             env->GetStringUTFChars(methodSignature, nullptr));
    env->ReleaseStringUTFChars(methodName, nullptr);
    env->ReleaseStringUTFChars(methodSignature, nullptr);
    env->CallNonvirtualVoidMethodA(obj, superClazz, superMethod, getArgs(env, args));
}
