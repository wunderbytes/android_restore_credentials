package eu.wunderbytes.android_restore_credentials

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** AndroidRestoreCredentialsPlugin */
class AndroidRestoreCredentialsPlugin :
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var appContext: Context
    private var client: RestoreCredentialsClient? = null

    // Lazily created so the plugin can be constructed in plain JVM tests that
    // only exercise routing / argument validation (which return before any
    // coroutine is launched). Dispatchers.Main requires a Looper.
    private val scope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    /** Internal hook for tests to inject a prebuilt [client]. */
    internal fun setClientForTest(client: RestoreCredentialsClient) {
        this.client = client
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        appContext = flutterPluginBinding.applicationContext
        channel =
            MethodChannel(flutterPluginBinding.binaryMessenger, "android_restore_credentials")
        channel.setMethodCallHandler(this)
        if (client == null) {
            client = RestoreCredentialsClient(appContext)
        }
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result,
    ) {
        when (call.method) {
            "createRestoreKey" -> handleCreate(call, result)
            "getRestoreKey" -> handleGet(call, result)
            "clearRestoreKey" -> handleClear(result)
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (::channel.isInitialized) {
            channel.setMethodCallHandler(null)
            // Cancel any in-flight coroutines. `scope` may not have been
            // created if no handler ever launched a coroutine.
            runCatching { scope.cancel() }
        }
    }

    private fun handleCreate(
        call: MethodCall,
        result: Result,
    ) {
        val requestJson = call.argument<String>("requestJson")
        if (requestJson.isNullOrBlank()) {
            result.error(
                RestoreCredentialsException.CODE_INVALID_ARGUMENT,
                "requestJson must not be blank",
                null,
            )
            return
        }
        val preferCloudBackup = call.argument<Boolean>("preferCloudBackup") ?: true
        val c = client ?: return notAttached(result)

        scope.launch {
            try {
                val response =
                    withContext(Dispatchers.IO) {
                        c.createRestoreKey(
                            requestJson = requestJson,
                            preferCloudBackup = preferCloudBackup,
                        )
                    }
                result.success(response)
            } catch (e: RestoreCredentialsException) {
                result.error(e.code, e.message ?: e.javaClass.simpleName, null)
            } catch (e: Exception) {
                result.error(
                    RestoreCredentialsException.CODE_UNKNOWN,
                    e.message ?: e.javaClass.simpleName,
                    null,
                )
            }
        }
    }

    private fun handleGet(
        call: MethodCall,
        result: Result,
    ) {
        val requestJson = call.argument<String>("requestJson")
        if (requestJson.isNullOrBlank()) {
            result.error(
                RestoreCredentialsException.CODE_INVALID_ARGUMENT,
                "requestJson must not be blank",
                null,
            )
            return
        }
        val c = client ?: return notAttached(result)

        scope.launch {
            try {
                val response =
                    withContext(Dispatchers.IO) {
                        c.getRestoreKey(requestJson = requestJson)
                    }
                result.success(response)
            } catch (e: RestoreCredentialsException) {
                result.error(e.code, e.message ?: e.javaClass.simpleName, null)
            } catch (e: Exception) {
                result.error(
                    RestoreCredentialsException.CODE_UNKNOWN,
                    e.message ?: e.javaClass.simpleName,
                    null,
                )
            }
        }
    }

    private fun handleClear(result: Result) {
        val c = client ?: return notAttached(result)

        scope.launch {
            try {
                withContext(Dispatchers.IO) { c.clearRestoreKey() }
                result.success(null)
            } catch (e: RestoreCredentialsException) {
                result.error(e.code, e.message ?: e.javaClass.simpleName, null)
            } catch (e: Exception) {
                result.error(
                    RestoreCredentialsException.CODE_UNKNOWN,
                    e.message ?: e.javaClass.simpleName,
                    null,
                )
            }
        }
    }

    private fun notAttached(result: Result) {
        result.error(
            RestoreCredentialsException.CODE_UNKNOWN,
            "Plugin is not attached to the Flutter engine",
            null,
        )
    }
}
