package com.anthonymandra.cloudprovider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsProvider
import com.dropbox.core.android.Auth
import java.lang.IllegalStateException
import android.provider.DocumentsContract.Root.FLAG_SUPPORTS_CREATE
import android.R
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Root.COLUMN_ROOT_ID



class DropboxProvider: DocumentsProvider() {
    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        try {
            val client = DropboxClientFactory.client
        } catch(e: IllegalStateException) { // Test if we can attempt auth thru the provider
            context?.let {
                Auth.startOAuth2Authentication(it, "tnw5ufssav0syht")   // TODO: appKey
            }
        }

//        val result = MatrixCursor(arrayOf())
//        val row = result.newRow()
//        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, rootId)
//        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
//        row.add(
//            DocumentsContract.Root.COLUMN_TITLE,
//            context!!.getString(R.string.app_name)
//        )
//        row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_LOCAL_ONLY or DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
//        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, rootDocumentId)
    }
}