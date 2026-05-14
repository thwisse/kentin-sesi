package io.github.thwisse.kentinsesi.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.thwisse.kentinsesi.R
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.UserRepository
import io.github.thwisse.kentinsesi.util.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI Durumları
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val authResult: AuthResult) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- Giriş Durumu ---
    private val _loginState = MutableLiveData<AuthState>(AuthState.Idle)
    val loginState: LiveData<AuthState> = _loginState

    // --- Kayıt Durumu ---
    private val _registrationState = MutableLiveData<AuthState>(AuthState.Idle)
    val registrationState: LiveData<AuthState> = _registrationState

    // --- Profil Güncelleme Durumu (YENİ) ---
    // Resource<Unit> kullanıyoruz çünkü geriye veri dönmüyor, sadece başarı/hata önemli.
    private val _updateProfileState = MutableLiveData<Resource<Unit>>()
    val updateProfileState: LiveData<Resource<Unit>> = _updateProfileState

    // GİRİŞ YAP
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            val result = authRepository.loginUser(email, password)
            
            // Artık Resource<T> kullanıyoruz, Result<T> değil
            when (result) {
                is Resource.Success -> {
                    result.data?.let { authResult ->
                        _loginState.value = AuthState.Success(authResult)
                    } ?: run {
                        _loginState.value = AuthState.Error(context.getString(R.string.login_success_no_data))
                    }
                }
                is Resource.Error -> {
                    _loginState.value = AuthState.Error(result.message ?: context.getString(R.string.login_error))
                }
                is Resource.Loading -> {
                    // Zaten Loading durumundayız
                }
            }
        }
    }

    // KAYIT OL
    fun registerUser(email: String, password: String) {
        viewModelScope.launch {
            _registrationState.value = AuthState.Loading

            // 1. Auth İşlemi (Artık Resource dönüyor)
            val authResult = authRepository.registerUser(email, password)

            when (authResult) {
                is Resource.Success -> {
                    val authData = authResult.data
                    val user = authData?.user
                    
                    if (user != null) {
                        // 2. Profil Oluşturma
                        val profileResource = userRepository.createUserProfile(user.uid, "", email)

                        when (profileResource) {
                            is Resource.Success -> {
                                // AuthResult'ı geri döndürmek için authData'yı kullan
                                authData?.let {
                                    _registrationState.value = AuthState.Success(it)
                                } ?: run {
                                    _registrationState.value = AuthState.Error(context.getString(R.string.registration_success_no_data))
                                }
                            }
                            is Resource.Error -> {
                                _registrationState.value = AuthState.Error(profileResource.message ?: context.getString(R.string.profile_error))
                            }
                            is Resource.Loading -> {
                                // Loading durumu zaten başta set edildi
                            }
                        }
                    } else {
                        _registrationState.value = AuthState.Error(context.getString(R.string.user_data_not_found))
                    }
                }
                is Resource.Error -> {
                    _registrationState.value = AuthState.Error(authResult.message ?: context.getString(R.string.registration_error))
                }
                is Resource.Loading -> {
                    // Zaten Loading durumundayız
                }
            }
        }
    }

    // PROFİL TAMAMLAMA (YENİ)
    fun completeProfile(fullName: String, username: String, city: String, district: String, avatarSeed: String) {
        viewModelScope.launch {
            // Resource.Loading<Unit> olarak tip belirtiyoruz (Hata: Cannot infer type çözümü)
            _updateProfileState.value = Resource.Loading()

            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                val result = userRepository.updateUserProfile(currentUser.uid, fullName, username, city, district, avatarSeed)
                _updateProfileState.value = result
            } else {
                // Resource.Error<Unit> olarak tip belirtiyoruz
                _updateProfileState.value = Resource.Error(context.getString(R.string.user_session_not_found))
            }
        }
    }
}