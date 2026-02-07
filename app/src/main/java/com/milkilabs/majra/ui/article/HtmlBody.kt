package com.milkilabs.majra.ui.article

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlBody(
    html: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val fontSize = MaterialTheme.typography.bodyMedium.fontSize
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextColor(textColor.toArgb())
                setLineSpacing(0f, 1.2f)
                textSize = with(density) { fontSize.toPx() } / density.density
            }
        },
        update = { view ->
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        },
    )
}
