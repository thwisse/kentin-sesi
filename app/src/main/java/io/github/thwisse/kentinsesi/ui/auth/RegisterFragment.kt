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
import io.github.thwisse.kentinsesi.util.ValidationUtils
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
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // ValidationUtils kullanarak daha iyi hata mesajları gösteriyoruz
            val emailError = ValidationUtils.getValidationError("email", email, requireContext())
            val passwordError = ValidationUtils.getValidationError("password", password, requireContext())

            when {
                email.isEmpty() -> {
                    binding.etEmail.error = getString(R.string.email_required)
                    binding.etEmail.requestFocus()
                    return@setOnClickListener
                }
                emailError != null -> {
                    binding.etEmail.error = emailError
                    binding.etEmail.requestFocus()
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    binding.etPassword.error = getString(R.string.password_required)
                    binding.etPassword.requestFocus()
                    return@setOnClickListener
                }
                passwordError != null -> {
                    binding.etPassword.error = passwordError
                    binding.etPassword.requestFocus()
                    return@setOnClickListener
                }
            }

            // Tüm validasyonlar geçti, ViewModel'i çağır
            viewModel.registerUser(email, password)
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
                            Toast.makeText(requireContext(), getString(R.string.registration_success), Toast.LENGTH_SHORT).show()

                            // DİKKAT: Artık navigateToMain() YOK.
                            // auth_nav_graph içinde bir sonraki adıma geçiyoruz:
                            findNavController().navigate(R.id.action_registerFragment_to_setupProfileFragment)
                        }

                        is AuthState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.registration_failed, state.message),
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("RegisterFragment", "Registration failed: ${state.message}")
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