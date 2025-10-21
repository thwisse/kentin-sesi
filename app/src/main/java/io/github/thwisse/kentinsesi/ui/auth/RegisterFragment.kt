package io.github.thwisse.kentinsesi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentRegisterBinding
import io.github.thwisse.kentinsesi.ui.AuthActivity
import io.github.thwisse.kentinsesi.ui.MainActivity
import kotlinx.coroutines.launch

@AndroidEntryPoint // Hilt için
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // ViewModel'i alıyoruz
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeRegistrationState() // ViewModel'deki durumu dinle
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Tüm alanlar doldurulmalıdır", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            // Sadece ViewModel'i çağır
            viewModel.registerUser(fullName, email, password)
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun observeRegistrationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registrationState.observe(viewLifecycleOwner) { state ->
                    // ProgressBar yönetimi
                    binding.progressBar.isVisible = state is AuthState.Loading
                    binding.btnRegister.isEnabled = state !is AuthState.Loading
                    binding.tvGoToLogin.isEnabled = state !is AuthState.Loading

                    when (state) {
                        is AuthState.Success -> {
                            Toast.makeText(requireContext(), "Kayıt başarılı!", Toast.LENGTH_SHORT)
                                .show()
                            Log.d("RegisterFragment", "Kayıt başarılı!")
                            navigateToMain()
                        }

                        is AuthState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                "Kayıt başarısız: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("RegisterFragment", "Kayıt başarısız: ${state.message}")
                        }

                        AuthState.Idle -> {}
                        AuthState.Loading -> {}
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
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