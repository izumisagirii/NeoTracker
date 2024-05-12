//
// Created by CUI on 2/26/24.
//

#include <stdint.h>
#include "SeqGenerate.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>


AAssetManager *mgr = nullptr;

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
    this->window = std::deque<Complex>(ZC_LENGTH);
    this->window_cor = std::deque<Complex>(ZC_LENGTH);
}

double SeqGenerate::getNew() {
    double amp = (*pArray * *pCarry).real;
    pArray++;
    pCarry++;
    if (pArray == array.end()) {
        pArray = array.begin();
    }
    if (pCarry == multiplyArray.end()) {
        pCarry = multiplyArray.begin();
    }
    return amp;
//    return pArray->real;
}

int16_t SeqGenerate::getNewInt16() {
    double value = getNew();
//    double mappedValue = (value - (-1)) / (1 - (-1));
    double scaledValue = value * 32768.0;
    return static_cast<int16_t>(std::round(scaledValue));
}


std::array<Complex, ZC_LENGTH> SeqGenerate::jsonToComplexArray(const std::string &json) {
    std::array<Complex, ZC_LENGTH> _array;
    Json::Value json_val;
    Json::Reader json_reader;
    bool success = json_reader.parse(json, json_val);
    if (!success) {
        raise(SIGFPE);
    }
    assert(json_val.size() == ZC_LENGTH);
    int index = 0;
    for (const auto &elem: json_val) {
        double real = elem["real"].asDouble();
        double imag = elem["imag"].asDouble();
        _array[index++] = Complex(real, imag);
    }
    return _array;
}

std::string SeqGenerate::readJsonFromAssets(std::string fileName) {
    AAsset *asset = AAssetManager_open(mgr, fileName.c_str(), AASSET_MODE_BUFFER);
    if (asset) {
        size_t dataFileSize = AAsset_getLength(asset);
        char *buffer = (char *) malloc(dataFileSize);
        memset(buffer, 0x00, dataFileSize);
        AAsset_read(asset, buffer, dataFileSize);
        AAsset_close(asset);
//        __android_log_print(ANDROID_LOG_INFO, "JSON", "%s", buffer);
        std::string jsonContent(buffer, dataFileSize);
        free(buffer);
        return jsonContent;
    }
    return "";
}

void SeqGenerate::generateCarrierArray(int fc) {
    for (int i = 0; i < 48000; i++) {
        double radians = 2 * i * M_PI * (double(fc) / 48000.0);
        double cosValue = cos(radians);
        double sinValue = sin(radians);
        multiplyArray[i] = Complex(cosValue, sinValue);
    }
}

void SeqGenerate::init() {
    std::string json = readJsonFromAssets(SIGNAL_NAME);
    array = jsonToComplexArray(json);
//    __android_log_print(ANDROID_LOG_INFO, "ARRAY", "%s", (array[0].toString() + array[1].toString() + array[2].toString()).c_str());
    generateCarrierArray(carryRate);
    pArray = this->array.begin();
    pCarry = this->multiplyArray.begin();
    pDeMod = this->multiplyArray.begin();
    isInit = true;
}

Complex SeqGenerate::deModNew(double input) {
//    input = 1;
    auto res = *pDeMod * input;
    pDeMod++;
    if (pDeMod == multiplyArray.end()) {
        pDeMod = multiplyArray.begin();
    }

    return this->correlation( res );
//    return res;
//    return {res.real,res.real};
//    return {input,0};
}

//Complex SeqGenerate::filteredNew(Complex &input) {
//    window.push_front(input);
//    window.pop_back();
//    Complex _output;
//    for (int it = 0; it < FILTER_PARAM.size(); it++) {
//        _output = _output + window[it * ZC_LENGTH] * FILTER_PARAM[it];
//    }
//    return _output;
//}

Complex SeqGenerate::filteredNew(Complex &input) {
//    window.push_back(input);

    Complex _w = window.front();
    Complex _y = input - _w*(1 - 0.7);
    window.push_back(_y + _w);
    window.pop_front();

//    Complex _output;
//    for (int it = 0; it < FILTER_PARAM.size(); it++) {
//        _output = _output + window[it * ZC_LENGTH] * FILTER_PARAM[it];
//    }

    return _y;
}

Complex SeqGenerate::correlation(Complex input) {
    window_cor.push_back(input);
    window_cor.pop_front();
    Complex _output(0,0);
    for (int it = 0; it < ZC_LENGTH; it++) {
        _output = _output + (array[it] * window_cor[it]);
    }
    return _output;
}





