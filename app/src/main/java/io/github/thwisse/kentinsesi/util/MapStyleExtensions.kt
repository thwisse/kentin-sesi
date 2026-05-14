package io.github.thwisse.kentinsesi.util

import android.content.Context
import android.content.res.Configuration
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import io.github.thwisse.kentinsesi.R

fun GoogleMap.applyAppTheme(context: Context) {
    val isNight = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    if (isNight) {
        setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_night))
    } else {
        setMapStyle(null)
    }
}
