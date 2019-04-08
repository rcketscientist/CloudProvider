package com.anthonymandra.cloudprovider.app

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.dropbox.core.android.Auth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        loginButton.setOnClickListener {
            Auth.startOAuth2Authentication(this, BuildConfig.DROPBOX_API_KEY)
        }

        openDocument.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                setType("*/*")
            }
            startActivity(intent)
        }

        openDocumentTree.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // This garbage is the closest we have to a dropbox auth callback...
        if (Auth.getOAuth2Token() != null) {
            val rootUri = DocumentsContract.buildRootsUri(BuildConfig.DROPBOX_AUTHORITY)
            contentResolver.notifyChange(rootUri, null) // TODO: The dropbox sdk auth lifecycle sucks, do it by hand
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
