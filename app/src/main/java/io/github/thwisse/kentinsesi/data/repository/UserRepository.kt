package io.github.thwisse.kentinsesi.data.repository

import io.github.thwisse.kentinsesi.data.model.User

// Kullanıcı profili işlemleri için sözleşme
interface UserRepository {

    // Firestore'a yeni kullanıcı profili oluşturma
    suspend fun createUserProfile(uid: String, fullName: String, email: String): Result<Unit>

    // Mevcut kullanıcının profilini Firestore'dan getirme (ileride lazım olacak)
    // suspend fun getUserProfile(uid: String): Result<User?>
}