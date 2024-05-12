#ifndef WAV_H
#define WAV_H


#include <android/log.h>
#include <iostream>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <queue>
#include <functional>

#define WAV_HEADER_SIZE 44
#define PCM_FORMAT 1
#define SAMPLE_SIZE 2
#define CHANNEL_COUNT 1
#define SAMPLE_RATE 48000

struct WavHeader {
    char riffId[4];
    int32_t riffSize;
    char waveId[4];
    char fmtId[4];
    int32_t fmtSize;
    int16_t format;
    int16_t channels;
    int32_t sampleRate;
    int32_t byteRate;
    int16_t blockAlign;
    int16_t bitsPerSample;
    char dataId[4];
    int32_t dataSize;
};


inline void initWavHeader(WavHeader* wavHeader) {
    memcpy(wavHeader->riffId, "RIFF", 4);
    wavHeader->riffSize = 0; // 先设为 0，后面再更新
    memcpy(wavHeader->waveId, "WAVE", 4);
    memcpy(wavHeader->fmtId, "fmt ", 4);
    wavHeader->fmtSize = 16;
    wavHeader->format = PCM_FORMAT;
    wavHeader->channels = CHANNEL_COUNT;
    wavHeader->sampleRate = SAMPLE_RATE;
    wavHeader->byteRate = SAMPLE_RATE * CHANNEL_COUNT * SAMPLE_SIZE;
    wavHeader->blockAlign = CHANNEL_COUNT * SAMPLE_SIZE;
    wavHeader->bitsPerSample = SAMPLE_SIZE * 8;
    memcpy(wavHeader->dataId, "data", 4);
    wavHeader->dataSize = 0;
}




inline void openWavFile(const char *fileName, FILE* &wavFile, WavHeader* wavHeader) {
    wavFile = fopen(fileName, "wb");
    fwrite(wavHeader, 1, WAV_HEADER_SIZE, wavFile);
}


inline void closeWavFile( FILE* &wavFile, WavHeader* wavHeader) {
    if (wavFile == nullptr) {
        return;
    }
    int32_t fileSize = ftell(wavFile);
    wavHeader->riffSize = fileSize - 8;
    wavHeader->dataSize = fileSize - WAV_HEADER_SIZE;
    fseek(wavFile, 0, SEEK_SET);
    fwrite(wavHeader, 1, WAV_HEADER_SIZE, wavFile);
    fclose(wavFile);
    wavFile = nullptr;
}

class SingleThreadExecutor {
public:
    SingleThreadExecutor() : done(false), worker(&SingleThreadExecutor::work, this) {}

    ~SingleThreadExecutor() {
        {
            std::unique_lock<std::mutex> lock(m);
            done = true;
        }
        cv.notify_one();
        worker.join();
    }

    void submit(std::function<void()> task) {
        std::unique_lock<std::mutex> lock(m);
        tasks.push(task);
        cv.notify_one();
    }

private:
    void work() {
        while (true) {
            std::function<void()> task;
            {
                std::unique_lock<std::mutex> lock(m);
                cv.wait(lock, [this] { return done || !tasks.empty(); });
                if (done && tasks.empty()) break;
                task = tasks.front();
                tasks.pop();
            }
            task();
        }
    }

    std::thread worker;
    std::mutex m;
    std::condition_variable cv;
    std::queue<std::function<void()>> tasks;
    bool done;
};

#endif