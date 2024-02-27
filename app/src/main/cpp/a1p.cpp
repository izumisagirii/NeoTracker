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
}

oboe::AudioStream *stream;
SeqGenerate seqGenerate = SeqGenerate(CARRIER_RATE);

class AudioCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
        if(!seqGenerate.isInit){
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

auto audioCb = AudioCallback();

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