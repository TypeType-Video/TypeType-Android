package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
internal fun TvTextPrompt(
    title: String,
    initialValue: String = "",
    allowBlank: Boolean = false,
    password: Boolean = false,
    trimValue: Boolean = true,
    actionLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val inputFocus = remember { FocusRequester() }
    val actionFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        val submittedValue = if (trimValue) value.trim() else value
        if (allowBlank || submittedValue.isNotEmpty()) {
            keyboard?.hide()
            onSubmit(submittedValue)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(680.dp)) {
                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth().focusRequester(inputFocus)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                    keyboard?.hide()
                                    actionFocus.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onDismiss) { Text("Cancel") }
                        Button(
                            onClick = submit,
                            enabled = allowBlank || value.isNotBlank(),
                            modifier = Modifier.focusRequester(actionFocus),
                        ) { Text(actionLabel) }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        inputFocus.requestFocus()
        keyboard?.show()
    }
}

@Composable
internal fun TvConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(620.dp)) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = onConfirm) { Text(confirmLabel) }
                }
            }
        }
    }
}
