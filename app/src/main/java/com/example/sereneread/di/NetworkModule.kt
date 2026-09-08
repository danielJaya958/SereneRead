package com.example.sereneread.di

import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.data.remote.network.AuthInterceptor
import com.example.sereneread.domain.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // PENTING: Gunakan 10.0.2.2 untuk mengakses localhost komputer dari Emulator Android
    // Jangan lupa diakhiri dengan garis miring (/)
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        // Ini agar kita bisa melihat request & response API di Logcat (warna hijau/biru)
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)      // Pasang satpam token
            .addInterceptor(loggingInterceptor)   // Pasang pencatat log
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Converter JSON ke Data Class
            .build()
    }

    @Provides
    @Singleton
    fun provideSereneReadApi(retrofit: Retrofit): SereneReadApi {
        return retrofit.create(SereneReadApi::class.java)
    }
}