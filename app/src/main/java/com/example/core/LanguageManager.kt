package com.example.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage {
    ARABIC,
    ENGLISH
}

object LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.ARABIC)
        private set

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
    }

    val isArabic: Boolean
        get() = currentLanguage == AppLanguage.ARABIC
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

@Composable
fun ProvideLocalizedLayout(content: @Composable () -> Unit) {
    val lang = LanguageManager.currentLanguage
    val layoutDirection = if (lang == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppLanguage provides lang,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}

/**
 * Localized string key mapping
 */
fun loc(en: String, ar: String): String {
    return if (LanguageManager.isArabic) ar else en
}
