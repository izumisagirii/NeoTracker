//
// Created by CUI on 2/26/24.
//

#include <stdint.h>
#include "SeqGenerate.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

AAssetManager* mgr = NULL;  // 获取 AAssetManager
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_TestLayer_00024Companion_SetAssetManager(JNIEnv *env, jobject thiz,
                                                              jobject asset_manager) {
    mgr = AAssetManager_fromJava(env, asset_manager);
}


//
//void registerNative(JavaVM* vm) {
//    JNIEnv* env = nullptr;
//    vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
//    jclass customLayerClass = env->FindClass("cn/layers/TestLayer");
//    JNINativeMethod methods[] = {
//            {"SetAAssetManager", "(Landroid/content/res/AssetManager;)V", reinterpret_cast<void*>(&nativeSetAAssetManager)},
//    };
//    if (env->RegisterNatives(customLayerClass, methods, sizeof(methods) / sizeof(JNINativeMethod)) < 0) {
//        env->ExceptionDescribe();
//    }
//}


SeqGenerate::SeqGenerate(int fc) {
    carryRate = fc;
    isInit = false;
}

double SeqGenerate::getNew() {
    double amp = (array[pArray] * multiplyArray[pCarry]).real;
    pArray++;
    pCarry++;
    if (pArray == boundArray) {
        pArray = 0;
    }
    if (pCarry == 48000) {
        pCarry = 0;
    }
    return amp;
}

int16_t SeqGenerate::getNewInt16() {
    double value = getNew();
    double mappedValue = (value - (-1)) / (1 - (-1));
    double scaledValue = mappedValue * 65535;
    return round(scaledValue);
}

std::vector<Complex> SeqGenerate::jsonToComplexArray(std::string json) {
    std::vector<Complex> _array;
    Json::Value json_val;
    Json::Reader json_reader;
    bool success = json_reader.parse(json, json_val);
    if (!success) {
        raise(SIGFPE);
    }
    for (const auto& elem : json_val) {
        double real = elem["real"].asDouble();
        double imag = elem["imag"].asDouble();
        _array.push_back(Complex(real,imag));
    }
    return _array;
}

std::string SeqGenerate::readJsonFromAssets(std::string fileName) {
    AAsset* asset = AAssetManager_open(mgr, fileName.c_str(), AASSET_MODE_BUFFER);
    if (asset) {
        size_t dataFileSize = AAsset_getLength(asset);
        char* buffer = (char*)malloc(dataFileSize);
        memset(buffer, 0x00, dataFileSize);
        AAsset_read(asset, buffer, dataFileSize);
        AAsset_close(asset);
        std::string jsonContent(buffer, dataFileSize);
        free(buffer);
        return jsonContent;
    }
    return "";
}

void SeqGenerate::generateCarrierArray(int fc) {
    multiplyArray = std::vector<Complex>(48000);
    for (int i = 0; i < 48000; i++) {
        double radians = 2 * i * M_PI * (double (fc) / 48000.0);
        double cosValue = cos(radians);
        double sinValue = sin(radians);
        multiplyArray[i] = Complex(cosValue,sinValue);
    }
}

void SeqGenerate::init() {
    std::string json = readJsonFromAssets(SIGNAL_NAME);
    array = jsonToComplexArray(json);
    boundArray = array.size();
    generateCarrierArray(carryRate);
    pArray = 0;
    pCarry = 0;
    isInit = true;
}

