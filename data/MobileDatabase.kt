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
    
    @Query("UPDATE mobile_cards SET balance = :newBalance WHERE phoneNumber = :phoneNumber")
    suspend fun updateBalance(phoneNumber: String, newBalance: Double)
}

@Database(entities = [MobileCard::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
