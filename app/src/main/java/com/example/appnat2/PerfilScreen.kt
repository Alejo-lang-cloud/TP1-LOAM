package com.example.appnat2

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PerfilScreen() {
    AndroidView(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.screen_perfil, null)
        }
    )
}
