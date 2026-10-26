package com.example.data

import android.content.Context
import com.example.R
import com.example.model.School
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

class SchoolRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun observeSchools(): Flow<List<School>> = flow {
        val path = "schools"
        db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { it.toObjects(School::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    suspend fun findSchoolByInviteCode(inviteCode: String): School? {
        val path = "schools"
        return try {
            val query = db.collection(path)
                .whereEqualTo("inviteCode", inviteCode.trim().uppercase())
                .limit(1)
                .get()
                .await()
            query.toObjects(School::class.java).firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun getSchoolById(schoolId: String): School? {
        val path = "schools/$schoolId"
        return try {
            val doc = db.collection("schools").document(schoolId).get().await()
            doc.toObject(School::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun createSchool(name: String, inviteCode: String, principalName: String, city: String): String = withContext(NonCancellable) {
        val path = "schools"
        val docRef = db.collection(path).document()
        val data = hashMapOf(
            "name" to name,
            "inviteCode" to inviteCode.trim().uppercase(),
            "principalName" to principalName,
            "city" to city,
            "isActive" to true,
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
}
