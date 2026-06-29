package jv.watersms.enterprises.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jv.watersms.enterprises.ui.theme.DarkBlueBg
import jv.watersms.enterprises.ui.theme.DarkPurpleBg
import jv.watersms.enterprises.ui.theme.DarkGreyBg
import jv.watersms.enterprises.ui.theme.MyApplicationTheme

@Composable
fun GradientBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBlueBg,
                        DarkPurpleBg,
                        DarkGreyBg
                    )
                )
            )
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun GradientBackgroundPreview() {
    MyApplicationTheme {
        GradientBackground {
            WaterSmsLogo(size = 80.dp)
        }
    }
}
