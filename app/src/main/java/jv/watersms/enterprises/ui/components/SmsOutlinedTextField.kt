package jv.watersms.enterprises.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jv.watersms.enterprises.ui.theme.MyApplicationTheme

@Composable
fun SmsOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    minLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder) }) else null,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        singleLine = singleLine,
        minLines = minLines,
        leadingIcon = leadingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun SmsOutlinedTextFieldPreview() {
    MyApplicationTheme {
        var text by remember { mutableStateOf("") }
        SmsOutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = "Test Field",
            placeholder = "Enter text...",
            modifier = Modifier.padding(16.dp)
        )
    }
}
