package com.streamlytv.data.repository

import android.content.Context
import androidx.room.*
import com.streamlytv.data.model.*
import com.streamlytv.data.model.ScanQueueItem
import kotlinx.coroutines.flow.Flow

// ─── Channel DAO ───────────────────────────────────────────────────────────────
@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE `group` = :group ORDER BY name ASC")
    fun getByGroup(group: String): Flow<List<Channel>>

    @Query("SELECT DISTINCT `group` FROM channels ORDER BY `group` ASC")
    fun getAllGroups(): Flow<List<String>>

    @Query("""
        SELECT * FROM channels 
        WHERE (:query = '' OR name LIKE '%' || :query || '%')
        AND (:language = '' OR language = :language OR :language = 'All')
        AND (:only4K = 0 OR is4K = 1)
        AND (:only51 = 0 OR is51 = 1)
        ORDER BY name ASC
    """)
    fun searchChannels(
        query: String = "",
        language: String = "All",
        only4K: Boolean = false,
        only51: Boolean = false
    ): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM channels")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int
}

// ─── Playlist DAO ──────────────────────────────────────────────────────────────
@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY id ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("UPDATE playlists SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean)
}

// ─── Stream Metadata DAO ──────────────────────────────────────────────────────
@Dao
interface StreamMetadataDao {

    @Query("SELECT * FROM stream_metadata WHERE streamId = :streamId")
    suspend fun get(streamId: String): StreamMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: StreamMetadata)

    @Query("SELECT * FROM stream_metadata WHERE is4K = 1")
    fun getAll4K(): Flow<List<StreamMetadata>>

    @Query("SELECT * FROM stream_metadata WHERE is51 = 1")
    fun getAll51(): Flow<List<StreamMetadata>>
}

// ─── VOD State DAO ────────────────────────────────────────────────────────────
@Dao
interface VodStateDao {

    @Query("SELECT * FROM vod_state WHERE streamId = :streamId")
    suspend fun get(streamId: String): VodState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: VodState)

    @Query("UPDATE vod_state SET resumePositionMs = :posMs, totalDurationMs = :durMs, lastPlayedAt = :time WHERE streamId = :streamId")
    suspend fun updateProgress(streamId: String, posMs: Long, durMs: Long, time: Long)

    @Query("UPDATE vod_state SET isWatched = 1, resumePositionMs = 0 WHERE streamId = :streamId")
    suspend fun markWatched(streamId: String)

    @Query("UPDATE vod_state SET isLiked = :liked, isDisliked = :disliked WHERE streamId = :streamId")
    suspend fun setRating(streamId: String, liked: Boolean, disliked: Boolean)

    @Query("SELECT * FROM vod_state WHERE isLiked = 1 ORDER BY lastPlayedAt DESC")
    fun getLiked(): Flow<List<VodState>>

    @Query("SELECT * FROM vod_state WHERE isWatched = 1 ORDER BY lastPlayedAt DESC")
    fun getWatched(): Flow<List<VodState>>

    @Query("SELECT * FROM vod_state ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecent(): Flow<List<VodState>>

    @Query("SELECT * FROM vod_state WHERE streamId = :streamId")
    fun observe(streamId: String): Flow<VodState?>
}

// ─── Scan Queue DAO ───────────────────────────────────────────────────────────
@Dao
interface ScanQueueDao {

    // Insert new items, ignore if already exists
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ScanQueueItem>)

    // Fetch next batch to scan: pending only, newest content first, max 30 days old
    @Query("""
        SELECT * FROM scan_queue 
        WHERE scanStatus = 'pending'
        AND addedTimestamp >= :cutoffTimestamp
        AND scanAttempts < 3
        ORDER BY addedTimestamp DESC
        LIMIT :batchSize
    """)
    suspend fun getNextBatch(cutoffTimestamp: Long, batchSize: Int = 20): List<ScanQueueItem>

    @Query("UPDATE scan_queue SET scanStatus = :status, scanAttempts = scanAttempts + 1, lastAttemptAt = :time WHERE streamId = :streamId")
    suspend fun updateStatus(streamId: Int, status: String, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM scan_queue WHERE scanStatus = 'pending' AND addedTimestamp >= :cutoff")
    suspend fun pendingCount(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM scan_queue WHERE scanStatus = 'scanned'")
    suspend fun scannedCount(): Int

    @Query("SELECT COUNT(*) FROM scan_queue")
    suspend fun totalCount(): Int
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [
        Channel::class,
        Playlist::class,
        StreamMetadata::class,
        VodState::class,
        ScanQueueItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun streamMetadataDao(): StreamMetadataDao
    abstract fun vodStateDao(): VodStateDao
    abstract fun scanQueueDao(): ScanQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streamlytv.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
