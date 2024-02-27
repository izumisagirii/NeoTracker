// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("a1p");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("a1p")
//      }
//    }
#include <jni.h>
#include <oboe/Oboe.h>
#include <math.h>
#include "SeqGenerate.h"
#include "GlobalData.h"

extern "C" {
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_startPlayback(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopPlayback(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_startRecord(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopRecord(JNIEnv *env, jobject thiz);
}

oboe::AudioStream *stream;
oboe::AudioStream *stream_in;
SeqGenerate seqGenerate = SeqGenerate(CARRIER_RATE);
std::vector<char> audioDataArray;
JNIEnv *globalEnv;

class AudioCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
        if (!seqGenerate.isInit) {
            seqGenerate.init();
        }
        int16_t *outputBuffer = static_cast<int16_t *>(audioData);
        for (int i = 0; i < numFrames; ++i) {
//            outputBuffer[i] = static_cast<int16_t>(65535 * (sin(2 * M_PI * 0.05 * i) + 1)); //Test sin signal
            outputBuffer[i] = seqGenerate.getNewInt16();
        }
        return oboe::DataCallbackResult::Continue;
    }
};

class RecordCallback : public oboe::AudioStreamCallback {
private:
//    // Swap endian (little to big) or (big to little) for int16_t
//    int16_t swap_int16(int16_t value) {
//        return (value << 8) | (value >> 8);
//    }
public:
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
        auto *inputData = reinterpret_cast<char *>(audioData);
        audioDataArray.insert(audioDataArray.end(), inputData, inputData + numFrames * sizeof(int16_t));
//        for (short &i :audioDataArray){
//            i = swap_int16(i);
//        }
        return oboe::DataCallbackResult::Continue;
    }
};


auto audioCb = AudioCallback();
auto recordCb = RecordCallback();

extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_startPlayback(JNIEnv *env, jobject thiz) {
    oboe::AudioStreamBuilder builder;
    builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setFormat(oboe::AudioFormat::I16)
            ->setSampleRate(48000)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setCallback(&audioCb);

    oboe::Result result = builder.openStream(&stream);
    if (result == oboe::Result::OK && stream) {
        stream->requestStart();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopPlayback(JNIEnv *env, jobject thiz) {
    if (stream) {
        stream->close();
        stream = nullptr;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_startRecord(JNIEnv *env, jobject thiz) {
    oboe::AudioStreamBuilder builder = oboe::AudioStreamBuilder();
    builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(48000)
            ->setCallback(&recordCb);

    oboe::Result result = builder.openStream(&stream_in);
    if (result == oboe::Result::OK && stream_in) {
        audioDataArray.clear();
        stream_in->requestStart();
    }
    globalEnv = env;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopRecord(JNIEnv *env, jobject thiz) {
    if (stream_in) {
        stream_in->close();
        stream_in = nullptr;
    }
    jbyteArray audioDataJavaArray = env->NewByteArray(audioDataArray.size());
    jboolean isCopy;
    jbyte *audioData = env->GetByteArrayElements(audioDataJavaArray, &isCopy);
    memcpy(audioData, audioDataArray.data(), audioDataArray.size());
    env->ReleaseByteArrayElements(audioDataJavaArray, audioData, 0);
    jclass cls = env->GetObjectClass(thiz);
    jmethodID mid = env->GetMethodID(cls, "processAudioData", "([B)V"); // 注意修改方法签名，使用 [B 而不是 [I
    env->CallVoidMethod(thiz, mid, audioDataJavaArray);
    env->DeleteLocalRef(audioDataJavaArray);
}





