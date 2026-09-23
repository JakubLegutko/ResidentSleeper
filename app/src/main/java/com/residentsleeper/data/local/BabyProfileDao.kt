package com.residentsleeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.residentsleeper.data.model.BabyProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyProfileDao {
    @Query("SELECT * FROM baby_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<BabyProfile?>

    @Query("SELECT * FROM baby_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): BabyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: BabyProfile)

    @Update
    suspend fun update(profile: BabyProfile)
}
