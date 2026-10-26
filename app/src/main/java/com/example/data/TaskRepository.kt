package com.example.data

import android.content.Context
import com.example.R
import com.example.model.SubmissionStatus
import com.example.model.Task
import com.example.model.TaskScope
import com.example.model.TaskSubmission
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TaskRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    // Observe tasks within a school by scope (STUDENT_TASK, TEACHER_TASK)
    fun observeSchoolTasks(schoolId: String, scope: String): Flow<List<Task>> = flow {
        val path = "tasks"
        db.collection(path)
            .whereEqualTo("schoolId", schoolId)
            .whereEqualTo("scope", scope)
            .snapshots()
            .map { it.toObjects(Task::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    // Observe personal tasks created by student for themselves
    fun observePersonalTasks(userId: String): Flow<List<Task>> = flow {
        val path = "tasks"
        db.collection(path)
            .whereEqualTo("createdBy", userId)
            .whereEqualTo("scope", TaskScope.PERSONAL_TASK.name)
            .snapshots()
            .map { it.toObjects(Task::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    suspend fun createTask(task: Task): String = withContext(NonCancellable) {
        val uid = requireUserId()
        val path = "tasks"
        val docRef = db.collection(path).document()
        val data = hashMapOf(
            "schoolId" to task.schoolId,
            "createdBy" to uid,
            "creatorRole" to task.creatorRole,
            "creatorName" to task.creatorName,
            "scope" to task.scope,
            "title" to task.title,
            "description" to task.description,
            "subjectOrCategory" to task.subjectOrCategory,
            "targetFilter" to task.targetFilter,
            "targetSchoolName" to task.targetSchoolName,
            "priority" to task.priority,
            "dueDate" to task.dueDate,
            "reminderOffsets" to task.reminderOffsets,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String) = withContext(NonCancellable) {
        val path = "tasks/$taskId"
        try {
            db.collection("tasks").document(taskId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // Observe user submissions to map completed status
    fun observeUserSubmissions(userId: String): Flow<List<TaskSubmission>> = flow {
        val path = "task_submissions"
        db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(TaskSubmission::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    // Observe all submissions in a school for teacher / principal analytics
    fun observeSchoolSubmissions(schoolId: String): Flow<List<TaskSubmission>> = flow {
        val path = "task_submissions"
        db.collection(path)
            .whereEqualTo("schoolId", schoolId)
            .snapshots()
            .map { it.toObjects(TaskSubmission::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    suspend fun markTaskCompleted(
        taskId: String,
        userId: String,
        userName: String,
        schoolId: String,
        userRole: String,
        isOntime: Boolean
    ) = withContext(NonCancellable) {
        val submissionId = "${taskId}_${userId}"
        val path = "task_submissions/$submissionId"
        val docRef = db.collection("task_submissions").document(submissionId)
        val data = hashMapOf(
            "taskId" to taskId,
            "userId" to userId,
            "userName" to userName,
            "schoolId" to schoolId,
            "userRole" to userRole,
            "status" to if (isOntime) SubmissionStatus.COMPLETED.name else SubmissionStatus.LATE.name,
            "isOntime" to isOntime,
            "completedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }
}
