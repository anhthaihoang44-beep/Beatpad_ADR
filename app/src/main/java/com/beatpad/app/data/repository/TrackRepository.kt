package com.beatpad.app.data.repository

import com.beatpad.app.data.local.TrackDao
import com.beatpad.app.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

interface TrackRepository {
    val allTracks: Flow<List<TrackEntity>>
    suspend fun getTrackById(id: Long): TrackEntity?
    suspend fun insertTrack(track: TrackEntity): Long
    suspend fun updateTrack(track: TrackEntity)
    suspend fun deleteTrack(track: TrackEntity)
    suspend fun renameTrack(id: Long, newName: String)
}

class RoomTrackRepository(
    private val trackDao: TrackDao
) : TrackRepository {

    override val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()

    override suspend fun getTrackById(id: Long): TrackEntity? = withContext(Dispatchers.IO) {
        trackDao.getTrackById(id)
    }

    override suspend fun insertTrack(track: TrackEntity): Long = withContext(Dispatchers.IO) {
        trackDao.insertTrack(track)
    }

    override suspend fun updateTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        trackDao.updateTrack(track)
    }

    override suspend fun deleteTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(track.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        trackDao.deleteTrack(track)
    }

    override suspend fun renameTrack(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(id) ?: return@withContext
        trackDao.updateTrack(existing.copy(name = newName))
    }
}
