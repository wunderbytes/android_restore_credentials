package eu.wunderbytes.android_restore_credentials

import android.content.Context
import android.os.Build
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Public Credential Manager wrapper for Restore Credentials.
 *
 * Host [android.app.backup.BackupAgent] code can call this without a Flutter
 * engine. Prefer [android.content.Context.getApplicationContext] from UI code.
 */
open class RestoreCredentialsClient internal constructor(
    private val context: Context,
    private val operations: CredentialManagerOperations,
    private val sdkInt: Int,
) {
    // Class is `open` so tests can substitute a fake client in the plugin.
    constructor(context: Context) : this(
        context = context,
        operations = CredentialManagerCredentialOperations(CredentialManager.create(context)),
        sdkInt = Build.VERSION.SDK_INT,
    )

    /**
     * Creates a restore key. Tries cloud backup first when [preferCloudBackup]
     * is true, then retries locally on [E2eeUnavailableException].
     *
     * @param requestJson WebAuthn `PublicKeyCredentialCreationOptionsJSON`
     * @return `CreateRestoreCredentialResponse.responseJson` (attestation)
     */
    open suspend fun createRestoreKey(
        requestJson: String,
        preferCloudBackup: Boolean = true,
    ): String {
        ensureSupportedSdk()
        ensureRequestJson(requestJson)
        return try {
            val response = createWithE2eeRetry(requestJson, preferCloudBackup)
            val restoreResponse =
                response as? CreateRestoreCredentialResponse
                    ?: throw RestoreCredentialsException(
                        RestoreCredentialsException.CODE_UNEXPECTED_TYPE,
                        "Expected CreateRestoreCredentialResponse, got ${response.javaClass.simpleName}",
                    )
            restoreResponse.responseJson
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestoreCredentialsException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_INVALID_ARGUMENT)
        } catch (e: E2eeUnavailableException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_E2EE_UNAVAILABLE)
        } catch (e: CreateRestoreCredentialDomException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_CREATE_DOM)
        } catch (e: CreateCredentialException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_CREATE_FAILED)
        } catch (e: Exception) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_UNKNOWN)
        }
    }

    /**
     * Retrieves a restore key. The get request contains only
     * [GetRestoreCredentialOption].
     *
     * @param requestJson WebAuthn `PublicKeyCredentialRequestOptionsJSON`
     * @return [RestoreCredential.authenticationResponseJson]
     */
    open suspend fun getRestoreKey(requestJson: String): String {
        ensureSupportedSdk()
        ensureRequestJson(requestJson)
        return try {
            val option = GetRestoreCredentialOption(requestJson)
            val getRequest = GetCredentialRequest(listOf(option))
            val response = operations.getCredential(context, getRequest)
            val credential = response.credential
            val restore =
                credential as? RestoreCredential
                    ?: throw RestoreCredentialsException(
                        RestoreCredentialsException.CODE_UNEXPECTED_TYPE,
                        "Expected RestoreCredential, got ${credential.javaClass.simpleName}",
                    )
            restore.authenticationResponseJson
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestoreCredentialsException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_INVALID_ARGUMENT)
        } catch (e: NoCredentialException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_NO_CREDENTIAL)
        } catch (e: GetCredentialCancellationException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_GET_CANCELED)
        } catch (e: GetCredentialInterruptedException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_GET_INTERRUPTED)
        } catch (e: GetCredentialException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_GET_FAILED)
        } catch (e: Exception) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_UNKNOWN)
        }
    }

    /**
     * Deletes the restore key on this device. Uses
     * [ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL]; a typeless
     * clear does not remove restore keys.
     */
    open suspend fun clearRestoreKey() {
        ensureSupportedSdk()
        try {
            val clearRequest =
                ClearCredentialStateRequest(
                    ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL,
                )
            operations.clearCredentialState(clearRequest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestoreCredentialsException) {
            throw e
        } catch (e: ClearCredentialException) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_CLEAR_FAILED)
        } catch (e: Exception) {
            throw e.toRestoreCredentialsException(RestoreCredentialsException.CODE_UNKNOWN)
        }
    }

    private suspend fun createWithE2eeRetry(
        requestJson: String,
        preferCloudBackup: Boolean,
    ): CreateCredentialResponse {
        if (!preferCloudBackup) {
            return create(requestJson, isCloudBackupEnabled = false)
        }
        return try {
            create(requestJson, isCloudBackupEnabled = true)
        } catch (e: E2eeUnavailableException) {
            create(requestJson, isCloudBackupEnabled = false)
        }
    }

    private suspend fun create(
        requestJson: String,
        isCloudBackupEnabled: Boolean,
    ): CreateCredentialResponse {
        val request =
            CreateRestoreCredentialRequest(
                requestJson = requestJson,
                isCloudBackupEnabled = isCloudBackupEnabled,
            )
        return operations.createCredential(context, request)
    }

    private fun ensureSupportedSdk() {
        if (sdkInt < RestoreCredentialsException.MIN_SDK_INT) {
            throw RestoreCredentialsException(
                RestoreCredentialsException.CODE_UNSUPPORTED_ANDROID_VERSION,
                "Restore Credentials requires API ${RestoreCredentialsException.MIN_SDK_INT}+ (Android 9). This device is API $sdkInt.",
            )
        }
    }

    private fun ensureRequestJson(requestJson: String) {
        if (requestJson.isBlank()) {
            throw RestoreCredentialsException(
                RestoreCredentialsException.CODE_INVALID_ARGUMENT,
                "requestJson must not be blank",
            )
        }
    }
}

internal interface CredentialManagerOperations {
    suspend fun createCredential(
        context: Context,
        request: CreateCredentialRequest,
    ): CreateCredentialResponse

    suspend fun getCredential(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse

    suspend fun clearCredentialState(request: ClearCredentialStateRequest)
}

private class CredentialManagerCredentialOperations(
    private val credentialManager: CredentialManager,
) : CredentialManagerOperations {
    override suspend fun createCredential(
        context: Context,
        request: CreateCredentialRequest,
    ): CreateCredentialResponse = credentialManager.createCredential(context, request)

    override suspend fun getCredential(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse = credentialManager.getCredential(context, request)

    override suspend fun clearCredentialState(request: ClearCredentialStateRequest) {
        credentialManager.clearCredentialState(request)
    }
}

private fun Throwable.toRestoreCredentialsException(code: String): RestoreCredentialsException {
    val typeName = javaClass.simpleName
    val detail = message
    val mappedMessage =
        if (detail.isNullOrBlank()) {
            typeName
        } else {
            "$typeName: $detail"
        }
    return RestoreCredentialsException(code, mappedMessage, this)
}
