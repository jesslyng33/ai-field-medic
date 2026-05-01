package com.google.ai.edge.gallery.ui.fieldmedic

import android.content.Context
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

private const val ROUTE_ONBOARDING_MEDICAL = "fm_onboarding_medical"
private const val ROUTE_ONBOARDING_TRIP = "fm_onboarding_trip"
private const val ROUTE_HOME = "fm_home"
private const val ROUTE_ASSESSMENT = "fm_assessment"
private const val ROUTE_THINKING = "fm_thinking"
private const val ROUTE_GUIDANCE = "fm_guidance"
private const val ROUTE_SUMMARY = "fm_summary"
private const val ROUTE_TRIAGE_LOOP = "fm_triage_loop"

@Composable
fun FieldMedicNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("field_medic_prefs", Context.MODE_PRIVATE)
    }
    val medicalOnboardingDone = remember {
        prefs.getBoolean("medical_onboarding_done", false)
    }

    val fmViewModel: FieldMedicViewModel = viewModel()

    FieldMedicAppTheme {
        NavHost(
            navController = navController,
            startDestination = if (medicalOnboardingDone) ROUTE_ONBOARDING_TRIP else ROUTE_ONBOARDING_MEDICAL,
            enterTransition = {
                fadeIn(tween(250)) +
                    slideInHorizontally(tween(320, easing = EaseOutCubic)) { it / 10 }
            },
            exitTransition = {
                fadeOut(tween(200)) +
                    slideOutHorizontally(tween(220)) { -it / 10 }
            },
            popEnterTransition = {
                fadeIn(tween(250)) +
                    slideInHorizontally(tween(320, easing = EaseOutCubic)) { -it / 10 }
            },
            popExitTransition = {
                fadeOut(tween(200)) +
                    slideOutHorizontally(tween(220)) { it / 10 }
            },
        ) {
            composable(ROUTE_ONBOARDING_MEDICAL) {
                OnboardingMedicalScreen(
                    onContinue = {
                        prefs.edit().putBoolean("medical_onboarding_done", true).apply()
                        navController.navigate(ROUTE_ONBOARDING_TRIP) {
                            popUpTo(ROUTE_ONBOARDING_MEDICAL) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_ONBOARDING_TRIP) {
                OnboardingTripScreen(
                    onReady = {
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_ONBOARDING_TRIP) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_HOME) {
                FieldMedicHomeScreen(
                    onGetHelp = { navController.navigate(ROUTE_ASSESSMENT) },
                    onTriageLoop = { navController.navigate(ROUTE_TRIAGE_LOOP) },
                )
            }

            composable(ROUTE_ASSESSMENT) {
                AssessmentScreen(
                    viewModel = fmViewModel,
                    onBack = { navController.navigateUp() },
                    onAnalyze = { navController.navigate(ROUTE_THINKING) }
                )
            }

            composable(ROUTE_THINKING) {
                ThinkingScreen(
                    viewModel = fmViewModel,
                    onReady = {
                        navController.navigate(ROUTE_GUIDANCE) {
                            popUpTo(ROUTE_THINKING) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_GUIDANCE) {
                GuidanceScreen(
                    viewModel = fmViewModel,
                    onEndSession = { navController.navigate(ROUTE_SUMMARY) }
                )
            }

            composable(ROUTE_TRIAGE_LOOP) {
                val triageVm: TriageLoopViewModel = viewModel()
                TriageLoopScreen(
                    viewModel = triageVm,
                    onExit = { navController.navigateUp() },
                )
            }

            composable(ROUTE_SUMMARY) {
                SummaryScreen(
                    onNewSession = {
                        navController.navigate(ROUTE_ONBOARDING_TRIP) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
