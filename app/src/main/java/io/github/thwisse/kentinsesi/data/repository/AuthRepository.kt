package io.github.thwisse.kentinsesi.data.repository

import com.google.firebase.auth.AuthResult // Başarılı sonuç için
import com.google.firebase.auth.FirebaseUser // Mevcut kullanıcı için

// Authentication işlemleri için sözleşme (interface)
interface AuthRepository {

    // Mevcut giriş yapmış kullanıcıyı döndürür (varsa)
    val currentUser: FirebaseUser?

    // E-posta ve şifre ile giriş yapma (Coroutine ile asenkron)
    suspend fun loginUser(email: String, password: String): Result<AuthResult>

    // Yeni kullanıcı kaydetme (Coroutine ile asenkron)
    suspend fun registerUser(email: String, password: String): Result<AuthResult>

    // Çıkış yapma
    fun logout()
}