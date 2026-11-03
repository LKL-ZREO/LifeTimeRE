package com.lifetimere.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lifetimere.app.ui.screens.CharacterScreen
import com.lifetimere.app.ui.screens.HistoryScreen
import com.lifetimere.app.ui.screens.TodayScreen
import com.lifetimere.app.ui.screens.WorldScreen
import com.lifetimere.app.ui.theme.LifeTimeTheme

@Preview(name = "今日", showBackground = true, locale = "zh")
@Composable
private fun TodayPreview() {
    LifeTimeTheme {
        Surface {
            TodayScreen(
                uiState = TodayUiState(isLoaded = true, dateLabel = "2026年11月1日"),
                onDraftChange = {},
                onSave = {},
            )
        }
    }
}

@Preview(name = "角色", showBackground = true, locale = "zh")
@Composable
private fun CharacterPreview() {
    LifeTimeTheme { Surface { CharacterScreen(emptyMap(), emptyList()) } }
}

@Preview(name = "世界", showBackground = true, locale = "zh")
@Composable
private fun WorldPreview() {
    LifeTimeTheme {
        Surface {
            WorldScreen(
                journeyDirection = null,
                campState = com.lifetimere.app.domain.CampState(),
                onSaveJourneyDirection = {},
            )
        }
    }
}

@Preview(name = "回顾", showBackground = true, locale = "zh")
@Composable
private fun HistoryPreview() {
    LifeTimeTheme {
        Surface {
            HistoryScreen(
                entries = emptyList(),
                reviewStatuses = emptyMap(),
                onWriteClick = {},
                onEditEntry = { _, _ -> },
                onDeleteEntry = {},
            )
        }
    }
}
