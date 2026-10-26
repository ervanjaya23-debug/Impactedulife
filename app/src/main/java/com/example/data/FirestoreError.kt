package com.example.data

import android.util.Log
import kotlinx.coroutines.CancellationException

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
    if (error is CancellationException) return
    Log.e(
        "FirestoreError",
        """{"event": "firestore_error", "operation": "${operation.name}", "path": "$path", "error": "${error.localizedMessage ?: error.message}"}"""
    )
}
