package com.example.a1p

import java.util.concurrent.atomic.AtomicIntegerArray

external fun setCppCallback(callback: (Array<Double>) -> Unit)
external fun clearCppCallback()

external fun setCppCallbackOp(callback: (IntArray) -> Unit)
external fun clearCppCallbackOp()