package io.github.thwisse.kentinsesi.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await // Firebase Task'lerini Coroutine'e çevirmek için
import javax.inject.Inject          // Hilt'in FirebaseAuth'u buraya enjekte etmesi için

// AuthRepository interface'inin Firebase ile çalışan somut implementasyonu
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth // Hilt, FirebaseModule sayesinde bunu sağlayacak
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun loginUser(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
                .await() // await() ile Coroutine içinde bekletiyoruz
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerUser(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
    }
}