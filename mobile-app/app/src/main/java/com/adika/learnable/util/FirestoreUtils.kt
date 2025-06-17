package com.adika.learnable.util

import com.google.firebase.Timestamp

/**
 * Utility class untuk konversi data Firestore
 */
object FirestoreUtils {
    /**
     * Konversi Timestamp ke Long (milliseconds)
     */
    fun Timestamp.toLong(): Long = this.seconds * 1000 + this.nanoseconds / 1000000

    /**
     * Konversi Long (milliseconds) ke Timestamp
     */
    fun Long.toTimestamp(): Timestamp = Timestamp(this / 1000, ((this % 1000) * 1000000).toInt())

    /**
     * Konversi Map<String, Any> ke Map<String, String>
     * Digunakan untuk Notification.data yang perlu di-Parcelize
     */
    fun Map<String, Any>.toStringMap(): Map<String, String> {
        return this.mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> value.toString()
            }
        }
    }

    /**
     * Konversi Map<String, String> ke Map<String, Any>
     * Digunakan saat menyimpan Notification.data ke Firestore
     */
    fun Map<String, String>.toAnyMap(): Map<String, Any> {
        return this.mapValues { (_, value) ->
            when {
                value.toIntOrNull() != null -> value.toInt()
                value.toFloatOrNull() != null -> value.toFloat()
                value.toBooleanStrictOrNull() != null -> value.toBooleanStrict()
                else -> value
            }
        }
    }
} 