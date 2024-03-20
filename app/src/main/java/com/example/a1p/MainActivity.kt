package com.example.a1p

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1p.ui.theme.A1pTheme
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.chart.zoom.rememberVicoZoomState
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.BaseAxis
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.lineSeries
import com.patrykandpatrick.vico.core.zoom.Zoom
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class GlobalData {
    companion object {
        lateinit var activity: ComponentActivity
        val signalRec = SignalRec()
        val buttonColor = mutableStateOf(Color.Green)
        val buttonText = mutableStateOf("Start Recording")
        var ifRecording = false
        val timeCounter = TimeCounter()
        val time = mutableStateOf("00:00:00")
        var signalFName = "seq.json"
        val modelProducer = CartesianChartModelProducer.build()

        //        val data = mutableStateOf(List(384) { (0..10).random() })
        val data = mutableStateOf(List(384) { 0 })

//        val seqGenerator = SeqGenerate(17800)
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        init {
            System.loadLibrary("a1p")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalData.activity = this
        val it = TestLayer()
        it.setAssetManager(this.assets)
        setContent {
            A1pTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("NeoTracker")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Surface(color = Color.DarkGray) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hello $name!",
                fontSize = 30.sp,
                modifier = Modifier.padding(24.dp),
            )
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                PlotView()
            }
            Text(
                text = GlobalData.time.value,
                fontSize = 25.sp,
                modifier = Modifier.padding(24.dp),
            )
            Button(
                onClick = { onClick() },
                colors = ButtonDefaults.buttonColors(
//                contentColor = Color.Green,
                    containerColor = GlobalData.buttonColor.value
                )
            ) {
                Text(
                    text = GlobalData.buttonText.value,
                    fontSize = 20.sp,
                    color = Color.Black,
                    fontStyle = FontStyle.Normal
                )
            }
        }
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                PlotView()
            }
        }
    }
}

@Composable
fun PlotView() {
    Column(modifier = Modifier.padding(5.dp, 20.dp)) {
        LaunchedEffect(Unit) {
//            GlobalData.data.value = GlobalData.data.value.map { it + (-1..1).random() }
            GlobalData.modelProducer.tryRunTransaction {
                lineSeries {
                    series(GlobalData.data.value)
                }
            }
        }
        CartesianChartHost(
            rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(
                    guideline = null,
                    itemPlacer = remember { AxisItemPlacer.Horizontal.default(spacing = 50) }
                ),
            ),
            GlobalData.modelProducer,
            zoomState = rememberVicoZoomState(
                zoomEnabled = false,
                initialZoom = remember { Zoom.Content }
            ),
            runInitialAnimation = false,
            diffAnimationSpec = null,
        )
    }
}

fun onClick() {
    if (GlobalData.ifRecording) {
        GlobalData.signalRec.stop()
        GlobalData.timeCounter.reset()
        GlobalData.buttonColor.value = Color.Green
        GlobalData.buttonText.value = "Start Recording"
        GlobalData.ifRecording = false
    } else {
        if (!GlobalData.signalRec.start()) {
            return
        }
        GlobalData.timeCounter.start()
        GlobalData.buttonColor.value = Color.Red
        GlobalData.buttonText.value = "!Stop Recording"
        GlobalData.ifRecording = true
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    A1pTheme {
        Greeting("NeoTracker")
    }
}
//@Composable
//fun getActivity(): ComponentActivity? {
//    val activity = LocalContext.current
//    fun Context.getActivity(): ComponentActivity? = when (this) {
//        is ComponentActivity -> this
//        is ContextWrapper -> baseContext.getActivity()
//        else -> null
//    }
//    return  activity.getActivity()
//}