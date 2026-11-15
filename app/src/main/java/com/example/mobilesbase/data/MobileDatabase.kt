package com.example.mobilesbase.data

import android.content.Context
import androidx.room.*
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

@Database(entities = [MobileCard::class], version = 2, exportSchema = false)
abstract class MobileDatabase : RoomDatabase() {
    abstract fun mobileDao(): MobileDao

    companion object {
        @Volatile
        private var INSTANCE: MobileDatabase? = null

        fun getDatabase(context: Context): MobileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MobileDatabase::class.java,
                    "mobiles_database"
                ).fallbackToDestructiveMigration().build() // Destructive migration OK for personal app
                INSTANCE = instance
                instance
            }
        }
    }
}
