package io.github.thwisse.kentinsesi.util

import android.content.Context
import io.github.thwisse.kentinsesi.R

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    fun isValidPostTitle(title: String): Boolean {
        return title.isNotBlank() && title.length <= Constants.MAX_POST_TITLE_LENGTH
    }

    fun isValidPostDescription(description: String): Boolean {
        return description.isNotBlank() && description.length <= Constants.MAX_POST_DESCRIPTION_LENGTH
    }

    fun isValidComment(comment: String): Boolean {
        return comment.isNotBlank() && comment.length <= Constants.MAX_COMMENT_LENGTH
    }

    fun getValidationError(field: String, value: String, context: Context): String? {
        return when (field) {
            "email" -> if (!isValidEmail(value)) context.getString(R.string.validation_email_invalid) else null
            "password" -> if (!isValidPassword(value)) context.getString(R.string.validation_password_min_length) else null
            "title" -> if (!isValidPostTitle(value)) context.getString(R.string.validation_title_max_length) else null
            "description" -> if (!isValidPostDescription(value)) context.getString(R.string.validation_description_max_length) else null
            "comment" -> if (!isValidComment(value)) context.getString(R.string.validation_comment_max_length) else null
            else -> null
        }
    }
}
