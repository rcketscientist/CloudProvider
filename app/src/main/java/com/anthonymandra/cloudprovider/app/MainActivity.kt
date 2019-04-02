package com.anthonymandra.cloudprovider.app

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        Single.fromCallable {
            val config = DbxRequestConfig.newBuilder(authority).build()
            val client = DbxClientV2(config, "") // FIXME: REMOVE
            val account = client.users().currentAccount
            Log.d("cloud_test", account.name.displayName)
            var rootFolders = client.files().listFolder("")
            while (true) {
                for (metadata in rootFolders.entries) {
                    Log.d("cloud_path", metadata.pathLower)
                }

                if (!rootFolders.hasMore) {
                    break
                }

                Log.d("cloud_path", "continue")
                rootFolders = client.files().listFolderContinue(rootFolders.cursor)
            }
        }
            .subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
            .subscribe()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val authority: String   //TODO: Singleton this or something
        get() {
            var authority = "com.anthonymandra.cloudprovider.DropboxProvider"

            try {
                val componentName = ComponentName(this, this::class.java.name)
                val providerInfo = this.packageManager.getProviderInfo(componentName, 0)
                authority = providerInfo.authority
            } finally {
                return authority
            }
        }
}
