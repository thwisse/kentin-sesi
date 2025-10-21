package io.github.thwisse.kentinsesi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module // Hilt'e bunun bir modül olduğunu söyler
@InstallIn(SingletonComponent::class) // Bu modüldeki bağımlılıkların uygulama ömrü boyunca yaşayacağını söyler (Singleton)
object FirebaseModule {

    @Provides // Hilt'e bir bağımlılığın nasıl sağlanacağını öğretir
    @Singleton // Bu nesnenin uygulama boyunca sadece bir kez oluşturulacağını garantiler
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth // Firebase.auth nesnesini döndür
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore // Firebase.firestore nesnesini döndür
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return Firebase.storage // Firebase.storage nesnesini döndür
    }
}