package com.example.appnat2

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appnat2.databinding.ScreenFavoritesBinding

@Composable
fun FavoritesScreen() {
    AndroidView(
        factory = { context ->
            ScreenFavoritesBinding.inflate(android.view.LayoutInflater.from(context)).root
        }
    )
}
