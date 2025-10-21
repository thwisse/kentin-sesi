package io.github.thwisse.kentinsesi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.thwisse.kentinsesi.data.repository.AuthRepository // Interface importu
import io.github.thwisse.kentinsesi.data.repository.AuthRepositoryImpl // Implementasyon importu
import io.github.thwisse.kentinsesi.data.repository.UserRepository
import io.github.thwisse.kentinsesi.data.repository.UserRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds // Hilt'e AuthRepository istendiğinde AuthRepositoryImpl vermesini söyler
    @Singleton // Repository'lerin de tek örnek olması genellikle iyidir
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    // TODO: Adım X'te PostRepository için @Binds metodu buraya eklenecek.
}