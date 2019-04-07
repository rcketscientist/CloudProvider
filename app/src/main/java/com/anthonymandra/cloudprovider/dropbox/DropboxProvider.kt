package com.anthonymandra.cloudprovider.dropbox

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_WRITE
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import com.anthonymandra.cloudprovider.app.BuildConfig
import com.anthonymandra.cloudprovider.app.R
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.Metadata
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class DropboxProvider: DocumentsProvider() {
    private val ROOT_DOCUMENT_ID = "DROPBOX_ROOT"

    private val DEFAULT_ROOT_PROJECTION = arrayOf(
        DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.COLUMN_ICON,
        DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID
    )

    private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE
    )

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        try {
            val client = DropboxClientFactory.client
            val metadata = client.files().getMetadata(documentId)
            val file = File(context.filesDir, metadata.getName())

            Log.d("openDocument", "opening ${documentId}")
            FileOutputStream(file).use {
                val metadata = client.files().download(documentId).download(it) //TODO: pipes
                return ParcelFileDescriptor.open(file, MODE_READ_WRITE)
            }
        } catch (e: Exception) {
            throw FileNotFoundException()
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        Log.d("cloud_path", "queryChildDocuments")
        // TODO: Likely need to be more strict about projection (ie: map to supported)
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val dropboxPath = if (parentDocumentId == ROOT_DOCUMENT_ID) "" else parentDocumentId

        try {
            val client = DropboxClientFactory.client

            Log.d("cloud_path", "list")
            var childFolders = client.files().listFolder(dropboxPath)
            while (true) {
                for (metadata in childFolders.entries) {
                    Log.d("cloud_path", metadata.pathLower)
                    addDocumentRow(result, metadata)
                }

                if (!childFolders.hasMore) {
                    break
                }

                childFolders = client.files().listFolderContinue(childFolders.cursor)
            }
        } catch(e: IllegalStateException) { // Test if we can attempt auth thru the provider
            context?.let {
                Log.d("cloud_path", "queryChildDocuments Auth")
                Auth.startOAuth2Authentication(it, BuildConfig.DROPBOX_API_KEY)   // TODO: appKey
            }
        }
        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        Log.d("cloud_path", "queryDocument")

        // TODO: Likely need to be more strict about projection (ie: map to supported)
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)

        // We create a root folder to access the cloud TODO: There's gotta be a better way of doing this
        if (documentId == ROOT_DOCUMENT_ID) {
//            return queryChildDocuments(documentId, projection, "") // TODO: not sure this is kosher
            val row = result.newRow()
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "root")
            row.add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)   // TODO:
            return result
        }

        try {
            val client = DropboxClientFactory.client
            val metadata = client.files().getMetadata(documentId)
            addDocumentRow(result, metadata)
        } catch(e: IllegalStateException) { // Test if we can attempt auth thru the provider
            context?.let {
                Log.d("cloud_path", "queryDocument Auth")
                Auth.startOAuth2Authentication(it, BuildConfig.DROPBOX_API_KEY)   // TODO: appKey
            }
        }
        return result
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        // TODO: Likely need to be more strict about projection (ie: map to supported)
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        val row = result.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, "com.anthonymandra.cloudprovider.dropbox")
        row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_dropbox_gray)
        row.add(DocumentsContract.Root.COLUMN_TITLE, "Dropbox")
        row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_CREATE)   // TODO:
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
        return result
    }

    fun addDocumentRow(result: MatrixCursor, metadata: Metadata) {
        if (metadata is FolderMetadata) {
            val row = result.newRow()
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, metadata.id)
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, metadata.name)
            row.add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)   // TODO:
        } else if (metadata is FileMetadata) {
            val row = result.newRow()
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, metadata.id)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, metadata.name)
//                        row.add(DocumentsContract.Document.COLUMN_FLAGS, )    //TODO:
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, metadata.clientModified.time)  // TODO: There's also serverModified...
            row.add(DocumentsContract.Document.COLUMN_SIZE, metadata.size)

            val mime = MimeTypeMap.getSingleton()
            val ext = metadata.name.substring(metadata.name.indexOf(".") + 1)
            val type = mime.getMimeTypeFromExtension(ext) ?: "application/octet-stream" // TODO: Is this the best fallback?
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, type)
        }
    }
}