package com.residentsleeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.residentsleeper.data.model.BabyProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyProfileDao {
    @Query("SELECT * FROM baby_profile ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<BabyProfile>>

    @Query("SELECT * FROM baby_profile ORDER BY id ASC")
    suspend fun getAllProfilesSync(): List<BabyProfile>

    @Query("SELECT * FROM baby_profile WHERE isActive = 1 LIMIT 1")
    fun getActiveProfileFlow(): Flow<BabyProfile?>

    @Query("SELECT * FROM baby_profile WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): BabyProfile?

    @Query("SELECT * FROM baby_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): BabyProfile?

    @Query("SELECT COUNT(*) FROM baby_profile")
    suspend fun getProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: BabyProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<BabyProfile>)

    @Update
    suspend fun update(profile: BabyProfile)

    @Delete
    suspend fun delete(profile: BabyProfile)

    @Query("UPDATE baby_profile SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE baby_profile SET isActive = 1 WHERE id = :profileId")
    suspend fun setActiveFlag(profileId: Long)

    @Transaction
    suspend fun setActiveProfile(profileId: Long) {
        clearActiveFlags()
        setActiveFlag(profileId)
    }

    @Query("DELETE FROM baby_profile")
    suspend fun deleteAll()
}
