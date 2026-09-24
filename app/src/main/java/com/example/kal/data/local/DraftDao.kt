// In file: data/local/DraftDao.kt
package com.example.kal.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    // 1. CHANGE THIS: Make 'saveDraft' return the new ID (as a Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: Draft): Long // <-- CHANGE 'suspend fun saveDraft(draft: Draft)' TO THIS

    @Query("SELECT * FROM drafts ORDER BY id DESC")
    fun getMyDrafts(): Flow<List<Draft>>

    // 2. ADD THIS: A new function to get one draft by its ID
    @Query("SELECT * FROM drafts WHERE id = :id")
    fun getDraftById(id: Int): Flow<Draft?> // <-- ADD THIS NEW FUNCTION

    @Delete
    suspend fun deleteDraft(draft: Draft)
}