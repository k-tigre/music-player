package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

/**
 * [Intent.ACTION_OPEN_DOCUMENT_TREE] with flags required so the user’s choice can be stored via
 * [android.content.ContentResolver.takePersistableUriPermission]. The stock
 * [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree] intent does not set
 * [Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION], which breaks persistence on some Android versions.
 */
class OpenAudiobookFolderContract : ActivityResultContract<Uri?, Uri?>() {

    override fun createIntent(context: Context, input: Uri?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}
