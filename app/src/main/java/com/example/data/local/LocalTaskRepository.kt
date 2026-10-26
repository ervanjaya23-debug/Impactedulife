package com.example.data.local

import kotlinx.coroutines.flow.Flow

class LocalTaskRepository(private val taskDao: TaskDao) {

    fun getTasksBySchool(schoolId: String): Flow<List<TaskEntity>> =
        taskDao.getTasksBySchool(schoolId)

    fun getTasksByScope(schoolId: String, scope: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByScope(schoolId, scope)

    fun getTasksAssignedTo(schoolId: String, userEmailOrId: String): Flow<List<TaskEntity>> =
        taskDao.getTasksAssignedTo(schoolId, userEmailOrId)

    suspend fun saveTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun saveTasks(tasks: List<TaskEntity>) {
        taskDao.insertTasks(tasks)
    }

    suspend fun updateStatus(id: String, status: String) {
        taskDao.updateTaskStatus(id, status)
    }

    suspend fun delete(id: String) {
        taskDao.deleteTask(id)
    }
}
