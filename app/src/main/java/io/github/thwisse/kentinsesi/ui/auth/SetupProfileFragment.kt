package io.github.thwisse.kentinsesi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentSetupProfileBinding
import io.github.thwisse.kentinsesi.ui.AuthActivity
import io.github.thwisse.kentinsesi.ui.MainActivity
import io.github.thwisse.kentinsesi.util.Resource
import io.github.thwisse.kentinsesi.util.loadAvatar
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class SetupProfileFragment : Fragment() {

    private var _binding: FragmentSetupProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    
    // Avatar history management
    private val avatarHistory = mutableListOf<String>()
    private var currentAvatarIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupAvatarSelection()
        setupClickListeners()
        observeState()
    }

    private fun setupDropdowns() {
        // Hatay ilçeleri listesi
        val districts = listOf(
            "Altınözü", "Antakya", "Arsuz", "Belen", "Defne", "Dörtyol",
            "Erzin", "Hassa", "İskenderun", "Kırıkhan", "Kumlu",
            "Payas", "Reyhanlı", "Samandağ", "Yayladağı"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, districts)
        binding.actvDistrict.setAdapter(adapter)
    }
    
    private fun setupAvatarSelection() {
        // İlk avatar'ı otomatik üret
        if (avatarHistory.isEmpty()) {
            val initialSeed = generateAvatarSeed()
            avatarHistory.add(initialSeed)
            currentAvatarIndex = 0
            binding.ivAvatar.loadAvatar(initialSeed)
        }
        
        updateAvatarButtons()
        
        // Previous button
        binding.btnAvatarPrevious.setOnClickListener {
            if (currentAvatarIndex > 0) {
                currentAvatarIndex--
                binding.ivAvatar.loadAvatar(avatarHistory[currentAvatarIndex])
                updateAvatarButtons()
            }
        }
        
        // Next button
        binding.btnAvatarNext.setOnClickListener {
            if (currentAvatarIndex < avatarHistory.size - 1) {
                // Geçmişte gezinme
                currentAvatarIndex++
                binding.ivAvatar.loadAvatar(avatarHistory[currentAvatarIndex])
            } else {
                // Yeni avatar üret
                if (avatarHistory.size >= 31) {
                    // Max limit - en eskiyi sil
                    avatarHistory.removeAt(0)
                }
                val newSeed = generateAvatarSeed()
                avatarHistory.add(newSeed)
                currentAvatarIndex = avatarHistory.size - 1
                binding.ivAvatar.loadAvatar(newSeed)
            }
            updateAvatarButtons()
        }
    }
    
    private fun updateAvatarButtons() {
        binding.btnAvatarPrevious.isEnabled = currentAvatarIndex > 0
    }
    
    private fun generateAvatarSeed(): String = UUID.randomUUID().toString()

    private fun setupClickListeners() {
        binding.btnSaveProfile.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val usernameRaw = binding.etUsername.text.toString().trim()
            val city = binding.actvCity.text.toString().trim() // "Hatay" sabit zaten
            val district = binding.actvDistrict.text.toString().trim()

            if (fullName.isEmpty()) {
                binding.tilFullName.error = getString(R.string.validation_name_required)
                return@setOnClickListener
            } else {
                binding.tilFullName.error = null
            }

            val username = usernameRaw
                .removePrefix("@").trim()
                .lowercase()

            val usernameRegex = Regex("^[a-z0-9_]{3,20}$")
            if (username.isBlank()) {
                binding.tilUsername.error = getString(R.string.validation_username_required)
                return@setOnClickListener
            } else if (!usernameRegex.matches(username)) {
                binding.tilUsername.error = getString(R.string.validation_username_format)
                return@setOnClickListener
            } else {
                binding.tilUsername.error = null
            }

            if (district.isEmpty()) {
                binding.tilDistrict.error = getString(R.string.validation_district_required)
                return@setOnClickListener
            } else {
                binding.tilDistrict.error = null
            }

            // Şu an seçili olan avatar seed'ini al
            val avatarSeed = avatarHistory.getOrNull(currentAvatarIndex) ?: generateAvatarSeed()
            
            // ViewModel'e verileri gönder (avatarSeed ekledik)
            viewModel.completeProfile(fullName, username, city, district, avatarSeed)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateProfileState.observe(viewLifecycleOwner) { resource ->

                    binding.progressBar.isVisible = resource is Resource.Loading
                    binding.btnSaveProfile.isEnabled = resource !is Resource.Loading

                    when (resource) {
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), getString(R.string.profile_ready), Toast.LENGTH_SHORT).show()
                            // Burası AuthActivity'yi bitirip MainActivity'yi başlatır
                            navigateToMain()
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        }
                        is Resource.Loading -> { }
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        // AuthActivity'den MainActivity'ye geçiş
        (activity as? AuthActivity)?.let {
            val intent = Intent(it, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.startActivity(intent)
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}