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
#include "SignalProc.h"

extern "C" {
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_startPlayback(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopPlayback(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_startRecord(JNIEnv *env, jobject thiz, jint micId);

JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopRecord(JNIEnv *env, jobject thiz);

}

oboe::AudioStream *stream;
oboe::AudioStream *stream_in;
SeqGenerate seqGenerate = SeqGenerate(CARRIER_RATE);
SignalProc signalProc = SignalProc();
WavHeader *wavHeader;
FILE *wavFile;


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
public:
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
        int32_t bytesPerFrame = audioStream->getChannelCount() * audioStream->getBytesPerSample();
        int32_t totalBytes = numFrames * bytesPerFrame;
        if(wavFile != nullptr) {
            fwrite(audioData, 1, totalBytes, wavFile);
        }

        int16_t* samples = static_cast<int16_t*>(audioData);
        for (int i = 0; i < numFrames; ++i) {
//            printf("Sample %d: %d\n", i, samples[i]);
            auto _point = seqGenerate.deModNew(samples[i]);
            auto _point_filtered = seqGenerate.filteredNew(_point);
            signalProc.processStream(_point.magnitude(),_point_filtered.magnitude());
        }
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
Java_com_example_a1p_SignalRec_startRecord(JNIEnv *env, jobject thiz, jint micId) {
    wavHeader = new WavHeader;
    initWavHeader(wavHeader);
    jclass cls = env->GetObjectClass(thiz);
    jmethodID mid = env->GetMethodID(cls, "getCacheDir", "()Ljava/io/File;");
    jobject file = env->CallObjectMethod(thiz, mid);
    jclass file_cls = env->GetObjectClass(file);
    jmethodID file_mid = env->GetMethodID(file_cls, "getAbsolutePath", "()Ljava/lang/String;");
    jstring path = (jstring) env->CallObjectMethod(file, file_mid);
    const char *cache_dir = env->GetStringUTFChars(path, JNI_FALSE);
    char *file_name = "output.wav";
    char *file_path = (char *) malloc(strlen(cache_dir) + strlen(file_name) + 2);
    strcpy(file_path, cache_dir);
    strcat(file_path, "/");
    strcat(file_path, file_name);
    env->ReleaseStringUTFChars(path, cache_dir);
    openWavFile(file_path, wavFile, wavHeader);
    if(wavFile == nullptr){
        __android_log_print(ANDROID_LOG_INFO, "FILE", "%d", errno);
    }


    oboe::AudioStreamBuilder builder = oboe::AudioStreamBuilder();
    builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setDeviceId(micId)
            ->setSampleRate(48000)
            ->setCallback(&recordCb);

    oboe::Result result = builder.openStream(&stream_in);
    if (result == oboe::Result::OK && stream_in) {
        stream_in->requestStart();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_a1p_SignalRec_stopRecord(JNIEnv *env, jobject thiz) {
    if (stream_in) {
        stream_in->close();
        stream_in = nullptr;
    }
    closeWavFile(wavFile, wavHeader);
    delete wavHeader;
}





