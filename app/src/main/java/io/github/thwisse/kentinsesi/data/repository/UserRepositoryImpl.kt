package io.github.thwisse.kentinsesi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import io.github.thwisse.kentinsesi.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// UserRepository interface'inin Firestore ile çalışan somut implementasyonu
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore // Hilt, FirebaseModule sayesinde bunu sağlayacak
) : UserRepository {

    companion object {
        private const val USERS_COLLECTION = "users" // Koleksiyon adını sabit olarak tanımlayalım
    }

    override suspend fun createUserProfile(
        uid: String,
        fullName: String,
        email: String
    ): Result<Unit> {
        return try {
            // Yeni bir User nesnesi oluştur
            val newUser = User(
                uid = uid,
                fullName = fullName,
                email = email,
                role = "citizen" // Varsayılan rol
                // points ve title zaten varsayılan değerlere sahip
            )
            // Firestore'daki "users" koleksiyonuna, kullanıcının uid'sini doküman ID'si olarak kullanarak yaz.
            firestore.collection(USERS_COLLECTION).document(uid).set(newUser).await()
            Result.success(Unit) // Başarılı, geriye bir şey döndürmeye gerek yok
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // İleride getUserProfile fonksiyonunu buraya ekleyeceğiz...
}