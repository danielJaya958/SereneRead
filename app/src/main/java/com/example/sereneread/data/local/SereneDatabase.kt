package com.example.sereneread.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.local.entity.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 4,
    exportSchema = false // Ubah true jika ingin export schema JSON (opsional)
)
@TypeConverters(Converters::class) // Daftarkan converter di sini
abstract class SereneDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        // v2: tambah kolom progressSynced untuk menandai progres yang belum terkirim ke server
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN progressSynced INTEGER NOT NULL DEFAULT 1")
            }
        }

        // v3: tambah kolom lastReadAt untuk menentukan buku yang terakhir dibaca
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastReadAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v4: tahun terbit dan waktu selesai dibaca (dari backend)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN publishedYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE books ADD COLUMN finishedAt INTEGER")
            }
        }
    }

    // Kita tidak perlu membuat Instance Singleton manual di sini
    // Karena kita akan menggunakan Hilt untuk membuatnya (Dependency Injection).
}