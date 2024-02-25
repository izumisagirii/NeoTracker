package com.example.a1p

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1p.ui.theme.A1pTheme


class GlobalData {
    companion object {
        lateinit var activity : ComponentActivity
        val signalRec = SignalRec()
        val buttonColor = mutableStateOf(Color.Green)
        val buttonText = mutableStateOf("Start Recording")
        var ifRecording = false
        val timeCounter = TimeCounter()
        val time = mutableStateOf("00:00:00")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalData.activity = this
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hello $name!",
            fontSize = 30.sp,
            modifier = Modifier.padding(24.dp),
        )
        Spacer(modifier = Modifier.size(50.dp))
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
}

fun onClick() {
    if (GlobalData.ifRecording) {
        GlobalData.signalRec.stop()
        GlobalData.timeCounter.reset()
        GlobalData.buttonColor.value = Color.Green
        GlobalData.buttonText.value = "Start Recording"
        GlobalData.ifRecording = false
    } else {
        if(!GlobalData.signalRec.start()){
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