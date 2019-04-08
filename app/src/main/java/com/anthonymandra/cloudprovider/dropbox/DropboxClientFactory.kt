package com.anthonymandra.cloudprovider.dropbox

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2


/**
 * Singleton instance of [DbxClientV2] and friends
 */
object DropboxClientFactory {

    private var sDbxClient: DbxClientV2? = null
    @Volatile private var INSTANCE: DbxClientV2? = null

    fun getInstance(context: Context): DbxClientV2? =
        INSTANCE ?: synchronized(this) {
            val prefs = context.getSharedPreferences("cloudprovider", MODE_PRIVATE)
            val accessToken = prefs.getString("dropboxToken", null) ?: Auth.getOAuth2Token()
            if (accessToken != null) {
                prefs.edit().putString("dropboxToken", accessToken).apply()
                val requestConfig = DbxRequestConfig.newBuilder("com.anthonymandra.cloudprovider")
//                    .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                    .build()
                return DbxClientV2(requestConfig, accessToken).also { INSTANCE = it }
            }
            return null
        }

    val client: DbxClientV2
        get() {
            if (sDbxClient == null) {
                Auth.getOAuth2Token()?.let {
                    val requestConfig =
                        DbxRequestConfig.newBuilder("com.anthonymandra.cloudprovider")
                            .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                            .build()
                    sDbxClient = DbxClientV2(requestConfig, it)
                }
            }
            return sDbxClient ?: throw IllegalStateException("Client not initialized.")
        }
}