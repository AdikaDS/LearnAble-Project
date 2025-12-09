package com.adika.learnable.util

import com.adika.learnable.model.User

sealed class GoogleSignInResult {
    data class Success(val user: User) : GoogleSignInResult()
    data class NeedsMoreData(val user: User, val requiredFields: List<String>) : GoogleSignInResult()
}
