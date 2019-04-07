package com.anthonymandra.cloudprovider.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2

/**
 * Singleton instance of [DbxClientV2] and friends
 */
object DropboxClientFactory {

    private var sDbxClient: DbxClientV2? = null

    val client: DbxClientV2
        get() {
            if (sDbxClient == null) {
                Auth.getOAuth2Token()?.let {
                    val requestConfig =
                        DbxRequestConfig.newBuilder("com.anthonymandra.cloudprovider")
                            .build()
                    sDbxClient = DbxClientV2(requestConfig, it)
                }
            }
            return sDbxClient ?: throw IllegalStateException("Client not initialized.")
        }
}