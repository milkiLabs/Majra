package com.milki.majra.data.repository

sealed interface SyncResult {
    data class Success(val username: String, val postCount: Int) : SyncResult
    data class Failure(val message: String) : SyncResult
}
