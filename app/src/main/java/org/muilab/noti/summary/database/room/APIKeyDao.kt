package org.muilab.noti.summary.database.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.muilab.noti.summary.model.APIKeyEntity


@Dao
interface APIKeyDao {
    @Query("SELECT APIKey FROM api_key_pool")
    fun getAllAPI(): Flow<List<String>>

    @Query("SELECT APIKey FROM api_key_pool")
    fun getAllAPIStatic(): List<String>

    @Query("SELECT * FROM api_key_pool WHERE APIKey = :apiKey")
    fun getAPIByAPIKey(apiKey: String): APIKeyEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAPIKey(api: APIKeyEntity)

    @Transaction
    fun insertAPIKeyIfNotExists(api: APIKeyEntity) {
        val existingAPI = getAPIByAPIKey(apiKey = api.APIKey)
        if (existingAPI == null) {
            insertAPIKey(api)
        }
    }

    @Query("DELETE FROM api_key_pool WHERE APIKey = :api")
    fun deleteByAPIKey(api: String)

    @Query("DELETE FROM api_key_pool")
    fun deleteAllAPIKey()
}