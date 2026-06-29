package jv.watersms.enterprises

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import jv.watersms.enterprises.navigation.AppNavigation
import jv.watersms.enterprises.ui.components.GradientBackground
import jv.watersms.enterprises.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GradientBackground {
                    WaterSmsMainScreen()
                }
            }
        }
    }
}

@Composable
fun WaterSmsMainScreen() {
    AppNavigation()
}
