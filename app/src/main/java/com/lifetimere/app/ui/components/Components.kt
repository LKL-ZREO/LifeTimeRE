package com.lifetimere.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifetimere.app.domain.CharacterAttribute
import com.lifetimere.app.ui.navigation.AppDestination
import com.lifetimere.app.ui.theme.PixelAmber
import com.lifetimere.app.ui.theme.PixelCoral
import com.lifetimere.app.ui.theme.PixelForest
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelInk
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelNight
import com.lifetimere.app.ui.theme.PixelOutline
import com.lifetimere.app.ui.theme.PixelPanel
import com.lifetimere.app.ui.theme.PixelPanelRaised
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal
import com.lifetimere.app.ui.theme.PixelViolet

@Composable
fun Page(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(PixelNight)) {
        PixelPattern(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EchoRune(18.dp)
                Text(
                    "LIFETIME RE · ADVENTURE LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelGold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                action()
            }
            PixelDivider()
            content()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PixelPattern(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val step = 24.dp.toPx()
        val dot = 1.dp.toPx()
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawRect(
                    color = PixelPanelRaised.copy(alpha = 0.28f),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(dot, dot),
                )
                x += step
            }
            y += step
        }
    }
}

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(PixelInk),
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp),
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(2.dp, PixelOutline),
            colors = CardDefaults.cardColors(containerColor = PixelPanel),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
fun EchoCard(content: @Composable ColumnScope.() -> Unit) = PixelPanel(content = content)

@Composable
fun PixelTag(text: String, color: Color = PixelGold) {
    Text(
        text = text,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(1.dp))
            .border(1.dp, color.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
fun PixelDivider(color: Color = PixelOutline) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(18) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(if (index % 3 == 0) 3.dp else 1.dp)
                    .background(color.copy(alpha = if (index % 2 == 0) 0.9f else 0.45f)),
            )
        }
    }
}

@Composable
fun PixelMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PixelGold,
    segments: Int = 10,
) {
    val filled = (progress.coerceIn(0f, 1f) * segments).toInt()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(9.dp)
                    .background(if (index < filled) color else PixelInk)
                    .border(1.dp, if (index < filled) color else PixelOutline),
            )
        }
    }
}

@Composable
fun SecondaryText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun EchoRune(size: Dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / 8f
        fun block(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color = PixelGold) {
            drawRect(color, Offset(x * u, y * u), androidx.compose.ui.geometry.Size(w * u, h * u))
        }
        block(3, 0, 2, 1); block(2, 1, 4, 1)
        block(1, 2, 2, 1); block(5, 2, 2, 1)
        block(0, 3, 2, 2); block(6, 3, 2, 2)
        block(1, 5, 2, 1); block(5, 5, 2, 1)
        block(2, 6, 4, 1); block(3, 7, 2, 1)
        block(3, 3, 2, 2, PixelTeal)
    }
}

fun attributeColor(attribute: CharacterAttribute): Color = when (attribute) {
    CharacterAttribute.STRENGTH -> PixelCoral
    CharacterAttribute.ENDURANCE -> PixelForest
    CharacterAttribute.KNOWLEDGE -> PixelTeal
    CharacterAttribute.SKILL -> PixelAmber
    CharacterAttribute.RESILIENCE -> PixelViolet
}

@Composable
fun AttributeIcon(attribute: CharacterAttribute, modifier: Modifier = Modifier) {
    val color = attributeColor(attribute)
    Canvas(modifier.size(38.dp).background(PixelInk).border(2.dp, color)) {
        val u = size.minDimension / 12f
        fun block(x: Int, y: Int, w: Int = 1, h: Int = 1, c: Color = color) {
            drawRect(c, Offset(x * u, y * u), androidx.compose.ui.geometry.Size(w * u, h * u))
        }
        when (attribute) {
            CharacterAttribute.STRENGTH -> {
                block(2, 2, 3, 2); block(7, 2, 3, 2); block(4, 4, 4, 2)
                block(5, 6, 2, 4); block(3, 8, 6, 2)
            }
            CharacterAttribute.ENDURANCE -> {
                block(2, 5, 2, 3); block(8, 5, 2, 3); block(4, 3, 4, 7)
                block(1, 6, 10, 1, PixelParchment)
            }
            CharacterAttribute.KNOWLEDGE -> {
                block(2, 2, 4, 8); block(6, 2, 4, 8)
                block(5, 3, 2, 7, PixelParchment); block(3, 4, 2, 1, PixelInk); block(7, 4, 2, 1, PixelInk)
            }
            CharacterAttribute.SKILL -> {
                block(5, 1, 2, 7); block(3, 3, 6, 2)
                block(2, 7, 3, 3); block(7, 7, 3, 3)
            }
            CharacterAttribute.RESILIENCE -> {
                block(4, 1, 4, 2); block(2, 3, 8, 4); block(3, 7, 6, 2); block(5, 9, 2, 2)
                block(5, 4, 2, 3, PixelParchment)
            }
        }
    }
}

@Composable
fun PixelNavIcon(destination: AppDestination, selected: Boolean) {
    val color = if (selected) PixelGold else PixelMuted
    Canvas(Modifier.size(24.dp)) {
        val u = size.minDimension / 12f
        fun block(x: Int, y: Int, w: Int = 1, h: Int = 1) {
            drawRect(color, Offset(x * u, y * u), androidx.compose.ui.geometry.Size(w * u, h * u))
        }
        when (destination) {
            AppDestination.TODAY -> {
                block(2, 1, 8, 10); block(1, 3, 10, 7)
                drawRect(PixelInk, Offset(3 * u, 5 * u), androidx.compose.ui.geometry.Size(6 * u, u))
                drawRect(PixelInk, Offset(3 * u, 8 * u), androidx.compose.ui.geometry.Size(4 * u, u))
            }
            AppDestination.CHARACTER -> {
                block(4, 1, 4, 3); block(3, 4, 6, 3); block(2, 7, 8, 4)
                drawRect(PixelInk, Offset(5 * u, 2 * u), androidx.compose.ui.geometry.Size(u, u))
                drawRect(PixelInk, Offset(7 * u, 2 * u), androidx.compose.ui.geometry.Size(u, u))
            }
            AppDestination.WORLD -> {
                block(1, 5, 10, 6); block(3, 3, 6, 2); block(5, 1, 2, 2)
                drawRect(PixelInk, Offset(3 * u, 7 * u), androidx.compose.ui.geometry.Size(2 * u, 2 * u))
                drawRect(PixelInk, Offset(7 * u, 6 * u), androidx.compose.ui.geometry.Size(2 * u, 3 * u))
            }
            AppDestination.HISTORY -> {
                block(2, 1, 8, 10); block(1, 3, 2, 6)
                drawRect(PixelInk, Offset(4 * u, 4 * u), androidx.compose.ui.geometry.Size(4 * u, u))
                drawRect(PixelInk, Offset(4 * u, 7 * u), androidx.compose.ui.geometry.Size(4 * u, u))
            }
        }
    }
}
