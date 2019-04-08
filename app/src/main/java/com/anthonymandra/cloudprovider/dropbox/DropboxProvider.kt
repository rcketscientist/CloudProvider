package com.anthonymandra.cloudprovider.dropbox

import android.app.AuthenticationRequiredException
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

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor? {
        context?.let {
            val client = DropboxClientFactory.getInstance(it) ?: throw FileNotFoundException()  // FIXME:
            val metadata = client.files().getMetadata(documentId)
            val file = File(it.filesDir, metadata.name)

            FileOutputStream(file).use { os ->
                val metadata = client.files().download(documentId).download(os) //TODO: pipes
                return ParcelFileDescriptor.open(file, MODE_READ_WRITE)
            }
        }
        throw FileNotFoundException()
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        // TODO: Likely need to be more strict about projection (ie: map to supported)
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val dropboxPath = if (parentDocumentId == ROOT_DOCUMENT_ID) "" else parentDocumentId

        context?.let {
            val client = DropboxClientFactory.getInstance(it) ?: return result
            var childFolders = client.files().listFolder(dropboxPath)
            while (true) {
                for (metadata in childFolders.entries) {
                    if (metadata is FolderMetadata) {
                        Log.d("parent: $parentDocumentId", "child: ${metadata.id}, ${metadata.pathLower}")
                    } else if (metadata is FileMetadata) {
                        Log.d("parent: $parentDocumentId", "child: ${metadata.id}, ${metadata.pathLower}")
                    }
                    addDocumentRow(result, metadata)
                }

                if (!childFolders.hasMore) {
                    break
                }

                childFolders = client.files().listFolderContinue(childFolders.cursor)
            }
        }
        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
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

        context?.let {
            val client = DropboxClientFactory.getInstance(it) ?: return result
            val metadata = client.files().getMetadata(documentId)
            addDocumentRow(result, metadata)
        }

        return result
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        // TODO: Likely need to be more strict about projection (ie: map to supported)
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        // If we don't have a valid client we won't display roots
        context?.let {
            DropboxClientFactory.getInstance(it) ?: return result
        }

        val row = result.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, "com.anthonymandra.cloudprovider.dropbox")
        row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_dropbox_gray)
        row.add(DocumentsContract.Root.COLUMN_TITLE, "Dropbox")
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
        return result
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        Log.d("parent", parentDocumentId)
        Log.d("child", documentId)

        return false
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