package io.github.thwisse.kentinsesi.data.repository

import android.content.Context
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun loginUser(email: String, password: String): Resource<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Resource.Success(result)
        } catch (e: FirebaseAuthException) {
            Resource.Error(getAuthErrorMessage(e))
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.auth_error_login))
        }
    }

    override suspend fun registerUser(email: String, password: String): Resource<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Resource.Success(result)
        } catch (e: FirebaseAuthException) {
            Resource.Error(getAuthErrorMessage(e))
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.auth_error_register))
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override fun signOut() {
        auth.signOut()
    }
    
    private fun getAuthErrorMessage(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> context.getString(R.string.auth_error_invalid_email)
            "ERROR_WRONG_PASSWORD" -> context.getString(R.string.auth_error_wrong_password)
            "ERROR_USER_NOT_FOUND" -> context.getString(R.string.auth_error_user_not_found)
            "ERROR_USER_DISABLED" -> context.getString(R.string.auth_error_user_disabled)
            "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.auth_error_email_in_use)
            "ERROR_WEAK_PASSWORD" -> context.getString(R.string.auth_error_weak_password)
            "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.auth_error_network)
            "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.auth_error_too_many_requests)
            else -> exception.message ?: context.getString(R.string.auth_error_generic)
        }
    }
}