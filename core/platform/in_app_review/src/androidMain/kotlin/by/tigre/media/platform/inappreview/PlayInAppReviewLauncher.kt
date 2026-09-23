package by.tigre.media.platform.inappreview

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual typealias ActivityRef = Activity

class PlayInAppReviewLauncher : InAppReviewLauncher {
    override suspend fun launch(activity: ActivityRef): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        return try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = suspendCancellableCoroutine<ReviewInfo?> { continuation ->
                manager.requestReviewFlow()
                    .addOnCompleteListener { task ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            Log.w(TAG, "requestReviewFlow failed", task.exception)
                            continuation.resume(null)
                        }
                    }
            } ?: return false

            suspendCancellableCoroutine { continuation ->
                manager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener { task ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        if (!task.isSuccessful) {
                            Log.w(TAG, "launchReviewFlow failed", task.exception)
                        }
                        continuation.resume(task.isSuccessful)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "launch threw", e)
            false
        }
    }

    private companion object {
        const val TAG = "PlayInAppReviewLauncher"
    }
}
