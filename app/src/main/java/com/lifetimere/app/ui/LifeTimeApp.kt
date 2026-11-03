package com.lifetimere.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collect
import com.lifetimere.app.ui.components.PixelNavIcon
import com.lifetimere.app.ui.navigation.AppDestination
import com.lifetimere.app.ui.screens.CharacterScreen
import com.lifetimere.app.ui.screens.HistoryScreen
import com.lifetimere.app.ui.screens.TodayScreen
import com.lifetimere.app.ui.screens.SettingsScreen
import com.lifetimere.app.ui.screens.TravelerOnboardingScreen
import com.lifetimere.app.ui.screens.WorldScreen
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelInk
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelOutline
import com.lifetimere.app.ui.theme.PixelNight
import com.lifetimere.app.data.model.TravelerProfileStatus

@Composable
fun LifeTimeApp(appViewModel: LifeTimeViewModel = viewModel()) {
    LifecycleResumeEffect(Unit) {
        appViewModel.refreshDate()
        onPauseOrDispose { }
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val todayUiState by appViewModel.todayUiState.collectAsStateWithLifecycle()
    val entries by appViewModel.entries.collectAsStateWithLifecycle()
    val journeyDirection by appViewModel.journeyDirection.collectAsStateWithLifecycle()
    val attributeTotals by appViewModel.attributeTotals.collectAsStateWithLifecycle()
    val reviewStatuses by appViewModel.reviewStatuses.collectAsStateWithLifecycle()
    val growthSources by appViewModel.growthSources.collectAsStateWithLifecycle()
    val campState by appViewModel.campState.collectAsStateWithLifecycle()
    val travelerProfile by appViewModel.travelerProfile.collectAsStateWithLifecycle()
    val isTravelerProfileLoaded by appViewModel.isTravelerProfileLoaded.collectAsStateWithLifecycle()
    val aiSettings by appViewModel.aiSettings.collectAsStateWithLifecycle()
    val aiConnectionState by appViewModel.aiConnectionState.collectAsStateWithLifecycle()
    var destinationAfterRegistration by remember { mutableStateOf<AppDestination?>(null) }
    var initialDestination by remember { mutableStateOf(AppDestination.TODAY) }
    var firstEchoAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        appViewModel.firstCampEchoEvents.collect {
            firstEchoAnimation = true
            navController.navigate(AppDestination.WORLD.route) { launchSingleTop = true }
        }
    }

    LaunchedEffect(isTravelerProfileLoaded, travelerProfile?.status, destinationAfterRegistration) {
        val status = travelerProfile?.status
        if (isTravelerProfileLoaded && status != TravelerProfileStatus.IN_PROGRESS &&
            status != TravelerProfileStatus.RETAKING && destinationAfterRegistration != null
        ) {
            val destination = destinationAfterRegistration!!
            destinationAfterRegistration = null
            navController.navigate(destination.route) { launchSingleTop = true }
        }
    }

    if (!isTravelerProfileLoaded) {
        Box(Modifier.fillMaxSize().background(PixelNight), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PixelGold)
        }
    } else if (travelerProfile == null || travelerProfile?.status in setOf(
            TravelerProfileStatus.IN_PROGRESS,
            TravelerProfileStatus.RETAKING,
        )
    ) {
        TravelerOnboardingScreen(
            profile = travelerProfile,
            attributeGrowth = attributeTotals,
            onBegin = { appViewModel.beginTravelerRegistration() },
            onAnswer = appViewModel::chooseTravelerAnswer,
            onPrevious = appViewModel::previousTravelerQuestion,
            onComplete = {
                if (travelerProfile?.status == TravelerProfileStatus.RETAKING) {
                    destinationAfterRegistration = AppDestination.CHARACTER
                } else {
                    initialDestination = AppDestination.WORLD
                }
                appViewModel.completeTravelerRegistration()
            },
            onSkip = {
                if (travelerProfile?.status == TravelerProfileStatus.RETAKING) {
                    destinationAfterRegistration = AppDestination.CHARACTER
                } else {
                    initialDestination = AppDestination.TODAY
                }
                appViewModel.skipTravelerRegistration()
            },
            onRetake = { appViewModel.beginTravelerRegistration(retake = true) },
        )
    } else Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute != SETTINGS_ROUTE) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixelInk)
                    .border(width = 1.dp, color = PixelOutline)
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .selectableGroup(),
                ) {
                    AppDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.Tab,
                                    onClick = {
                                        val startDestination = navController.graph.findStartDestination()
                                        navController.navigate(destination.route) {
                                            popUpTo(startDestination.id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            // Popping to the start page can associate the popped stack with it.
                                            // Restoring that stack here would open its previous top page instead.
                                            restoreState = destination.route != startDestination.route
                                        }
                                    },
                                )
                                .background(if (selected) PixelGold.copy(alpha = 0.10f) else PixelInk)
                                .padding(top = 8.dp, bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            PixelNavIcon(destination, selected)
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) PixelGold else PixelMuted,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialDestination.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.TODAY.route) {
                TodayScreen(
                    uiState = todayUiState,
                    onDraftChange = appViewModel::updateDraft,
                    onSave = appViewModel::saveToday,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true } },
                    onReportReview = appViewModel::reportReview,
                    onSummarize = appViewModel::summarizeToday,
                    onEditNote = appViewModel::editNote,
                    onDeleteNote = appViewModel::deleteNote,
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    settings = aiSettings,
                    connectionState = aiConnectionState,
                    onBack = { navController.popBackStack() },
                    onSave = appViewModel::saveAiSettings,
                    onTestConnection = appViewModel::testAiConnection,
                )
            }
            composable(AppDestination.CHARACTER.route) {
                CharacterScreen(
                    attributeTotals = attributeTotals,
                    growthSources = growthSources,
                    travelerProfile = travelerProfile,
                    onEditProfile = {
                        destinationAfterRegistration = AppDestination.CHARACTER
                        appViewModel.beginTravelerRegistration(retake = true)
                    },
                    onReportReview = appViewModel::reportReview,
                )
            }
            composable(AppDestination.WORLD.route) {
                WorldScreen(
                    journeyDirection = journeyDirection,
                    campState = campState,
                    onSaveJourneyDirection = appViewModel::saveJourneyDirection,
                    isVisible = currentRoute == AppDestination.WORLD.route,
                    firstEchoSequence = firstEchoAnimation,
                    onFirstEchoAnimationComplete = { firstEchoAnimation = false },
                    onReportReview = appViewModel::reportReview,
                )
            }
            composable(AppDestination.HISTORY.route) {
                HistoryScreen(
                    entries = entries,
                    reviewStatuses = reviewStatuses,
                    onWriteClick = {
                        navController.navigate(AppDestination.TODAY.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    onEditEntry = appViewModel::updateEntry,
                    onDeleteEntry = appViewModel::deleteEntry,
                    sources = growthSources.associateBy { it.entry.id },
                    onSummarize = appViewModel::summarizeEntry,
                )
            }
        }
    }
}

private const val SETTINGS_ROUTE = "settings"
