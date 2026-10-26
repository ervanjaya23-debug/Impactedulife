package com.example.data

import android.content.Context
import com.example.R
import com.example.model.User
import com.example.model.UserStatus
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

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User?) : ProfileState()
}

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    fun observeUserProfileState(userId: String): Flow<ProfileState> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                val user = if (snapshot.exists()) snapshot.toObject(User::class.java) else null
                ProfileState.Success(user) as ProfileState
            }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(ProfileState.Success(null))
            }
    }

    fun observeUser(userId: String): Flow<User?> = flow {
        val path = "users/$userId"
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(User::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(user: User) = withContext(NonCancellable) {
        val path = "users/${user.id}"
        val docRef = db.collection("users").document(user.id)
        val data = hashMapOf(
            "email" to user.email,
            "displayName" to user.displayName,
            "photoUrl" to user.photoUrl,
            "schoolId" to user.schoolId,
            "schoolName" to user.schoolName,
            "role" to user.role,
            "status" to user.status,
            "className" to user.className,
            "subjectSpecialty" to user.subjectSpecialty,
            "managedSchools" to user.managedSchools,
            "managedClasses" to user.managedClasses,
            "streakCount" to user.streakCount,
            "onTimeStreakCount" to user.onTimeStreakCount,
            "bestStreakCount" to user.bestStreakCount,
            "totalOnTimeCompleted" to user.totalOnTimeCompleted,
            "totalTasksCompleted" to user.totalTasksCompleted,
            "lastOnTimeTaskDate" to user.lastOnTimeTaskDate,
            "lastActiveDate" to user.lastActiveDate,
            "createdAt" to (user.createdAt ?: FieldValue.serverTimestamp())
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

    suspend fun updateUserProfileAndSchool(
        userId: String,
        schoolId: String,
        displayName: String,
        schoolName: String,
        photoUrl: String,
        subjectSpecialty: String = "",
        managedSchools: List<String>? = null,
        managedClasses: List<String>? = null
    ) = withContext(NonCancellable) {
        val path = "users/$userId"
        val cleanName = displayName.trim()
        val cleanSchoolName = schoolName.trim()
        val updates = mutableMapOf<String, Any>(
            "displayName" to cleanName,
            "schoolName" to cleanSchoolName,
            "photoUrl" to photoUrl
        )
        if (subjectSpecialty.isNotBlank()) {
            updates["subjectSpecialty"] = subjectSpecialty.trim()
        }
        if (managedSchools != null) {
            val cleanedSchools = managedSchools.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            updates["managedSchools"] = cleanedSchools
        }
        if (managedClasses != null) {
            val cleanedClasses = managedClasses.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            updates["managedClasses"] = cleanedClasses
            if (cleanedClasses.isNotEmpty()) {
                updates["className"] = cleanedClasses.joinToString(", ")
            }
        }
        try {
            db.collection("users").document(userId)
                .update(updates)
                .await()

            if (schoolId.isNotBlank() && cleanSchoolName.isNotBlank()) {
                try {
                    db.collection("schools").document(schoolId)
                        .update("name", cleanSchoolName)
                        .await()
                } catch (_: Exception) {
                    // Best-effort sync to schools document
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun updateManagedSchools(
        userId: String,
        schoolId: String,
        activeSchoolName: String,
        managedSchools: List<String>
    ) = withContext(NonCancellable) {
        val path = "users/$userId"
        val cleaned = managedSchools.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val primarySchool = activeSchoolName.trim().ifBlank { cleaned.firstOrNull() ?: "" }
        val updates = mutableMapOf<String, Any>(
            "managedSchools" to cleaned
        )
        if (primarySchool.isNotBlank()) {
            updates["schoolName"] = primarySchool
        }
        try {
            db.collection("users").document(userId).update(updates).await()
            if (schoolId.isNotBlank() && primarySchool.isNotBlank()) {
                try {
                    db.collection("schools").document(schoolId).update("name", primarySchool).await()
                } catch (_: Exception) {}
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun updateManagedClasses(
        userId: String,
        managedClasses: List<String>
    ) = withContext(NonCancellable) {
        val path = "users/$userId"
        val cleaned = managedClasses.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val updates = mapOf(
            "managedClasses" to cleaned,
            "className" to cleaned.joinToString(", ")
        )
        try {
            db.collection("users").document(userId).update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    fun observePendingUsers(schoolId: String): Flow<List<User>> = flow {
        val path = "users"
        db.collection(path)
            .whereEqualTo("schoolId", schoolId)
            .whereEqualTo("status", UserStatus.PENDING.name)
            .snapshots()
            .map { it.toObjects(User::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    fun observeSchoolMembers(schoolId: String): Flow<List<User>> = flow {
        val path = "users"
        db.collection(path)
            .whereEqualTo("schoolId", schoolId)
            .whereEqualTo("status", UserStatus.ACTIVE.name)
            .snapshots()
            .map { it.toObjects(User::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
            .collect { emit(it) }
    }

    suspend fun approveMember(userId: String) = withContext(NonCancellable) {
        val path = "users/$userId"
        try {
            db.collection("users").document(userId)
                .update("status", UserStatus.ACTIVE.name)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun rejectMember(userId: String) = withContext(NonCancellable) {
        val path = "users/$userId"
        try {
            db.collection("users").document(userId)
                .update("status", UserStatus.SUSPENDED.name)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun findUserByEmail(email: String): User? {
        val path = "users"
        return try {
            val query = db.collection(path)
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1)
                .get()
                .await()
            query.toObjects(User::class.java).firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun addUserByEmail(
        email: String,
        displayName: String,
        role: String,
        schoolId: String,
        schoolName: String,
        className: String,
        subjectSpecialty: String
    ): String = withContext(NonCancellable) {
        val path = "users"
        val docRef = db.collection(path).document()
        val data = hashMapOf(
            "email" to email.trim().lowercase(),
            "displayName" to displayName.trim(),
            "role" to role,
            "status" to UserStatus.ACTIVE.name, // Pre-approved by Principal / Admin
            "schoolId" to schoolId,
            "schoolName" to schoolName,
            "className" to className,
            "subjectSpecialty" to subjectSpecialty,
            "streakCount" to 1L,
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

    suspend fun recordActiveStreak(userId: String) = withContext(NonCancellable) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val path = "users/$userId"
        try {
            val doc = db.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java) ?: return@withContext
            if (user.lastActiveDate != todayStr) {
                val newStreak = user.streakCount + 1L
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "streakCount" to newStreak,
                            "lastActiveDate" to todayStr
                        )
                    ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun checkAndLinkPreAddedUser(
        uid: String,
        email: String,
        displayName: String?
    ): User? = withContext(NonCancellable) {
        if (email.isBlank()) return@withContext null
        val preAdded = findUserByEmail(email) ?: return@withContext null
        if (preAdded.id != uid) {
            val linkedUser = preAdded.copy(
                id = uid,
                displayName = if (!displayName.isNullOrBlank()) displayName else preAdded.displayName,
                status = UserStatus.ACTIVE.name
            )
            saveUserProfile(linkedUser)
            if (preAdded.id.isNotBlank() && preAdded.id != uid) {
                try {
                    db.collection("users").document(preAdded.id).delete().await()
                } catch (e: Exception) {
                    // Ignore deletion error of temporary placeholder
                }
            }
            return@withContext linkedUser
        }
        return@withContext preAdded
    }

    suspend fun recordOnTimeTaskCompletion(
        userId: String,
        isOntime: Boolean
    ): DisciplineResult = withContext(NonCancellable) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val path = "users/$userId"
        try {
            val doc = db.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java) ?: return@withContext DisciplineResult(0, isOntime, 0, false, 0, 0, false, "")
            val oldTier = user.getDisciplineTier()

            val newTotalTasks = user.totalTasksCompleted + 1L
            val oldStreak = user.currentDisciplineStreak
            val (newStreak, newTotalOnTime, streakBroken) = if (isOntime) {
                val updated = oldStreak + 1L
                Triple(updated, user.totalOnTimeCompleted + 1L, false)
            } else {
                Triple(0L, user.totalOnTimeCompleted, oldStreak > 0L)
            }
            val newBestStreak = maxOf(user.bestStreakCount, newStreak)

            db.collection("users").document(userId)
                .update(
                    mapOf(
                        "onTimeStreakCount" to newStreak,
                        "streakCount" to newStreak,
                        "bestStreakCount" to newBestStreak,
                        "totalOnTimeCompleted" to newTotalOnTime,
                        "totalTasksCompleted" to newTotalTasks,
                        "lastOnTimeTaskDate" to if (isOntime) todayStr else user.lastOnTimeTaskDate,
                        "lastActiveDate" to todayStr
                    )
                ).await()

            val updatedUser = user.copy(
                onTimeStreakCount = newStreak,
                streakCount = newStreak,
                bestStreakCount = newBestStreak,
                totalOnTimeCompleted = newTotalOnTime,
                totalTasksCompleted = newTotalTasks
            )
            val newTier = updatedUser.getDisciplineTier()
            val isNewTier = isOntime && (newTier.name != oldTier.name)

            DisciplineResult(
                currentStreak = newStreak,
                isOntime = isOntime,
                bestStreak = newBestStreak,
                streakBroken = streakBroken,
                totalOnTime = newTotalOnTime,
                totalTasks = newTotalTasks,
                isNewTierReached = isNewTier,
                newTierName = newTier.name
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            DisciplineResult(0, isOntime, 0, false, 0, 0, false, "")
        }
    }
}

data class DisciplineResult(
    val currentStreak: Long = 0L,
    val isOntime: Boolean = false,
    val bestStreak: Long = 0L,
    val streakBroken: Boolean = false,
    val totalOnTime: Long = 0L,
    val totalTasks: Long = 0L,
    val isNewTierReached: Boolean = false,
    val newTierName: String = ""
)

