package com.iamconanpeter.kidsblockbuddy.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WorldSaveRepository(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false }
) {
    private val saveFile = File(context.filesDir, "world_primary.json")
    private val backupFile = File(context.filesDir, "world_backup_last_good.json")

    suspend fun load(): WorldSnapshot = withContext(Dispatchers.IO) {
        if (!saveFile.exists()) return@withContext WorldSnapshot.default()

        runCatching {
            json.decodeFromString<WorldSnapshot>(saveFile.readText())
        }.getOrElse {
            runCatching {
                json.decodeFromString<WorldSnapshot>(backupFile.readText())
            }.getOrElse {
                WorldSnapshot.default()
            }
        }
    }

    suspend fun save(snapshot: WorldSnapshot) = withContext(Dispatchers.IO) {
        val temp = File(context.filesDir, "world_primary.tmp")
        temp.writeText(json.encodeToString(snapshot.copy(updatedAtEpochMs = System.currentTimeMillis())))

        if (saveFile.exists()) {
            saveFile.copyTo(backupFile, overwrite = true)
        }

        if (saveFile.exists()) {
            saveFile.delete()
        }
        temp.renameTo(saveFile)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        saveFile.delete()
        backupFile.delete()
    }
}
