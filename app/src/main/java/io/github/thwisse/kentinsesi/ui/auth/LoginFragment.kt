package io.github.thwisse.kentinsesi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentLoginBinding
import io.github.thwisse.kentinsesi.ui.AuthActivity
import io.github.thwisse.kentinsesi.ui.MainActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        auth = Firebase.auth

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            // Giriş yapma fonksiyonunu çağır
            loginUser()
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    // Yeni giriş fonksiyonunu ekle
    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "E-posta ve şifre girilmelidir", Toast.LENGTH_SHORT).show()
            Log.d("LoginFragment", "E-posta ve şifre girilmelidir")
            return
        }

        // TODO: Yükleniyor (Loading) göstergesi eklenebilir

        // Firebase'e "Giriş yap" komutunu gönder
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Giriş başarılı!
                    Toast.makeText(requireContext(), "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                    Log.d("LoginFragment", "Giriş başarılı!")

                    // Kullanıcıyı MainActivity'ye yönlendir
                    (activity as? AuthActivity)?.let {
                        val intent = Intent(it, MainActivity::class.java)
                        it.startActivity(intent)
                        it.finish() // AuthActivity'yi kapat
                    }
                } else {
                    // Giriş başarısız (Şifre yanlış, kullanıcı bulunamadı vb.)
                    Toast.makeText(requireContext(), "Giriş başarısız: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("LoginFragment", "Giriş başarısız: ${task.exception?.message}")
                }
                // TODO: Yükleniyor göstergesini gizle
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}