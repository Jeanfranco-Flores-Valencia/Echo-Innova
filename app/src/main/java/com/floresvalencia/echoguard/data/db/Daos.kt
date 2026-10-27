package com.floresvalencia.echoguard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_account ORDER BY active DESC, role, name")
    fun observeAll(): Flow<List<StaffAccountEntity>>

    @Query("SELECT * FROM staff_account WHERE pinHash = :pinHash AND active = 1 LIMIT 1")
    suspend fun findActiveByPinHash(pinHash: String): StaffAccountEntity?

    @Query("SELECT * FROM staff_account WHERE username = :username")
    suspend fun find(username: String): StaffAccountEntity?

    @Query("SELECT COUNT(*) FROM staff_account WHERE pinHash = :pinHash AND username != :exceptUsername")
    suspend fun countWithPin(pinHash: String, exceptUsername: String = ""): Int

    @Query("SELECT COUNT(*) FROM staff_account WHERE role = :role AND active = 1")
    suspend fun countActive(role: String): Int

    @Query("SELECT COUNT(*) FROM staff_account")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: StaffAccountEntity)

    @Update
    suspend fun update(account: StaffAccountEntity)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_log ORDER BY timeMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<AuditEntity>>

    @Insert
    suspend fun insert(entry: AuditEntity)

    /** Conserva solo las últimas [keep] entradas. */
    @Query("DELETE FROM audit_log WHERE id NOT IN (SELECT id FROM audit_log ORDER BY id DESC LIMIT :keep)")
    suspend fun trim(keep: Int = 2_000)
}

@Dao
interface FeedbackDao {
    @Insert
    suspend fun insert(feedback: FeedbackEntity): Long

    @Insert
    suspend fun insertTasks(tasks: List<TaskResultEntity>)

    @Transaction
    suspend fun insertWithTasks(feedback: FeedbackEntity, tasks: List<TaskResultEntity>): Long {
        val id = insert(feedback)
        insertTasks(tasks.map { it.copy(feedbackId = id) })
        return id
    }

    @Transaction
    @Query("SELECT * FROM feedback ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FeedbackWithTasks>>

    @Transaction
    @Query("SELECT * FROM feedback WHERE synced = 0 ORDER BY createdAt")
    suspend fun pending(): List<FeedbackWithTasks>

    @Query("UPDATE feedback SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM feedback")
    suspend fun deleteAll()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM emergency_contact ORDER BY createdAt")
    fun observeAll(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contact ORDER BY createdAt")
    suspend fun all(): List<EmergencyContactEntity>

    @Insert
    suspend fun insert(contact: EmergencyContactEntity): Long

    @Query("DELETE FROM emergency_contact WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SosDao {
    @Query("SELECT * FROM sos_log ORDER BY timeMillis DESC LIMIT 20")
    fun observeRecent(): Flow<List<SosLogEntity>>

    @Insert
    suspend fun insert(entry: SosLogEntity): Long

    @Query("UPDATE sos_log SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)
}
