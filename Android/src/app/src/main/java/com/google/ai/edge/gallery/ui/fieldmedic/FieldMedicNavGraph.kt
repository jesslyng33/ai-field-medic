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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.NavBackStackEntry
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingAllergiesScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingConditionsScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingContactScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingIdentityScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingMedicationsScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingSummaryScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingViewModel
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingVitalsScreen
import com.google.ai.edge.gallery.ui.fieldmedic.onboarding.OnboardingWelcomeScreen
import com.google.ai.edge.gallery.ui.fieldmedic.settings.AllergiesEditScreen
import com.google.ai.edge.gallery.ui.fieldmedic.settings.ConditionsEditScreen
import com.google.ai.edge.gallery.ui.fieldmedic.settings.ContactEditScreen
import com.google.ai.edge.gallery.ui.fieldmedic.settings.MedicationsEditScreen
import com.google.ai.edge.gallery.ui.fieldmedic.settings.SettingsScreen

private const val ROUTE_ONBOARDING_GRAPH = "fm_onboarding_graph"
private const val ROUTE_ONBOARDING_WELCOME = "fm_onboarding_welcome"
private const val ROUTE_ONBOARDING_IDENTITY = "fm_onboarding_identity"
private const val ROUTE_ONBOARDING_VITALS = "fm_onboarding_vitals"
private const val ROUTE_ONBOARDING_ALLERGIES = "fm_onboarding_allergies"
private const val ROUTE_ONBOARDING_CONDITIONS = "fm_onboarding_conditions"
private const val ROUTE_ONBOARDING_MEDICATIONS = "fm_onboarding_medications"
private const val ROUTE_ONBOARDING_CONTACT = "fm_onboarding_contact"
private const val ROUTE_ONBOARDING_SUMMARY = "fm_onboarding_summary"

private const val ROUTE_ONBOARDING_TRIP = "fm_onboarding_trip"
private const val ROUTE_HOME = "fm_home"

private const val ROUTE_SETTINGS = "fm_settings"
private const val ROUTE_SETTINGS_ALLERGIES = "fm_settings_allergies"
private const val ROUTE_SETTINGS_CONDITIONS = "fm_settings_conditions"
private const val ROUTE_SETTINGS_MEDICATIONS = "fm_settings_medications"
private const val ROUTE_SETTINGS_CONTACT = "fm_settings_contact"

private const val ROUTE_ASSESSMENT = "fm_assessment"
private const val ROUTE_THINKING = "fm_thinking"
private const val ROUTE_GUIDANCE = "fm_guidance"
private const val ROUTE_SUMMARY = "fm_summary"

private const val PREFS_NAME = "field_medic_prefs"
private const val PREF_MEDICAL_DONE = "medical_onboarding_done"

@Composable
fun FieldMedicNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val medicalOnboardingDone = remember {
        prefs.getBoolean(PREF_MEDICAL_DONE, false)
    }

    FieldMedicAppTheme {
        NavHost(
            navController = navController,
            startDestination = if (medicalOnboardingDone) ROUTE_ONBOARDING_TRIP else ROUTE_ONBOARDING_GRAPH,
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
            navigation(
                route = ROUTE_ONBOARDING_GRAPH,
                startDestination = ROUTE_ONBOARDING_WELCOME,
            ) {
                composable(ROUTE_ONBOARDING_WELCOME) {
                    OnboardingWelcomeScreen(
                        onContinue = { navController.navigate(ROUTE_ONBOARDING_IDENTITY) },
                    )
                }
                composable(ROUTE_ONBOARDING_IDENTITY) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingIdentityScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onContinue = {
                            if (vm.identityComplete) navController.navigate(ROUTE_ONBOARDING_VITALS)
                        },
                    )
                }
                composable(ROUTE_ONBOARDING_VITALS) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingVitalsScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onContinue = { navController.navigate(ROUTE_ONBOARDING_ALLERGIES) },
                    )
                }
                composable(ROUTE_ONBOARDING_ALLERGIES) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingAllergiesScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onContinue = { navController.navigate(ROUTE_ONBOARDING_CONDITIONS) },
                    )
                }
                composable(ROUTE_ONBOARDING_CONDITIONS) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingConditionsScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onContinue = { navController.navigate(ROUTE_ONBOARDING_MEDICATIONS) },
                    )
                }
                composable(ROUTE_ONBOARDING_MEDICATIONS) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingMedicationsScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onContinue = { navController.navigate(ROUTE_ONBOARDING_CONTACT) },
                    )
                }
                composable(ROUTE_ONBOARDING_CONTACT) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingContactScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onContinue = {
                            if (vm.contactComplete) navController.navigate(ROUTE_ONBOARDING_SUMMARY)
                        },
                    )
                }
                composable(ROUTE_ONBOARDING_SUMMARY) { backStack ->
                    val vm = onboardingViewModel(navController, backStack)
                    OnboardingSummaryScreen(
                        vm = vm,
                        onBack = { navController.navigateUp() },
                        onFinish = {
                            vm.commit {
                                prefs.edit().putBoolean(PREF_MEDICAL_DONE, true).apply()
                                navController.navigate(ROUTE_ONBOARDING_TRIP) {
                                    popUpTo(ROUTE_ONBOARDING_GRAPH) { inclusive = true }
                                }
                            }
                        },
                    )
                }
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
                    onSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            }

            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    onBack = { navController.navigateUp() },
                    onEditProfile = {
                        prefs.edit().putBoolean(PREF_MEDICAL_DONE, false).apply()
                        navController.navigate(ROUTE_ONBOARDING_GRAPH) {
                            popUpTo(ROUTE_HOME) { inclusive = false }
                        }
                    },
                    onEditAllergies = { navController.navigate(ROUTE_SETTINGS_ALLERGIES) },
                    onEditConditions = { navController.navigate(ROUTE_SETTINGS_CONDITIONS) },
                    onEditMedications = { navController.navigate(ROUTE_SETTINGS_MEDICATIONS) },
                    onEditContacts = { navController.navigate(ROUTE_SETTINGS_CONTACT) },
                )
            }
            composable(ROUTE_SETTINGS_ALLERGIES) {
                AllergiesEditScreen(onBack = { navController.navigateUp() })
            }
            composable(ROUTE_SETTINGS_CONDITIONS) {
                ConditionsEditScreen(onBack = { navController.navigateUp() })
            }
            composable(ROUTE_SETTINGS_MEDICATIONS) {
                MedicationsEditScreen(onBack = { navController.navigateUp() })
            }
            composable(ROUTE_SETTINGS_CONTACT) {
                ContactEditScreen(onBack = { navController.navigateUp() })
            }

            composable(ROUTE_ASSESSMENT) {
                AssessmentScreen(
                    onBack = { navController.navigateUp() },
                    onAnalyze = { navController.navigate(ROUTE_THINKING) }
                )
            }
            composable(ROUTE_THINKING) {
                ThinkingScreen(
                    onReady = {
                        navController.navigate(ROUTE_GUIDANCE) {
                            popUpTo(ROUTE_THINKING) { inclusive = true }
                        }
                    }
                )
            }
            composable(ROUTE_GUIDANCE) {
                GuidanceScreen(
                    onEndSession = { navController.navigate(ROUTE_SUMMARY) }
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

@Composable
private fun onboardingViewModel(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
): OnboardingViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(ROUTE_ONBOARDING_GRAPH)
    }
    return hiltViewModel(parentEntry)
}
