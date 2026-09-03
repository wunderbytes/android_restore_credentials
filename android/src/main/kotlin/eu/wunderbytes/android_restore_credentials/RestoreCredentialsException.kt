package eu.wunderbytes.android_restore_credentials

/**
 * Failure from [RestoreCredentialsClient]. [code] matches the Flutter
 * `PlatformException` codes so BackupAgent and Dart callers can branch the same way.
 */
class RestoreCredentialsException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val CODE_INVALID_ARGUMENT = "invalid_argument"
        const val CODE_UNSUPPORTED_ANDROID_VERSION = "unsupported_android_version"
        const val CODE_CREATE_DOM = "create_dom"
        const val CODE_E2EE_UNAVAILABLE = "e2ee_unavailable"
        const val CODE_NO_CREDENTIAL = "no_credential"
        const val CODE_GET_INTERRUPTED = "get_interrupted"
        const val CODE_GET_CANCELED = "get_canceled"
        const val CODE_GET_FAILED = "get_failed"
        const val CODE_CREATE_FAILED = "create_failed"
        const val CODE_CLEAR_FAILED = "clear_failed"
        const val CODE_UNEXPECTED_TYPE = "unexpected_type"
        const val CODE_UNKNOWN = "unknown"

        /** Restore Credentials requires Android 9 (API 28). */
        const val MIN_SDK_INT: Int = 28
    }
}
