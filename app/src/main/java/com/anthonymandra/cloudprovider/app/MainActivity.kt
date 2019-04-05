package com.anthonymandra.cloudprovider.app

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.anthonymandra.cloudprovider.DropboxClientFactory
import com.dropbox.core.android.Auth
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        loginButton.setOnClickListener {
            Auth.startOAuth2Authentication(this, "tnw5ufssav0syht")
        }

        fab.setOnClickListener {
            Single.fromCallable {
                val client = DropboxClientFactory.client
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

//            // Upload "test.txt" to Dropbox
//            FileInputStream("test.txt").use { file ->
//                val metadata = client.files().uploadBuilder("/test.txt")
//                    .uploadAndFinish(file)
//            }
            }
                .subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor()))
                .subscribe()
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
}
