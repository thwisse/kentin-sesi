package io.github.thwisse.kentinsesi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.databinding.FragmentRegisterBinding
import io.github.thwisse.kentinsesi.ui.AuthActivity
import io.github.thwisse.kentinsesi.ui.MainActivity
import androidx.navigation.fragment.findNavController

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        auth = Firebase.auth

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            // Kayıt olma fonksiyonunu çağır
            registerUser()
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    // Yeni kayıt fonksiyonunu ekle
    private fun registerUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Basit kontroller (boş olmasın)
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Tüm alanlar doldurulmalıdır", Toast.LENGTH_SHORT).show()
            Log.d("RegisterFragment", "Tüm alanlar doldurulmalıdır")
            return
        }

        // TODO: Yükleniyor (Loading) göstergesi eklenebilir

        // Firebase'e "Yeni kullanıcı oluştur" komutunu gönder
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Kayıt başarılı!
                    // TODO: Kullanıcının Adı Soyadı (fullName) veritabanına kaydedilmeli (Milestone 6)

                    Toast.makeText(requireContext(), "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                    Log.d("RegisterFragment", "Kayıt başarılı!")

                    // Kullanıcıyı MainActivity'ye yönlendir
                    (activity as? AuthActivity)?.let {
                        val intent = Intent(it, MainActivity::class.java)
                        it.startActivity(intent)
                        it.finish() // AuthActivity'yi kapat
                    }

                } else {
                    // Kayıt başarısız oldu (Şifre çok kısa, e-posta formatı yanlış, e-posta zaten kullanımda vb.)
                    Toast.makeText(requireContext(), "Kayıt başarısız: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("RegisterFragment", "Kayıt başarısız: ${task.exception?.message}")
                }
                // TODO: Yükleniyor göstergesini gizle
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}