package jv.watersms.enterprises.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jv.watersms.enterprises.ui.theme.MyApplicationTheme

@Composable
fun WaterSmsLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = (size.value * 0.08f).dp.toPx()
            val glowWidth = (size.value * 0.16f).dp.toPx()

            val colors = listOf(
                Color(0xFF3B82F6),
                Color(0xFFEC4899),
                Color(0xFFD946EF),
                Color(0xFF3B82F6)
            )
            val sweepGradient = Brush.sweepGradient(colors = colors)

            drawCircle(
                brush = sweepGradient,
                radius = (this.size.width - glowWidth) / 2f,
                style = Stroke(width = glowWidth, cap = StrokeCap.Round),
                alpha = 0.3f
            )

            drawCircle(
                brush = sweepGradient,
                radius = (this.size.width - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                alpha = 1.0f
            )
        }

        Text(
            text = "W",
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun WaterSmsLogoPreview() {
    MyApplicationTheme {
        WaterSmsLogo(size = 80.dp)
    }
}
