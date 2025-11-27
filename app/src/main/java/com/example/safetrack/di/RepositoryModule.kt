package com.example.safetrack.di

import com.example.safetrack.auth.data.repository.AuthRepositoryImpl
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.data.repository.SessionRepositoryImpl
import com.example.safetrack.core.domain.repository.SessionRepository
import com.example.safetrack.profile.data.repository.ProfileRepositoryImpl
import com.example.safetrack.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(sessionRepositoryImpl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(profileRepositoryImpl: ProfileRepositoryImpl): ProfileRepository
}