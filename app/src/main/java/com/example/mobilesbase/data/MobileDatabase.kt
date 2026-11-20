package com.example.mobilesbase.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MobileDao {
    @Query("SELECT * FROM mobile_cards ORDER BY id DESC")
    fun getAllCards(): Flow<List<MobileCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: MobileCard)

    @Update
    suspend fun updateCard(card: MobileCard)

    @Delete
    suspend fun deleteCard(card: MobileCard)
}

@Database(entities = [MobileCard::class], version = 3, exportSchema = false)
abstract class MobileDatabase : RoomDatabase() {
    abstract fun mobileDao(): MobileDao

    companion object {
        @Volatile
        private var INSTANCE: MobileDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mobile_cards ADD COLUMN billingDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): MobileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MobileDatabase::class.java,
                    "mobiles_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
