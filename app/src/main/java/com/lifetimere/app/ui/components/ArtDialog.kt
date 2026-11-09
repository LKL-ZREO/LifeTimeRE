package com.lifetimere.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifetimere.app.R
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelParchment

val LocalArtButton = staticCompositionLocalOf { R.drawable.world_wood_button }

@Composable
fun ArtDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    @DrawableRes art: Int = R.drawable.world_fire_panel,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        ArtPanel(art, title, text, confirmButton, dismissButton)
    }
}

@Composable
internal fun ArtPanel(
    @DrawableRes art: Int,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 270.dp, max = 620.dp)) {
        Image(painterResource(art), contentDescription = null,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        Column(Modifier.fillMaxWidth().padding(start = 34.dp, end = 34.dp, top = 72.dp, bottom = 46.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(color = PixelGold)) {
                title()
            }
            Box(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = PixelParchment)) {
                    text()
                }
            }
            CompositionLocalProvider(LocalArtButton provides
                if (art == R.drawable.world_atlas_panel) R.drawable.world_paper_button
                else R.drawable.world_wood_button) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}


@Composable
fun ArtTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes art: Int? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    colors: ButtonColors? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val buttonArt = art ?: LocalArtButton.current
    Box(
        modifier.widthIn(min = 80.dp).heightIn(min = 46.dp)
            .alpha(if (enabled) 1f else .45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(buttonArt), contentDescription = null,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        CompositionLocalProvider(LocalContentColor provides
            if (buttonArt == R.drawable.world_paper_button) Color(0xFF273347) else PixelParchment) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, content = content)
        }
    }
}

