package io.github.thwisse.kentinsesi.ui.auth // Paket adını kontrol et

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Coroutine'ler için
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel // Hilt için
import io.github.thwisse.kentinsesi.data.repository.AuthRepository
import io.github.thwisse.kentinsesi.data.repository.UserRepository
import kotlinx.coroutines.launch // Coroutine başlatmak için
import javax.inject.Inject // Hilt enjeksiyonu için

// UI durumlarını temsil etmek için basit bir Sealed Class (veya Enum kullanabilirsiniz)
sealed class AuthState {
    object Idle : AuthState() // Başlangıç durumu
    object Loading : AuthState() // İşlem yapılıyor
    data class Success(val authResult: AuthResult) : AuthState() // Başarılı
    data class Error(val message: String) : AuthState() // Hata
}

@HiltViewModel // Hilt'e bu ViewModel'i nasıl oluşturacağını ve sağlayacağını söyler
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository, // Hilt, RepositoryModule sayesinde bunu enjekte edecek
    private val userRepository: UserRepository  // Hilt, RepositoryModule sayesinde bunu enjekte edecek
) : ViewModel() {

    // --- Giriş Durumu (Login State) ---
    // _loginState: Sadece ViewModel içinden değiştirilebilir (Mutable)
    private val _loginState = MutableLiveData<AuthState>(AuthState.Idle)

    // loginState: Fragment'ların dinleyeceği, değiştirilemez (Immutable) versiyon
    val loginState: LiveData<AuthState> = _loginState

    // --- Kayıt Durumu (Registration State) ---
    private val _registrationState = MutableLiveData<AuthState>(AuthState.Idle)
    val registrationState: LiveData<AuthState> = _registrationState

    // Giriş yapma fonksiyonu
    fun loginUser(email: String, password: String) {
        // ViewModelScope: Bu Coroutine, ViewModel yaşadığı sürece yaşar.
        viewModelScope.launch {
            _loginState.value = AuthState.Loading // Durumu 'Yükleniyor' yap
            val result = authRepository.loginUser(email, password)
            result.onSuccess { authResult ->
                _loginState.value = AuthState.Success(authResult) // Başarılı
            }.onFailure { exception ->
                _loginState.value =
                    AuthState.Error(exception.message ?: "Bilinmeyen giriş hatası") // Hata
            }
        }
    }

    // Kayıt olma fonksiyonu
    fun registerUser(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _registrationState.value = AuthState.Loading // Durumu 'Yükleniyor' yap
            // 1. Firebase Auth'a kaydet
            val registerResult = authRepository.registerUser(email, password)

            registerResult.onSuccess { authResult ->
                // 2. Auth başarılıysa, Firestore'a profili kaydet
                val user = authResult.user // Yeni kaydedilen kullanıcıyı al
                if (user != null) {
                    val profileResult = userRepository.createUserProfile(user.uid, fullName, email)
                    profileResult.onSuccess {
                        // Hem Auth hem Profil kaydı başarılı
                        _registrationState.value = AuthState.Success(authResult)
                    }.onFailure { profileException ->
                        // Profil kaydı başarısız (Auth başarılıydı ama profil yazılamadı)
                        // TODO: Bu durumu daha iyi yönetmek gerekebilir (örn: Auth kaydını geri almak?)
                        _registrationState.value =
                            AuthState.Error(profileException.message ?: "Profil oluşturma hatası")
                    }
                } else {
                    // Auth sonucu başarılı ama user nesnesi null geldi (beklenmedik durum)
                    _registrationState.value = AuthState.Error("Kullanıcı bilgisi alınamadı.")
                }
            }.onFailure { authException ->
                // Auth kaydı başarısız oldu
                _registrationState.value =
                    AuthState.Error(authException.message ?: "Bilinmeyen kayıt hatası")
            }
        }
    }
}