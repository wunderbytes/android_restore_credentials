package eu.wunderbytes.android_restore_credentials

import android.content.Context
import android.os.Bundle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
internal class RestoreCredentialsClientTest {
    private lateinit var context: Context
    private lateinit var operations: FakeCredentialManagerOperations

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        operations = FakeCredentialManagerOperations()
    }

    @Test
    fun createRestoreKey_blankJson_throwsInvalidArgument() {
        val client = client()

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.createRestoreKey("  ") }
            }

        assertEquals(RestoreCredentialsException.CODE_INVALID_ARGUMENT, error.code)
        assertTrue(operations.createCloudFlags.isEmpty())
    }

    @Test
    fun createRestoreKey_api27_throwsUnsupported() {
        val client = client(sdkInt = 27)

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.createRestoreKey(VALID_CREATE_JSON) }
            }

        assertEquals(RestoreCredentialsException.CODE_UNSUPPORTED_ANDROID_VERSION, error.code)
        assertTrue(operations.createCloudFlags.isEmpty())
    }

    @Test
    fun createRestoreKey_retriesLocallyAfterE2eeUnavailable() {
        operations.createHandler = { request ->
            val restoreRequest = request as CreateRestoreCredentialRequest
            if (restoreRequest.isCloudBackupEnabled) {
                throw E2eeUnavailableException("backup off")
            }
            CreateRestoreCredentialResponse(CREATE_RESPONSE_JSON)
        }
        val client = client()

        val json =
            runBlocking {
                client.createRestoreKey(VALID_CREATE_JSON, preferCloudBackup = true)
            }

        assertEquals(CREATE_RESPONSE_JSON, json)
        assertEquals(listOf(true, false), operations.createCloudFlags)
    }

    @Test
    fun createRestoreKey_cloudSuccess_doesNotRetry() {
        operations.createHandler = {
            CreateRestoreCredentialResponse(CREATE_RESPONSE_JSON)
        }
        val client = client()

        val json =
            runBlocking {
                client.createRestoreKey(VALID_CREATE_JSON, preferCloudBackup = true)
            }

        assertEquals(CREATE_RESPONSE_JSON, json)
        assertEquals(listOf(true), operations.createCloudFlags)
    }

    @Test
    fun createRestoreKey_preferCloudBackupFalse_skipsCloud() {
        operations.createHandler = {
            CreateRestoreCredentialResponse(CREATE_RESPONSE_JSON)
        }
        val client = client()

        runBlocking {
            client.createRestoreKey(VALID_CREATE_JSON, preferCloudBackup = false)
        }

        assertEquals(listOf(false), operations.createCloudFlags)
    }

    @Test
    fun createRestoreKey_e2eeAfterRetry_mapsE2eeUnavailable() {
        operations.createHandler = {
            throw E2eeUnavailableException("still unavailable")
        }
        val client = client()

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.createRestoreKey(VALID_CREATE_JSON) }
            }

        assertEquals(RestoreCredentialsException.CODE_E2EE_UNAVAILABLE, error.code)
        assertEquals(listOf(true, false), operations.createCloudFlags)
    }

    @Test
    fun getRestoreKey_blankJson_throwsInvalidArgument() {
        val client = client()

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.getRestoreKey("") }
            }

        assertEquals(RestoreCredentialsException.CODE_INVALID_ARGUMENT, error.code)
    }

    @Test
    fun getRestoreKey_returnsAuthenticationJson() {
        operations.getHandler = {
            GetCredentialResponse(restoreCredential(GET_RESPONSE_JSON))
        }
        val client = client()

        val json = runBlocking { client.getRestoreKey(VALID_GET_JSON) }

        assertEquals(GET_RESPONSE_JSON, json)
        assertEquals(1, operations.getRequestCount)
        val options = operations.lastGetRequest!!.credentialOptions
        assertEquals(1, options.size)
        assertEquals(RestoreCredential.TYPE_RESTORE_CREDENTIAL, options.single().type)
    }

    @Test
    fun getRestoreKey_unexpectedType() {
        operations.getHandler = {
            GetCredentialResponse(CustomCredential("other.type", Bundle()))
        }
        val client = client()

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.getRestoreKey(VALID_GET_JSON) }
            }

        assertEquals(RestoreCredentialsException.CODE_UNEXPECTED_TYPE, error.code)
    }

    @Test
    fun getRestoreKey_noCredential() {
        operations.getHandler = { throw NoCredentialException("none") }
        val client = client()

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.getRestoreKey(VALID_GET_JSON) }
            }

        assertEquals(RestoreCredentialsException.CODE_NO_CREDENTIAL, error.code)
    }

    @Test
    fun clearRestoreKey_usesRestoreClearType() {
        val client = client()

        runBlocking { client.clearRestoreKey() }

        assertEquals(
            ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL,
            operations.lastClearRequest!!.requestType,
        )
    }

    @Test
    fun clearRestoreKey_api27_throwsUnsupported() {
        val client = client(sdkInt = 27)

        val error =
            assertFailsWith<RestoreCredentialsException> {
                runBlocking { client.clearRestoreKey() }
            }

        assertEquals(RestoreCredentialsException.CODE_UNSUPPORTED_ANDROID_VERSION, error.code)
        assertEquals(null, operations.lastClearRequest)
    }

    private fun client(sdkInt: Int = 28): RestoreCredentialsClient =
        RestoreCredentialsClient(
            context = context,
            operations = operations,
            sdkInt = sdkInt,
        )

    private fun restoreCredential(authenticationResponseJson: String): Credential {
        val data = Bundle()
        data.putString(
            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE",
            authenticationResponseJson,
        )
        return Credential.createFrom(RestoreCredential.TYPE_RESTORE_CREDENTIAL, data)
    }

    private class FakeCredentialManagerOperations : CredentialManagerOperations {
        val createCloudFlags = mutableListOf<Boolean>()
        var createHandler: (CreateCredentialRequest) -> CreateCredentialResponse = {
            error("createHandler not set")
        }
        var getHandler: (GetCredentialRequest) -> GetCredentialResponse = {
            error("getHandler not set")
        }
        var getRequestCount: Int = 0
        var lastGetRequest: GetCredentialRequest? = null
        var lastClearRequest: ClearCredentialStateRequest? = null

        override suspend fun createCredential(
            context: Context,
            request: CreateCredentialRequest,
        ): CreateCredentialResponse {
            val restoreRequest = request as CreateRestoreCredentialRequest
            createCloudFlags += restoreRequest.isCloudBackupEnabled
            return createHandler(request)
        }

        override suspend fun getCredential(
            context: Context,
            request: GetCredentialRequest,
        ): GetCredentialResponse {
            getRequestCount += 1
            lastGetRequest = request
            return getHandler(request)
        }

        override suspend fun clearCredentialState(request: ClearCredentialStateRequest) {
            lastClearRequest = request
        }
    }

    companion object {
        private const val VALID_CREATE_JSON =
            """{"challenge":"dGVzdA","rp":{"name":"Example","id":"example.com"},"user":{"id":"dXNlcg","name":"user","displayName":"User"},"pubKeyCredParams":[{"type":"public-key","alg":-7}]}"""
        private const val VALID_GET_JSON =
            """{"challenge":"dGVzdA","rpId":"example.com"}"""
        private const val CREATE_RESPONSE_JSON = """{"id":"cred","rawId":"cred","type":"public-key","response":{}}"""
        private const val GET_RESPONSE_JSON = """{"id":"cred","rawId":"cred","type":"public-key","response":{}}"""
    }
}
