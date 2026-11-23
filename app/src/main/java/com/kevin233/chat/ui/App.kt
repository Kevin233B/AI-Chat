package com.kevin233.chat.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen

@Composable
fun App(vm: ChatViewModel) {
    val settings = vm.settings
    val wallpaperActive = settings.wallpaper != "none" &&
        (settings.wallpaper != "custom" || settings.wallpaperPath.isNotBlank())
    AIChatTheme(settings.themeMode, settings.accent, settings.glassAlpha, wallpaperActive) {
        val view = LocalView.current
        val dark = darkTheme
        if (!view.isInEditMode) {
            SideEffect {
                val window = view.context.findActivity()?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (wallpaperActive) {
                WallpaperLayer(settings.wallpaper, settings.wallpaperPath)
                Box(Modifier.fillMaxSize().background(WallpaperScrim))
            } else {
                Box(Modifier.fillMaxSize().background(pal.bg))
            }
            when {
                !settings.onboarded -> OnboardingScreen(vm)
                vm.screen == Screen.AGENTS -> AgentsScreen(vm)
                vm.screen == Screen.HISTORY -> HistoryScreen(vm)
                vm.screen == Screen.SETTINGS || vm.screen == Screen.CONFIG_EDIT -> SettingsScreen(vm)
                else -> ChatScreen(vm)
            }
            ToastOverlay(vm.toast) { vm.clearToast() }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
