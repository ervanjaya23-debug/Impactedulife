package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM local_tasks WHERE schoolId = :schoolId ORDER BY deadline ASC")
    fun getTasksBySchool(schoolId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM local_tasks WHERE schoolId = :schoolId AND scope = :scope ORDER BY deadline ASC")
    fun getTasksByScope(schoolId: String, scope: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM local_tasks WHERE schoolId = :schoolId AND (assignedTeacherOrUser = :userEmailOrId OR assignedTeacherOrUser = 'ALL' OR assignedTeacherOrUser = 'ALL_TEACHERS') ORDER BY deadline ASC")
    fun getTasksAssignedTo(schoolId: String, userEmailOrId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE local_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String)

    @Query("DELETE FROM local_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}
