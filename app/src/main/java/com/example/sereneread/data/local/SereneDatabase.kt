package com.example.sereneread.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.local.entity.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = false // Ubah true jika ingin export schema JSON (opsional)
)
@TypeConverters(Converters::class) // Daftarkan converter di sini
abstract class SereneDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    // Kita tidak perlu membuat Instance Singleton manual di sini
    // Karena kita akan menggunakan Hilt untuk membuatnya (Dependency Injection).
}