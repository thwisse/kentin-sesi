package io.github.thwisse.kentinsesi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible // Görünürlük için
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // ViewModel'i almak için
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint // Hilt için
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentLoginBinding
import io.github.thwisse.kentinsesi.ui.AuthActivity
import io.github.thwisse.kentinsesi.ui.MainActivity
import kotlinx.coroutines.launch // Coroutine için

@AndroidEntryPoint // Hilt'in bu fragment'a ViewModel enjekte edebilmesi için
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // ViewModel'i Hilt aracılığıyla alıyoruz (Artık FirebaseAuth'a gerek yok)
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeLoginState() // ViewModel'deki durumu dinlemeye başla
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "E-posta ve şifre girilmelidir",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            // Artık Firebase kodları burada değil, sadece ViewModel'i çağırıyoruz
            viewModel.loginUser(email, password)
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    // ViewModel'deki loginState LiveData'sını dinleyen fonksiyon
    private fun observeLoginState() {
        // lifecycleScope: Bu Coroutine, Fragment'ın yaşam döngüsüne bağlıdır.
        // repeatOnLifecycle(Lifecycle.State.STARTED): Fragment STARTED durumuna geldiğinde başlar, STOPPED olduğunda durur.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.observe(viewLifecycleOwner) { state ->
                    // ProgressBar'ı durumu Loading ise göster, değilse gizle
                    binding.progressBar.isVisible = state is AuthState.Loading
                    // Butonları Loading durumunda deaktif et (isteğe bağlı)
                    binding.btnLogin.isEnabled = state !is AuthState.Loading
                    binding.tvGoToRegister.isEnabled = state !is AuthState.Loading

                    when (state) {
                        is AuthState.Success -> {
                            // Giriş başarılı, MainActivity'ye yönlendir
                            Toast.makeText(requireContext(), "Giriş başarılı!", Toast.LENGTH_SHORT)
                                .show()
                            Log.d("LoginFragment", "Giriş başarılı!")
                            navigateToMain()
                        }

                        is AuthState.Error -> {
                            // Hata mesajını göster
                            Toast.makeText(
                                requireContext(),
                                "Giriş başarısız: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("LoginFragment", "Giriş başarısız: ${state.message}")
                        }

                        AuthState.Idle -> {
                            // Başlangıç durumu, bir şey yapma
                        }

                        AuthState.Loading -> {
                            // Yükleniyor durumu, ProgressBar zaten gösterildi
                        }
                    }
                }
            }
        }
    }


    private fun navigateToMain() {
        (activity as? AuthActivity)?.let {
            val intent = Intent(it, MainActivity::class.java)
            // Intent flag'leri: Geri tuşuna basıldığında login ekranına dönülmesini engeller
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.startActivity(intent)
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Memory leak önlemek için binding'i temizle
    }
}