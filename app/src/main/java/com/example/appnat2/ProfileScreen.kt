package com.example.appnat2

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appnat2.databinding.ScreenProfileBinding

@Composable
fun ProfileScreen() {
    AndroidView(
        factory = { context ->
            ScreenProfileBinding.inflate(android.view.LayoutInflater.from(context)).root
        }
    )
}
