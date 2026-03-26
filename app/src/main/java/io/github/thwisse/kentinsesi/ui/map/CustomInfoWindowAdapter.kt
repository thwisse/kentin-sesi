package io.github.thwisse.kentinsesi.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.github.thwisse.kentinsesi.R

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        // Return null to allow Google Maps to frame the view natively
        return null
    }

    override fun getInfoContents(marker: Marker): View {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_info_contents, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSnippet = view.findViewById<TextView>(R.id.tvSnippet)

        tvTitle.text = marker.title
        
        val snippetText = marker.snippet
        if (!snippetText.isNullOrEmpty()) {
            tvSnippet.text = snippetText
            tvSnippet.visibility = View.VISIBLE
        } else {
            tvSnippet.visibility = View.GONE
        }

        return view
    }
}
