package io.github.thwisse.kentinsesi.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.model.Post
import io.github.thwisse.kentinsesi.databinding.FragmentMapBinding
import io.github.thwisse.kentinsesi.ui.home.HomeViewModel
import io.github.thwisse.kentinsesi.util.Resource

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null

    private var latestPosts: List<Post> = emptyList()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Konumunuza gitmek için izin gerekli.", Toast.LENGTH_SHORT).show()
            }
        }

    // Marker ile Post'u eşleştirmek için bir harita (Map) tutuyoruz
    private val markerPostMap = HashMap<Marker, Post>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)

        setupMenu()
        setupFilterResultListener()

        // Paylaşılan post state'ini erken dinle (harita hazır olmasa bile listeyi cache'le)
        observePosts()

        // Haritayı Başlat
        val mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.menu_map, menu)
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_post -> {
                        findNavController().navigate(R.id.action_nav_map_to_createPostFragment)
                        true
                    }
                    R.id.action_filter -> {
                        showFilterChoiceDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showFilterChoiceDialog() {
        val items = arrayOf("Filtrelerim", "Yeni filtre ayarla")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtre")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        findNavController().navigate(R.id.filterPresetsBottomSheetFragment)
                    }
                    1 -> {
                        val bundle = Bundle().apply {
                            viewModel.lastDistricts?.let { putStringArrayList("districts", ArrayList(it)) }
                            viewModel.lastCategories?.let { putStringArrayList("categories", ArrayList(it)) }
                            viewModel.lastStatuses?.let { putStringArrayList("statuses", ArrayList(it)) }
                            putBoolean("onlyMyPosts", viewModel.lastOnlyMyPosts)
                        }
                        findNavController().navigate(R.id.action_nav_map_to_filterBottomSheetFragment, bundle)
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun setupFilterResultListener() {
        setFragmentResultListener("filter_request") { _, bundle ->
            val districts = bundle.getStringArrayList("districts")
            val categories = bundle.getStringArrayList("categories")
            val statuses = bundle.getStringArrayList("statuses")
            val onlyMyPosts = bundle.getBoolean("onlyMyPosts", false)

            viewModel.getPosts(
                districts = districts?.toList(),
                categories = categories?.toList(),
                statuses = statuses?.toList(),
                onlyMyPosts = onlyMyPosts
            )

            Toast.makeText(requireContext(), "Filtreler uygulandı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(requireContext()))

        // Baloncuk tıklamasını dinle
        map.setOnInfoWindowClickListener(this)

        // Başlangıç konumu (Örn: İskenderun Meydanı)
        val startLocation = LatLng(36.58, 36.17)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 12f))

        enableMyLocation()

        // Harita hazır olduğunda mevcut filtrelenmiş listeyi bas
        addMarkers(latestPosts)
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true

            googleMap?.setOnMyLocationButtonClickListener {
                val lm = requireContext().getSystemService(LocationManager::class.java)
                val enabled = lm != null && LocationManagerCompat.isLocationEnabled(lm)
                if (!enabled) {
                    Toast.makeText(requireContext(), "Konumunuz kapalı. Lütfen konumu açın.", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    false
                }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun observePosts() {
        viewModel.postsState.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Success -> {
                    val posts = resource.data ?: emptyList()
                    latestPosts = posts
                    if (googleMap != null) {
                        addMarkers(posts)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Loading gösterilebilir
                }
            }
        }
    }

    private fun addMarkers(posts: List<Post>) {
        googleMap?.clear() // Önce temizle
        markerPostMap.clear()

        for (post in posts) {
            if (post.location != null) {
                val position = LatLng(post.location.latitude, post.location.longitude)

                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(post.title)
                        .snippet(post.category) // Altına kategori yazsın
                )

                // Marker ile Post nesnesini eşleştir
                if (marker != null) {
                    markerPostMap[marker] = post
                }
            }
        }
    }

    // Marker'ın üzerindeki balona tıklanınca çalışır
    override fun onInfoWindowClick(marker: Marker) {
        val post = markerPostMap[marker]
        if (post != null) {
            // Detay sayfasına git (sadece post ID gönder)
            val bundle = Bundle().apply { putString("postId", post.id) }
            try {
                findNavController().navigate(R.id.action_nav_map_to_postDetailFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigasyon hatası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}