package com.example.ui.features.workspace

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Remembers a user-selected SAF folder and creates output documents inside it.
 * No broad external-storage permission is required.
 */
object WorkspaceSaveLocation {
    private const val PREFS = "hasseena_workspace_save_location"
    private const val KEY_TREE_URI = "saved_tree_uri"

    fun getSavedFolder(context: Context): Uri? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null) ?: return null
        return runCatching { Uri.parse(value) }.getOrNull()
    }

    fun rememberFolder(context: Context, treeUri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TREE_URI, treeUri.toString()).apply()
    }

    fun createFile(
        context: Context,
        treeUri: Uri,
        mimeType: String,
        displayName: String
    ): Uri {
        val permissions = context.contentResolver.persistedUriPermissions
        val hasWriteAccess = permissions.any {
            it.uri == treeUri && it.isWritePermission
        }
        check(hasWriteAccess) {
            "Selected folder ki saved write permission nahi hai. Dobara folder select karein."
        }

        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        val safeName = displayName
            .replace("/", "_")
            .replace("\\", "_")
            .trim()
            .ifBlank { "jarvis-export.bin" }

        return DocumentsContract.createDocument(
            context.contentResolver,
            parentUri,
            mimeType,
            safeName
        ) ?: error("Selected folder mein file create nahi ho saki.")
    }
}
