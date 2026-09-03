package eu.wunderbytes.android_restore_credentials

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/*
 * Unit tests for the Kotlin portion of this plugin's implementation.
 *
 * These cover method routing and argument validation. The Credential Manager
 * work is exercised by `RestoreCredentialsClientTest` with a fake
 * `CredentialManager`. Here we inject a fake [RestoreCredentialsClient] so the
 * plugin never calls `CredentialManager.create`.
 *
 * Run with `./gradlew :android_restore_credentials:testDebugUnitTest` from the
 * `example/android` directory.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
internal class AndroidRestoreCredentialsPluginTest {
    @Before
    fun setUp() {
        // Run coroutines launched on Dispatchers.Main eagerly so the plugin's
        // scope (Main.immediate) completes before the test asserts.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onMethodCall_unknownMethod_invokesNotImplemented() {
        val plugin = AndroidRestoreCredentialsPlugin()

        val call = MethodCall("bogus", null)
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        Mockito.verify(mockResult).notImplemented()
    }

    @Test
    fun onMethodCall_createRestoreKey_missingRequestJson_returnsInvalidArgument() {
        val plugin = AndroidRestoreCredentialsPlugin()

        val call = MethodCall("createRestoreKey", mapOf<String, Any>("preferCloudBackup" to true))
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        val captor = org.mockito.ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(mockResult).error(captor.capture(), Mockito.anyString(), Mockito.any())
        assertEquals(RestoreCredentialsException.CODE_INVALID_ARGUMENT, captor.value)
    }

    @Test
    fun onMethodCall_getRestoreKey_missingRequestJson_returnsInvalidArgument() {
        val plugin = AndroidRestoreCredentialsPlugin()

        val call = MethodCall("getRestoreKey", null)
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        val captor = org.mockito.ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(mockResult).error(captor.capture(), Mockito.anyString(), Mockito.any())
        assertEquals(RestoreCredentialsException.CODE_INVALID_ARGUMENT, captor.value)
    }

    @Test
    fun onMethodCall_clearRestoreKey_delegatesToClient() {
        val fakeClient = FakeClient()
        val plugin = AndroidRestoreCredentialsPlugin().apply { setClientForTest(fakeClient) }

        val call = MethodCall("clearRestoreKey", null)
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        Mockito.verify(mockResult, Mockito.timeout(1000)).success(null)
        assertEquals(1, fakeClient.clearCalls)
    }

    @Test
    fun onMethodCall_createRestoreKey_delegatesToClient() {
        val fakeClient = FakeClient(createResult = "attestation-json")
        val plugin = AndroidRestoreCredentialsPlugin().apply { setClientForTest(fakeClient) }

        val call = MethodCall(
            "createRestoreKey",
            mapOf<String, Any>(
                "requestJson" to "create-json",
                "preferCloudBackup" to false,
            ),
        )
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        Mockito.verify(mockResult, Mockito.timeout(1000)).success("attestation-json")
        assertEquals("create-json", fakeClient.lastCreateRequestJson)
        assertEquals(false, fakeClient.lastCreatePreferCloudBackup)
    }

    @Test
    fun onMethodCall_getRestoreKey_delegatesToClient() {
        val fakeClient = FakeClient(getResult = "assertion-json")
        val plugin = AndroidRestoreCredentialsPlugin().apply { setClientForTest(fakeClient) }

        val call = MethodCall("getRestoreKey", mapOf<String, Any>("requestJson" to "get-json"))
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        Mockito.verify(mockResult, Mockito.timeout(1000)).success("assertion-json")
        assertEquals("get-json", fakeClient.lastGetRequestJson)
    }

    @Test
    fun onMethodCall_createRestoreKey_mapsClientExceptionToErrorCode() {
        val fakeClient = FakeClient(
            createException = RestoreCredentialsException(
                RestoreCredentialsException.CODE_CREATE_DOM,
                "bad json",
            ),
        )
        val plugin = AndroidRestoreCredentialsPlugin().apply { setClientForTest(fakeClient) }

        val call = MethodCall(
            "createRestoreKey",
            mapOf<String, Any>("requestJson" to "create-json"),
        )
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)
        plugin.onMethodCall(call, mockResult)

        val captor = org.mockito.ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(mockResult, Mockito.timeout(1000))
            .error(captor.capture(), Mockito.anyString(), Mockito.any())
        assertEquals(RestoreCredentialsException.CODE_CREATE_DOM, captor.value)
    }

    private open class FakeClient(
        private val createResult: String = "create-response",
        private val getResult: String = "get-response",
        private val createException: RestoreCredentialsException? = null,
    ) : RestoreCredentialsClient(
            context = RuntimeEnvironment.getApplication(),
            operations = NoopCredentialManagerOperations,
            sdkInt = 28,
        ) {
        var lastCreateRequestJson: String? = null
        var lastCreatePreferCloudBackup: Boolean? = null
        var lastGetRequestJson: String? = null
        var clearCalls: Int = 0

        override suspend fun createRestoreKey(
            requestJson: String,
            preferCloudBackup: Boolean,
        ): String {
            lastCreateRequestJson = requestJson
            lastCreatePreferCloudBackup = preferCloudBackup
            createException?.let { throw it }
            return createResult
        }

        override suspend fun getRestoreKey(requestJson: String): String {
            lastGetRequestJson = requestJson
            return getResult
        }

        override suspend fun clearRestoreKey() {
            clearCalls += 1
        }
    }

    private object NoopCredentialManagerOperations : CredentialManagerOperations {
        override suspend fun createCredential(
            context: android.content.Context,
            request: androidx.credentials.CreateCredentialRequest,
        ): androidx.credentials.CreateCredentialResponse =
            throw UnsupportedOperationException("FakeClient overrides all methods")

        override suspend fun getCredential(
            context: android.content.Context,
            request: androidx.credentials.GetCredentialRequest,
        ): androidx.credentials.GetCredentialResponse =
            throw UnsupportedOperationException("FakeClient overrides all methods")

        override suspend fun clearCredentialState(
            request: androidx.credentials.ClearCredentialStateRequest,
        ) = throw UnsupportedOperationException("FakeClient overrides all methods")
    }
}
