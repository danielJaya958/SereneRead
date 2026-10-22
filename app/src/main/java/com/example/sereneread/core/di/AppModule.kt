package com.example.sereneread.core.di

import android.app.Application
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.Room
import com.example.sereneread.data.local.SereneDatabase
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.data.repository.BookRepositoryImpl
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.usecase.AddBookUseCase
import com.example.sereneread.domain.usecase.BookUseCases
import com.example.sereneread.domain.usecase.GetBookDetailsUseCase
import com.example.sereneread.domain.usecase.GetBooksUseCase
import com.example.sereneread.domain.usecase.ToggleFavoriteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Modul ini hidup selama aplikasi hidup
object AppModule {

    // 1. Sediakan Database Instance
    @Provides
    @Singleton
    fun provideDatabase(app: Application): SereneDatabase {
        return Room.databaseBuilder(
            app,
            SereneDatabase::class.java,
            "serene_read_db"
        )
            .addMigrations(SereneDatabase.MIGRATION_1_2, SereneDatabase.MIGRATION_2_3, SereneDatabase.MIGRATION_3_4)
            .build()
    }

    // 2. Sediakan DAO
    @Provides
    @Singleton
    fun provideBookDao(db: SereneDatabase): BookDao {
        return db.bookDao()
    }

    // 3. Sediakan Repository
    // Hilt akan melihat BookRepositoryImpl butuh BookDao,
    // dan Hilt sudah tahu cara bikin BookDao dari fungsi di atas.
    @Provides
    @Singleton
    fun provideBookRepository(
        db: SereneDatabase,
        dao: BookDao,
        api: SereneReadApi, // 1. Tambahkan parameter ini agar Hilt menyediakannya
        @ApplicationContext context: Context // Dibutuhkan WorkManager untuk sinkronisasi progres
    ): BookRepository {
        return BookRepositoryImpl(db, dao, api, context) // 2. Masukkan ke dalam constructor
    }

    // 4. Sediakan Use Cases
    @Provides
    @Singleton
    fun provideBookUseCases(repository: BookRepository): BookUseCases {
        return BookUseCases(
            getBooks = GetBooksUseCase(repository),
            getBook = GetBookDetailsUseCase(repository),
            addBook = AddBookUseCase(repository),
            toggleFavorite = ToggleFavoriteUseCase(repository)
        )
    }
}