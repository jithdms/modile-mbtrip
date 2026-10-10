package com.example.mbtrip.diagnosis

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView

/** Hosts the XML questionnaire inside the app's existing Compose navigation. */
@Composable
fun DiagnosisScreen(
    modifier: Modifier = Modifier,
    displayName: String = "나",
    companions: List<CompanionMember> = emptyList(),
    onContinue: (() -> Unit)? = null,
    onResult: ((DiagnosisResult) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context -> DiagnosisView(context) },
        update = { view -> view.configure(displayName, companions, onContinue, onResult) }
    )
}
