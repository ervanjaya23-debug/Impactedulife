package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Nudge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NudgeRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    suspend fun getRemainingNudgesToday(toUserId: String, taskId: String): Int {
        val uid = requireUserId()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val path = "nudges"
        return try {
            val query = db.collection(path)
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("taskId", taskId)
                .whereEqualTo("dateKey", todayStr)
                .get()
                .await()
            val sentCount = query.size()
            val maxAllowed = 3
            (maxAllowed - sentCount).coerceAtLeast(0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            3
        }
    }

    suspend fun sendNudge(nudge: Nudge): Boolean = withContext(NonCancellable) {
        val uid = requireUserId()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Check quota first
        val remaining = getRemainingNudgesToday(nudge.toUserId, nudge.taskId)
        if (remaining <= 0) {
            return@withContext false
        }

        val path = "nudges"
        val docRef = db.collection(path).document()
        val data = hashMapOf(
            "schoolId" to nudge.schoolId,
            "fromUserId" to uid,
            "fromUserName" to nudge.fromUserName,
            "fromRole" to nudge.fromRole,
            "toUserId" to nudge.toUserId,
            "toUserName" to nudge.toUserName,
            "toRole" to nudge.toRole,
            "nudgeType" to nudge.nudgeType,
            "taskId" to nudge.taskId,
            "taskTitle" to nudge.taskTitle,
            "templateText" to nudge.templateText,
            "dateKey" to todayStr,
            "sentAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    fun observeIncomingNudges(userId: String): Flow<List<Nudge>> = flow {
        val path = "nudges"
        db.collection(path)
            .whereEqualTo("toUserId", userId)
            .snapshots()
            .map { it.toObjects(Nudge::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }
}
