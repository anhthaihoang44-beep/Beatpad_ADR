package com.beatpad.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val filePath: String,
    val eventsJson: String
)
