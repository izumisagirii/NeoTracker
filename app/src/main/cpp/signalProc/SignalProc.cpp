//
// Created by CUI on 3/21/24.
//

#include "SignalProc.h"

void SignalProc::processStream(const double value, const double move_value) {
    buffer.push_back(value);
    buffer_filtered.push_back(move_value);
    if (buffer.size() > peak_interval) {
        auto max_it = std::max_element(buffer.end() - peak_interval, buffer.end());
        auto length = std::distance(buffer.begin(), max_it);
        if (length >= ZC_LENGTH - 5) {
            __android_log_print(ANDROID_LOG_INFO, "THREAD_PRO_FRAME", "Length value: %d", length);
            std::vector<double> output(ZC_LENGTH);
            std::copy_n(buffer_filtered.begin(), ZC_LENGTH, output.begin());
            buffer_filtered.erase(buffer_filtered.begin(), buffer_filtered.begin() + length);
            buffer.erase(buffer.begin(), buffer.begin() + length);

            auto max_it_output = std::max_element(output.begin(), output.end());
            auto max_index = std::distance(output.begin(), max_it_output);
            auto max_value = *max_it_output;

            if (max_value > 20000) {
                this->sendToJavaOp({static_cast<int>(max_index)});
            } else {
                this->sendToJavaOp({});
            }

            auto future = std::async(std::launch::async, &SignalProc::sendToJava, this, output);
        }
    }
}


void SignalProc::sendToJava(const std::vector<double> &data) {
    JNIEnv *env = nullptr;
    JavaVMAttachArgs args;
    char name[] = "Response Show Service Callback Thread";
    args.version = JNI_VERSION_1_6;
    args.name = name;
    args.group = NULL;
    if (g_JavaVM->AttachCurrentThread(&env, &args) != JNI_OK) {
        return;
    }

    if (g_callback != nullptr) {
        jclass doubleClass = env->FindClass("java/lang/Double");
        jmethodID doubleConstructor = env->GetMethodID(doubleClass, "<init>", "(D)V");
        jobjectArray doubleArray = env->NewObjectArray(data.size(), doubleClass, nullptr);
        for (size_t i = 0; i < data.size(); ++i) {
            jdouble value = data[i];
            jobject doubleObject = env->NewObject(doubleClass, doubleConstructor, value);
            env->SetObjectArrayElement(doubleArray, i, doubleObject);
        }

        jclass callbackClass = env->GetObjectClass(g_callback);
        jmethodID callbackMethod = env->GetMethodID(callbackClass, "invoke",
                                                    "(Ljava/lang/Object;)Ljava/lang/Object;");
        env->CallObjectMethod(g_callback, callbackMethod, doubleArray);

        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }

    if (g_JavaVM->DetachCurrentThread() != JNI_OK) {
        return;
    }
}

void SignalProc::sendToJavaOp(const std::vector<int> &data) {
    JNIEnv *env = nullptr;
    JavaVMAttachArgs args;
    char name[] = "Int Response Callback Thread";
    args.version = JNI_VERSION_1_6;
    args.name = name;
    args.group = NULL;
    if (g_JavaVM->AttachCurrentThread(&env, &args) != JNI_OK) {
        return;
    }

    if (g_callback_op != nullptr) {
        // 创建基本类型的int数组而不是Integer对象数组
        jintArray intArray = env->NewIntArray(data.size());
        // 将std::vector<int>的数据复制到intArray
        env->SetIntArrayRegion(intArray, 0, data.size(), &data[0]);

        jclass callbackClass = env->GetObjectClass(g_callback_op);
        jmethodID callbackMethod = env->GetMethodID(callbackClass, "invoke",
                                                    "(Ljava/lang/Object;)Ljava/lang/Object;"); // 注意方法签名的变化
        env->CallObjectMethod(g_callback_op, callbackMethod, intArray); // 使用CallVoidMethod而不是CallObjectMethod

        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }

    if (g_JavaVM->DetachCurrentThread() != JNI_OK) {
        return;
    }
}


jobject g_callback = nullptr;
jobject g_callback_op = nullptr;
JavaVM* g_JavaVM = nullptr;


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_JavaVM = vm;
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_GetDataKt_setCppCallback(JNIEnv *env, jclass clazz, jobject callback) {
    if (g_callback != nullptr) {
        env->DeleteGlobalRef(g_callback);
    }
    g_callback = env->NewGlobalRef(callback);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_GetDataKt_clearCppCallback(JNIEnv *env, jclass clazz) {
    if (g_callback != nullptr) {
        env->DeleteGlobalRef(g_callback);
        g_callback = nullptr;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_GetDataKt_setCppCallbackOp(JNIEnv *env, jclass clazz, jobject callback) {
    if (g_callback_op != nullptr) {
        env->DeleteGlobalRef(g_callback_op);
    }
    g_callback_op = env->NewGlobalRef(callback);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_GetDataKt_clearCppCallbackOp(JNIEnv *env, jclass clazz) {
    if (g_callback_op != nullptr) {
        env->DeleteGlobalRef(g_callback_op);
        g_callback_op = nullptr;
    }
}