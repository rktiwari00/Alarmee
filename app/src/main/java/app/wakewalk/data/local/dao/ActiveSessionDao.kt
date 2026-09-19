package app.wakewalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.wakewalk.data.local.entity.ActiveSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {

    @Query("SELECT * FROM active_session WHERE singletonId = 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Query("SELECT * FROM active_session WHERE singletonId = 1")
    fun getActiveSessionFlow(): Flow<ActiveSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session WHERE singletonId = 1")
    suspend fun clearActiveSession()
}
