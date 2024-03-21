//
// Created by CUI on 3/21/24.
//

#ifndef A1P_SIGNALPROC_H
#define A1P_SIGNALPROC_H

#include "GlobalData.h"
#include <future>
#include <vector>
#include <algorithm>
#include <jni.h>

extern jobject g_callback;
extern JavaVM *g_JavaVM;

class SignalProc {
private:
    std::vector<double> buffer;
    std::vector<double> buffer_filtered;
    const int peak_interval = ZC_LENGTH - 5;
    const int output_size = ZC_LENGTH;

    void sendToJava(const std::vector<double> &data);

public:
    SignalProc() {}

    void processStream(const double value, const double move_value);


};


#endif //A1P_SIGNALPROC_H
