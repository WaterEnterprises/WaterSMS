package jv.watersms.enterprises.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jv.watersms.enterprises.ui.theme.MyApplicationTheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
            )
        )
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun GlassCardPreview() {
    MyApplicationTheme {
        GlassCard(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Glass Card Content",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
