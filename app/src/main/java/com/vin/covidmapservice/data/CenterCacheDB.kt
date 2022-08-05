package com.vin.covidmapservice.data;

import android.content.Context
import androidx.room.Room
import com.vin.covidmapservice.domain.Center
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Helper / exposed API for Room DB
class CenterCacheDB {
    private var dao: CenterCacheDAO? = null
    constructor(context: Context?) { // Pass null as context to create the 'dummy' DB which does nothing thanks to the ?. operator (they're nice for testing stuffs)
        if (context != null) {
            val db = Room.databaseBuilder(context, CenterCacheDBAbstract::class.java, "CenterCacheDB").build()
            dao = db.centerCacheDAO()
        }
    }

    // getAll() returns the live(flow) list of center, so whenever the DB changes (i.e. getting cached),
    // the composables that accesses the list of center (collected within the viewmodel) will be automatically notified & re-composed
    fun getAll(): Flow<List<Center>> = dao?.getAll() ?: flow { emit(listOf<Center>()) }
    fun deleteAll() = dao?.deleteAll()
    fun insert(vararg centers: Center) = dao?.insert(*centers) // (*centers => center[0], center[1], center[2]...)
    fun delete(center: Center) = dao?.delete(center) // (not really used but added per traditional CRUD operation & in case of later use)
    fun getCount(): Int = dao?.getCount() ?: 0
}
