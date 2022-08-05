package com.vin.covidmapservice.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.vin.covidmapservice.domain.Center
import kotlinx.coroutines.flow.Flow

// Room DAO
@Dao
interface CenterCacheDAO {
    @Query("SELECT * FROM $DB_TABLE_NAME")
    fun getAll(): Flow<List<Center>>
    @Query("DELETE FROM $DB_TABLE_NAME")
    fun deleteAll()
    @Insert
    fun insert(vararg centers: Center)
    @Delete
    fun delete(center: Center)
    @androidx.room.Query("SELECT COUNT(id) FROM $DB_TABLE_NAME")
    fun getCount(): Int
}