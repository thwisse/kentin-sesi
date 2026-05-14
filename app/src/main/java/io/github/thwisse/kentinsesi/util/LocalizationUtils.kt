package io.github.thwisse.kentinsesi.util

import android.content.Context
import io.github.thwisse.kentinsesi.R

fun String.toLocalizedCategory(context: Context): String {
    return when (this) {
        "Altyapı", "infrastructure", "Altyapı (Yol/Su)", "infrastructure_road_water" ->
            context.getString(R.string.category_infrastructure)
        "Ulaşım", "transportation" ->
            context.getString(R.string.category_transportation)
        "Çevre", "environment" ->
            context.getString(R.string.category_environment)
        "Aydınlatma", "lighting" ->
            context.getString(R.string.category_lighting)
        "Park/Bahçe", "park" ->
            context.getString(R.string.category_park)
        "Sokak Hayvanları", "animals" ->
            context.getString(R.string.category_animals)
        "Temizlik/Çöp", "cleaning" ->
            context.getString(R.string.category_cleaning)
        "Trafik", "traffic" ->
            context.getString(R.string.category_traffic)
        "Diğer", "other" ->
            context.getString(R.string.category_other)
        else -> this
    }
}

fun String.toLocalizedTitle(context: Context): String {
    return when (this) {
        Constants.TITLE_NEW_USER, "Yeni Kullanıcı" ->
            context.getString(R.string.title_new_user)
        Constants.TITLE_SENSITIVE_CITIZEN, "Duyarlı Vatandaş" ->
            context.getString(R.string.title_sensitive_citizen)
        Constants.TITLE_ACTIVE_CITIZEN, "Aktif Vatandaş" ->
            context.getString(R.string.title_active_citizen)
        Constants.TITLE_CHAMPION, "Şampiyon Vatandaş" ->
            context.getString(R.string.title_champion_citizen)
        else -> this
    }
}
