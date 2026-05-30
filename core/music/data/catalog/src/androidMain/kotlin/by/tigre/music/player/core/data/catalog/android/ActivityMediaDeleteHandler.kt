package by.tigre.music.player.core.data.catalog.android

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import by.tigre.logger.Log
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityMediaDeleteHandler(
    activity: ComponentActivity,
) : MediaDeleteHandler {

    private val activityRef = activity

    private var pendingContinuation: Continuation<Boolean>? = null

    private val intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val continuation = pendingContinuation
            pendingContinuation = null
            continuation?.resume(result.resultCode == Activity.RESULT_OK)
        }

    override suspend fun delete(uris: List<Uri>): Boolean {
        if (uris.isEmpty()) return false
        val activity = activityRef
        if (activity.isFinishing || activity.isDestroyed) return false

        return withContext(Dispatchers.Main.immediate) {
            suspendCoroutine { continuation ->
                val resolver = activity.contentResolver
                val needsConsent = mutableListOf<Uri>()

                for (uri in uris) {
                    try {
                        if (resolver.delete(uri, null, null) <= 0) {
                            needsConsent.add(uri)
                        }
                    } catch (e: RecoverableSecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            needsConsent.add(uri)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uris.size == 1) {
                            launchRecoverableFromException(e, continuation)
                            return@suspendCoroutine
                        } else {
                            Log.e(e) { "$TAG: RecoverableSecurityException for $uri" }
                            needsConsent.add(uri)
                        }
                    } catch (e: SecurityException) {
                        Log.e(e) { "$TAG: SecurityException deleting $uri" }
                        needsConsent.add(uri)
                    }
                }

                when {
                    needsConsent.isEmpty() -> continuation.resume(true)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        launchDeleteRequest(resolver, needsConsent, continuation)
                    }
                    else -> {
                        Log.w(TAG) { "Cannot delete ${needsConsent.size} file(s) without API 30+ batch consent" }
                        continuation.resume(false)
                    }
                }
            }
        }
    }

    private fun launchRecoverableFromException(
        exception: RecoverableSecurityException,
        continuation: Continuation<Boolean>
    ) {
        pendingContinuation = continuation
        intentSenderLauncher.launch(
            IntentSenderRequest.Builder(exception.userAction.actionIntent.intentSender).build()
        )
    }

    private fun launchDeleteRequest(
        resolver: ContentResolver,
        uris: List<Uri>,
        continuation: Continuation<Boolean>
    ) {
        try {
            val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
            pendingContinuation = continuation
            intentSenderLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        } catch (e: Exception) {
            Log.e(e) { "$TAG: Failed to create delete request for ${uris.size} item(s)" }
            continuation.resume(false)
        }
    }

    private companion object {
        const val TAG = "ActivityMediaDeleteHandler"
    }
}
