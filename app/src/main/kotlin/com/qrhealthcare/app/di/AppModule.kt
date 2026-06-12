package com.qrhealthcare.app.di

import com.qrhealthcare.app.data.api.ApiClient
import com.qrhealthcare.app.data.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService = ApiClient.apiService

    // SessionManager and AppRepository are @Singleton with @Inject constructors,
    // so Hilt auto-creates them. No manual @Provides needed.
}
