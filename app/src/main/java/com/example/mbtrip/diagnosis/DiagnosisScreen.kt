package com.example.mbtrip.diagnosis

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun DiagnosisScreen(
    modifier: Modifier = Modifier,
    displayName: String = "나",
    onContinue: (() -> Unit)? = null,
    onResult: ((DiagnosisResult) -> Unit)? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    saved: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context -> DiagnosisView(context) },
            update = { view ->
                view.configure(displayName, onContinue, onResult)
                view.setSaveState(isSaving, saveError)
            },
        )
        when {
            isSaving -> Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp)); Text("진단 결과를 저장하고 있어요.")
            }
            saveError != null -> Column(Modifier.padding(16.dp)) {
                Text(saveError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("저장 다시 시도") }
            }
            saved -> Text("진단 결과를 저장했어요.", Modifier.padding(16.dp))
        }
    }
}
