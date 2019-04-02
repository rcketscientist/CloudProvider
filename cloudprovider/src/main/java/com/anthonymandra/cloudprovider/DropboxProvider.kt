package com.anthonymandra.cloudprovider

import android.database.Cursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsProvider
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.DbxRequestConfig
import android.content.pm.ProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.dropbox.core.v2.users.FullAccount





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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getDropboxClient() {
        val config = DbxRequestConfig.newBuilder(authority).build()
        val client = DbxClientV2(config, "") // FIXME: REMOVE
        val account = client.users().currentAccount
        Log.d("cloud_test", account.name.displayName)
    }

    private val authority: String   //TODO: Singleton this or something
        get() {
            var authority = "com.anthonymandra.cloudprovider.DropboxProvider"

            try {
                context?.let {
                    val componentName = ComponentName(it, this::class.java.name)
                    val providerInfo = it.packageManager.getProviderInfo(componentName, 0)
                    authority = providerInfo.authority
                }
            } finally {
                return authority
            }
    }
}