//
// Created by CUI on 2/26/24.
//

#ifndef A1P_GLOBALDATA_H
#define A1P_GLOBALDATA_H
#include <string>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "wav.h"
#include <array>
#include <jni.h>

constexpr std::array<double, 4> FILTER_PARAM = {1, -3, 3, -1};
constexpr auto SIGNAL_NAME = "seq.json";
constexpr auto CARRIER_RATE = 17800;
constexpr auto ZC_LENGTH = 384;


#endif //A1P_GLOBALDATA_H
